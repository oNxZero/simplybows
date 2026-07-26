package net.sweenus.simplybows.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Fire-rate limiter for non-player bow wielders. Both mob firing paths (patched
 * skeleton bow AI and the universal MobEntity tick hook) gate through this map,
 * so a mob reachable by both paths can never double-fire within one window.
 */
public final class MobBowFireManager {

    private static final int PRUNE_INTERVAL_TICKS = 200;

    private record FireKey(UUID entityId, Item bowItem) {
    }

    private static final Map<MinecraftServer, Map<FireKey, Long>> READY_TICKS_BY_SERVER = new WeakHashMap<>();

    private MobBowFireManager() {
    }

    public static boolean isReady(ServerWorld world, LivingEntity shooter, ItemStack stack) {
        Long readyTick = forWorld(world).get(keyOf(shooter, stack));
        return readyTick == null || world.getTime() >= readyTick;
    }

    public static void markFired(ServerWorld world, LivingEntity shooter, ItemStack stack, int minIntervalTicks) {
        forWorld(world).put(keyOf(shooter, stack), world.getTime() + Math.max(1, minIntervalTicks));
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % PRUNE_INTERVAL_TICKS != 0) {
            return;
        }
        long now = world.getTime();
        forWorld(world).values().removeIf(readyTick -> readyTick <= now);
    }

    private static FireKey keyOf(LivingEntity shooter, ItemStack stack) {
        return new FireKey(shooter.getUuid(), stack.getItem());
    }

    private static Map<FireKey, Long> forWorld(ServerWorld world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return new HashMap<>();
        }
        return READY_TICKS_BY_SERVER.computeIfAbsent(server, ignored -> new HashMap<>());
    }
}
