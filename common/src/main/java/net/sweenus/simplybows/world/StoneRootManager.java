package net.sweenus.simplybows.world;
import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.util.NetworkCompat;
import java.util.*;
/** Movement-only roots: attacks, items and AI continue normally. Unloading clears the root. */
public final class StoneRootManager {
    private record Root(Vec3d anchor,long expires) {}
    private static final Map<ServerWorld,Map<UUID,Root>> ROOTS=new WeakHashMap<>();
    public static boolean isRooted(Entity e) {
        if(e==null || !e.isAlive() || !(e.getWorld() instanceof ServerWorld w)) return false;
        Root root=ROOTS.getOrDefault(w,Map.of()).get(e.getUuid());
        return root!=null && w.getTime()<root.expires();
    }
    public static void root(ServerWorld w,LivingEntity e) {
        e.stopRiding();
        ROOTS.computeIfAbsent(w,k->new HashMap<>()).put(e.getUuid(),new Root(e.getPos(),w.getTime()+100));
        e.setVelocity(Vec3d.ZERO); e.velocityDirty=true;
        if(e instanceof ServerPlayerEntity p) NetworkCompat.sendVelocityUpdate(p);
    }
    public static void tick(ServerWorld w) {
        Map<UUID,Root> roots=ROOTS.get(w); if(roots==null) return;
        roots.entrySet().removeIf(entry->{
            Entity e=w.getEntity(entry.getKey()); Root root=entry.getValue();
            if(e==null || !e.isAlive() || w.getTime()>=root.expires() || e.squaredDistanceTo(root.anchor())>256) return true;
            e.stopRiding(); e.setVelocity(Vec3d.ZERO); e.velocityDirty=true;
            if(e.squaredDistanceTo(root.anchor())>.0001) {
                if(e instanceof ServerPlayerEntity p) p.networkHandler.requestTeleport(root.anchor().x,root.anchor().y,root.anchor().z,p.getYaw(),p.getPitch());
                else e.setPosition(root.anchor());
            }
            if(w.getTime()%10==0) w.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(net.minecraft.particle.ParticleTypes.BLOCK,net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()),e.getX(),e.getY()+.1,e.getZ(),4,.3,.1,.3,0);
            return false;
        });
        if(roots.isEmpty()) ROOTS.remove(w);
    }
}
