package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.IceChaosWallVisualEntity;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.NetworkCompat;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.HashMap;

/** Three-second server-enforced stun. No AI flags or terrain are modified. */
public final class IcePrisonManager {
    public static final int FREEZE_TICKS = net.sweenus.simplybows.util.WinterfangAbilityRules.FREEZE_TICKS;
    private static final Map<ServerWorld, Map<UUID, Prison>> PRISONS = new WeakHashMap<>();
    private IcePrisonManager() {}

    public static boolean isFrozen(Entity entity) {
        if (entity == null || !(entity.getWorld() instanceof ServerWorld world) || !entity.isAlive()) return false;
        Map<UUID, Prison> prisons = PRISONS.get(world);
        Prison prison = prisons == null ? null : prisons.get(entity.getUuid());
        return prison != null && world.getTime() < prison.expires;
    }

    public static void freeze(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (!target.isAlive() || isFrozen(target) || owner == null
                || !CombatTargeting.checkFriendlyFire(target, owner)) return;
        target.stopRiding();
        target.clearActiveItem();
        target.setSprinting(false);
        target.setJumping(false);
        Vec3d anchor = target.getPos();
        IceChaosWallVisualEntity shell = new IceChaosWallVisualEntity(world, anchor.x, anchor.y, anchor.z, target.getHeight() + 0.35F);
        shell.setPrisonStyle(target.getWidth() + 0.45F, target.getWidth() + 0.45F);
        shell.setHeightScale(1.0F);
        // A prison visual expires independently if its victim unloads or the server restarts.
        shell.setPrisonLifetime(FREEZE_TICKS);
        world.spawnEntity(shell);
        PRISONS.computeIfAbsent(world, w -> new HashMap<>()).put(target.getUuid(),
                new Prison(anchor, world.getTime() + FREEZE_TICKS, shell.getUuid()));
        hold(target, anchor);
        world.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, 0.65F);
    }

    public static void tick(ServerWorld world) {
        Map<UUID, Prison> prisons = PRISONS.get(world);
        if (prisons == null) return;
        prisons.entrySet().removeIf(entry -> {
            Prison prison = entry.getValue();
            Entity target = world.getEntity(entry.getKey());
            if (target == null || !target.isAlive() || world.getTime() >= prison.expires) {
                Entity shell = world.getEntity(prison.visual);
                if (shell != null) shell.discard();
                return true;
            }
            // Keep damage immunity timers moving while AI/action ticks are paused.
            if (target.timeUntilRegen > 0) target.timeUntilRegen--;
            if (target instanceof LivingEntity living && living.hurtTime > 0) living.hurtTime--;
            hold(target, prison.anchor);
            if (world.getTime() % 5 == 0) world.spawnParticles(ParticleTypes.SNOWFLAKE,
                    prison.anchor.x, prison.anchor.y + target.getHeight() * 0.5, prison.anchor.z,
                    6, target.getWidth() * 0.5, target.getHeight() * 0.4, target.getWidth() * 0.5, 0);
            return false;
        });
        if (prisons.isEmpty()) PRISONS.remove(world);
    }

    public static void hold(Entity target) {
        if (!(target.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, Prison> prisons = PRISONS.get(world);
        Prison prison = prisons == null ? null : prisons.get(target.getUuid());
        if (prison != null && isFrozen(target)) hold(target, prison.anchor);
    }

    private static void hold(Entity target, Vec3d anchor) {
        target.setVelocity(Vec3d.ZERO);
        target.velocityDirty = true;
        target.fallDistance = 0;
        target.prevX = anchor.x; target.prevY = anchor.y; target.prevZ = anchor.z;
        if (target instanceof LivingEntity living) living.clearActiveItem();
        if (target instanceof ServerPlayerEntity player) {
            if (target.squaredDistanceTo(anchor) > 0.0001) player.networkHandler.requestTeleport(
                    anchor.x, anchor.y, anchor.z, player.getYaw(), player.getPitch());
            NetworkCompat.sendVelocityUpdate(player);
        } else target.setPosition(anchor);
    }
    private record Prison(Vec3d anchor, long expires, UUID visual) {}
}
