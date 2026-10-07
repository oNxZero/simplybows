package net.sweenus.simplybows.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.world.BowPassiveParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    private static final Map<UUID, Integer> DEBUG_LAST_LOOKED_ENTITY = new HashMap<>();
    private static final double DEBUG_LOOK_DISTANCE = 48.0;

    @Shadow
    public abstract ServerWorld getServerWorld();

    @Inject(at = @At("HEAD"), method = "tick")
    public void simplybows$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            BowPassiveParticleManager.tick(serverPlayer, getServerWorld());
            simplybows$debugLogLookedEntity(serverPlayer);
        }
    }

    private static void simplybows$debugLogLookedEntity(ServerPlayerEntity player) {
        if (!SimplyBows.debugMode() || player.age % 5 != 0) {
            return;
        }

        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(DEBUG_LOOK_DISTANCE));
        Box searchBox = player.getBoundingBox().stretch(look.multiply(DEBUG_LOOK_DISTANCE)).expand(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                player.getWorld(),
                player,
                start,
                end,
                searchBox,
                entity -> entity != player && entity.canHit() && !entity.isSpectator()
        );
        if (entityHit == null) {
            return;
        }

        HitResult blockHit = player.raycast(DEBUG_LOOK_DISTANCE, 1.0F, false);
        double entityDistanceSq = start.squaredDistanceTo(entityHit.getPos());
        double blockDistanceSq = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : start.squaredDistanceTo(blockHit.getPos());
        if (entityDistanceSq > blockDistanceSq) {
            return;
        }

        Entity lookedEntity = entityHit.getEntity();
        Integer previous = DEBUG_LAST_LOOKED_ENTITY.put(player.getUuid(), lookedEntity.getId());
        if (previous != null && previous == lookedEntity.getId()) {
            return;
        }

        var entityId = Registries.ENTITY_TYPE.getId(lookedEntity.getType());
        String fullId = entityId == null ? "unknown" : entityId.toString();
        String path = entityId == null ? "unknown" : entityId.getPath();
        SimplyBows.LOGGER.info(
                "[DEBUG] Player '{}' is looking at entity '{}'(path='{}')",
                player.getName().getString(),
                fullId,
                path
        );
    }
}
