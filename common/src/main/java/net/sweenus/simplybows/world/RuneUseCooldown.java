package net.sweenus.simplybows.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * Shared ability cooldown for all Simply Bows on one player.
 * Casting any unique ability locks every Simply Bow until the CD ends —
 * so players bring a party instead of five bows.
 */
public final class RuneUseCooldown {

    public static final int SHORT_TICKS = 40;
    /** Instant / burst abilities without a long field use this as their "effect" window. */
    public static final int BURST_EFFECT_TICKS = 40;
    /** Client overlay + packet key shared by every unique bow. */
    public static final String GLOBAL_BOW_KEY = "global";

    /** Effects up to this length still use a flat 3× cooldown. */
    private static final int SOFT_START_TICKS = 100; // 5s
    /** Past the soft start, each extra effect tick only adds this many CD ticks. */
    private static final int SOFT_EXTRA_MULTIPLIER = 2;
    /** Hard ceiling so long Chaos/fields don't idle forever (~30s). */
    private static final int MAX_COOLDOWN_TICKS = 600;

    private static final Map<MinecraftServer, Map<UUID, Long>> GLOBAL_ENDS = new WeakHashMap<>();

    /**
     * Set from SimplyBowItem / mod init — avoids a class-init cycle with SimplyBowItem.
     * Args: player, ticks (overlay duration from now).
     */
    public static BiConsumer<ServerPlayerEntity, Integer> CLIENT_SYNC = null;

    private RuneUseCooldown() {
    }

    /**
     * Ability cooldown from cast: 3× for short effects, then 2× on the remainder, capped.
     */
    public static int fromEffectDuration(int effectDurationTicks) {
        int effect = Math.max(1, effectDurationTicks);
        int cooldown;
        if (effect <= SOFT_START_TICKS) {
            cooldown = effect * 3;
        } else {
            cooldown = SOFT_START_TICKS * 3 + (effect - SOFT_START_TICKS) * SOFT_EXTRA_MULTIPLIER;
        }
        return Math.max(SHORT_TICKS, Math.min(MAX_COOLDOWN_TICKS, cooldown));
    }

    /** True if this player can use any Simply Bow ability right now. */
    public static boolean isPlayerReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return true;
        }
        Long end = globalEnds(world).get(ownerId);
        return end == null || end <= world.getTime();
    }

    /**
     * Per-ability key is kept for call-site clarity; readiness is always global.
     */
    public static boolean isReady(ServerWorld world, UUID ownerId, String key) {
        return isPlayerReady(world, ownerId);
    }

    public static void start(ServerWorld world, UUID ownerId, String key, String bowKey) {
        start(world, ownerId, key, bowKey, fromEffectDuration(BURST_EFFECT_TICKS));
    }

    public static void startForEffect(ServerWorld world, UUID ownerId, String key, String bowKey, int effectDurationTicks) {
        start(world, ownerId, key, bowKey, fromEffectDuration(effectDurationTicks));
    }

    public static void start(ServerWorld world, UUID ownerId, String key, String bowKey, int ticks) {
        if (world == null || ownerId == null || ticks <= 0) {
            return;
        }
        long now = world.getTime();
        long newEnd = now + ticks;
        Long existing = globalEnds(world).get(ownerId);
        if (existing != null && existing > newEnd) {
            // Never shorten an existing longer shared CD.
            ticks = (int) Math.max(1L, existing - now);
            newEnd = existing;
        }
        globalEnds(world).put(ownerId, newEnd);

        BiConsumer<ServerPlayerEntity, Integer> sync = CLIENT_SYNC;
        if (sync != null && world.getEntity(ownerId) instanceof ServerPlayerEntity player) {
            sync.accept(player, ticks);
        }
    }

    private static Map<UUID, Long> globalEnds(ServerWorld world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return new HashMap<>();
        }
        return GLOBAL_ENDS.computeIfAbsent(server, ignored -> new HashMap<>());
    }
}
