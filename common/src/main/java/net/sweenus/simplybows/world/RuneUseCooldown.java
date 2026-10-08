package net.sweenus.simplybows.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.item.unique.SimplyBowItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RuneUseCooldown {

    public static final int SHORT_TICKS = 40;

    private static final Map<MinecraftServer, Map<String, Long>> ENDS = new WeakHashMap<>();

    private RuneUseCooldown() {
    }

    public static boolean isReady(ServerWorld world, UUID ownerId, String key) {
        if (ownerId == null || key == null) {
            return true;
        }
        Long end = ends(world).get(id(ownerId, key));
        return end == null || end <= world.getTime();
    }

    public static void start(ServerWorld world, UUID ownerId, String key, String bowKey) {
        start(world, ownerId, key, bowKey, SHORT_TICKS);
    }

    public static void start(ServerWorld world, UUID ownerId, String key, String bowKey, int ticks) {
        if (world == null || ownerId == null || key == null || ticks <= 0) {
            return;
        }
        ends(world).put(id(ownerId, key), world.getTime() + ticks);
        if (bowKey != null && world.getEntity(ownerId) instanceof ServerPlayerEntity player) {
            SimplyBowItem.simplybows$sendCooldownPacket(
                    player,
                    bowKey,
                    System.currentTimeMillis() + (long) ticks * 50L,
                    ticks
            );
        }
    }

    private static Map<String, Long> ends(ServerWorld world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return new HashMap<>();
        }
        return ENDS.computeIfAbsent(server, ignored -> new HashMap<>());
    }

    private static String id(UUID ownerId, String key) {
        return ownerId + ":" + key;
    }
}
