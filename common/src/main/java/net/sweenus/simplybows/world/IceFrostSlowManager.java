package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Preserve Frost's movement penalty on bosses that reject ordinary status effects. */
public final class IceFrostSlowManager {
    private static final Map<ServerWorld, Map<UUID, Long>> IMMUNE_TARGETS = new WeakHashMap<>();
    private static final Map<ServerWorld, Map<UUID, Vec3d>> START_POSITIONS = new WeakHashMap<>();
    private IceFrostSlowManager() {}
    public static void apply(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (!target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, net.sweenus.simplybows.util.WinterfangAbilityRules.FROST_TICKS, 2), owner)
                && !target.hasStatusEffect(StatusEffects.SLOWNESS))
            IMMUNE_TARGETS.computeIfAbsent(world, w -> new HashMap<>()).put(target.getUuid(), world.getTime() + net.sweenus.simplybows.util.WinterfangAbilityRules.FROST_TICKS);
    }
    public static void beforeTick(ServerWorld world, Entity target) {
        Map<UUID, Long> times = IMMUNE_TARGETS.get(world);
        Long end = times == null ? null : times.get(target.getUuid());
        if (end == null) return;
        if (!target.isAlive() || world.getTime() >= end) {
            times.remove(target.getUuid());
            if (times.isEmpty()) IMMUNE_TARGETS.remove(world);
            return;
        }
        START_POSITIONS.computeIfAbsent(world, w -> new HashMap<>()).put(target.getUuid(), target.getPos());
    }
    public static void afterTick(ServerWorld world, Entity target) {
        Map<UUID, Vec3d> starts = START_POSITIONS.get(world);
        Vec3d start = starts == null ? null : starts.remove(target.getUuid());
        if (start == null) return;
        if (starts.isEmpty()) START_POSITIONS.remove(world);
        Vec3d delta = target.getPos().subtract(start);
        // Keep teleports intact; Frost controls ordinary movement, not dimension transitions.
        if (delta.lengthSquared() > 64) return;
        Vec3d correction = delta.multiply(-0.45);
        target.setPosition(target.getPos().add(correction));
        if (target instanceof EnderDragonEntity dragon)
            for (var part : dragon.getBodyParts()) part.setPosition(part.getPos().add(correction));
    }
    public static void tick(ServerWorld world) {
        Map<UUID, Long> times = IMMUNE_TARGETS.get(world);
        if (times == null) return;
        times.entrySet().removeIf(e -> e.getValue() <= world.getTime() || world.getEntity(e.getKey()) == null);
        if (times.isEmpty()) IMMUNE_TARGETS.remove(world);
    }
}
