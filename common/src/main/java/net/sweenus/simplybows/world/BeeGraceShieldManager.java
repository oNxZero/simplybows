package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BeeGraceVisualEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BeeGraceShieldManager {

    public static final String GRACE_VISUAL_TAG = "simplybows_bee_grace_visual";
    private static double graceApplyRadius() { return SimplyBowsConfig.INSTANCE.buzzkill.graceApplyRadius.get(); }
    private static int maxBeesPerTarget() { return SimplyBowsConfig.INSTANCE.buzzkill.graceMaxBeesPerTarget.get(); }
    private static int baseDurationTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.graceBaseDuration.get(); }
    private static int stringDurationBonusTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.graceStringDurationBonus.get(); }
    private static int graceCooldownTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.graceCooldownTicks.get(); }
    private static final double ORBIT_RADIUS = 2.0;
    private static final double ORBIT_HEIGHT = 1.2;
    private static final double ORBIT_BOB_HEIGHT = 0.18;
    private static final double BASE_ORBIT_ANGULAR_SPEED = 0.15;
    private static final double ORBIT_ANGULAR_SPEED_RANDOM_RANGE = 0.03;
    private static final float ORBIT_POSITION_SMOOTHING = 0.24F;
    private static final float ORBIT_YAW_SMOOTHING = 0.35F;
    private static final int FADE_OUT_TICKS = 20;
    private static final double MIN_HORIZONTAL_MOTION_SQ_FOR_YAW = 1.0E-4;

    private static final Map<ServerWorld, List<ActiveGraceShield>> ACTIVE_SHIELDS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> GRACE_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private BeeGraceShieldManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveGraceShield> shields = ACTIVE_SHIELDS.get(world);
        return (shields != null && !shields.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void tryApplyFromImpact(ServerWorld world, Vec3d impactPos, LivingEntity owner, BowUpgradeData upgrades, LivingEntity struck) {
        if (world == null || impactPos == null || owner == null || !isGraceReady(world, owner.getUuid())) {
            return;
        }

        LivingEntity orbitTarget = struck != null && struck.isAlive() ? struck : nearestPlayer(world, impactPos, 4.0);
        if (orbitTarget == null) {
            return;
        }

        applyShield(world, orbitTarget, upgrades);
        int duration = baseDurationTicks() + Math.max(0, upgrades.stringLevel()) * stringDurationBonusTicks();
        startGraceCooldown(world, owner, Math.max(20, duration));
    }

    private static PlayerEntity nearestPlayer(ServerWorld world, Vec3d pos, double radius) {
        PlayerEntity best = null;
        double bestDist = radius * radius;
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, Box.of(pos, radius * 2.0, radius * 2.0, radius * 2.0), PlayerEntity::isAlive)) {
            double dist = player.squaredDistanceTo(pos);
            if (dist <= bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private static boolean isGraceReady(ServerWorld world, UUID ownerId) {
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = CooldownStorage.forWorld(GRACE_COOLDOWNS_BY_SERVER, world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startGraceCooldown(ServerWorld world, LivingEntity owner, int durationTicks) {
        int cooldownTicks = Math.max(Math.max(20, graceCooldownTicks()), durationTicks);
        CooldownStorage.forWorld(GRACE_COOLDOWNS_BY_SERVER, world)
                .put(owner.getUuid(), CooldownStorage.currentTick(world) + cooldownTicks);
        if (owner instanceof ServerPlayerEntity player) {
            SimplyBowItem.simplybows$sendCooldownPacket(player, "bee",
                    System.currentTimeMillis() + (long) cooldownTicks * 50L, cooldownTicks);
        }
    }

    public static boolean consumeShield(ServerWorld world, LivingEntity target) {
        List<ActiveGraceShield> shields = ACTIVE_SHIELDS.get(world);
        if (shields == null || shields.isEmpty() || target == null) {
            return false;
        }

        int consumeIndex = -1;
        long bestExpiry = Long.MAX_VALUE;
        for (int i = 0; i < shields.size(); i++) {
            ActiveGraceShield shield = shields.get(i);
            if (!target.getUuid().equals(shield.targetId)) {
                continue;
            }
            if (shield.expiryTick < bestExpiry) {
                bestExpiry = shield.expiryTick;
                consumeIndex = i;
            }
        }
        if (consumeIndex >= 0) {
            ActiveGraceShield consumed = shields.remove(consumeIndex);
            discardVisual(world, consumed.visualId);
            if (shields.isEmpty()) {
                ACTIVE_SHIELDS.remove(world);
            }

            Vec3d pos = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.7, 0.0);
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 0.75F, 1.35F + world.random.nextFloat() * 0.1F);
            world.spawnParticles(ParticleTypes.WAX_OFF, pos.x, pos.y, pos.z, 8, 0.22, 0.2, 0.22, 0.01);
            world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 5, 0.16, 0.14, 0.16, 0.01);
            return true;
        }
        return false;
    }

    public static void tick(ServerWorld world) {
        List<ActiveGraceShield> shields = ACTIVE_SHIELDS.get(world);
        if (shields == null || shields.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        shields.removeIf(shield -> {
            if (world.getTime() > shield.expiryTick) {
                discardVisual(world, shield.visualId);
                return true;
            }
            LivingEntity target = getLivingEntity(world, shield.targetId);
            if (target == null || !target.isAlive()) {
                discardVisual(world, shield.visualId);
                return true;
            }

            Entity visualEntity = world.getEntity(shield.visualId);
            if (!(visualEntity instanceof BeeGraceVisualEntity visual)) {
                return true;
            }
            return false;
        });

        if (shields.isEmpty()) {
            ACTIVE_SHIELDS.remove(world);
            return;
        }

        Map<UUID, List<ActiveGraceShield>> shieldsByTarget = new HashMap<>();
        for (ActiveGraceShield shield : shields) {
            shieldsByTarget.computeIfAbsent(shield.targetId, ignored -> new ArrayList<>()).add(shield);
        }

        for (Map.Entry<UUID, List<ActiveGraceShield>> entry : shieldsByTarget.entrySet()) {
            LivingEntity target = getLivingEntity(world, entry.getKey());
            if (target == null || !target.isAlive()) {
                continue;
            }

            List<ActiveGraceShield> targetShields = entry.getValue();
            targetShields.sort((a, b) -> Long.compare(a.spawnTick, b.spawnTick));
            if (world.getTime() % 10L == 0L) {
                pulseResistance(world, target, targetShields.getFirst().resistanceAmplifier);
            }
            int count = targetShields.size();
            for (int i = 0; i < count; i++) {
                ActiveGraceShield shield = targetShields.get(i);
                Entity visualEntity = world.getEntity(shield.visualId);
                if (visualEntity instanceof BeeGraceVisualEntity visual) {
                    updateOrbit(world, visual, target, shield, i, count);
                }
            }
        }
    }

    private static void applyShield(ServerWorld world, LivingEntity target, BowUpgradeData upgrades) {
        List<ActiveGraceShield> shields = ACTIVE_SHIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID targetId = target.getUuid();
        int maxStacks = getMaxBeesForStringLevel(upgrades.stringLevel());
        int currentStacks = 0;
        int replaceIndex = -1;
        long earliestExpiry = Long.MAX_VALUE;
        for (int i = 0; i < shields.size(); i++) {
            ActiveGraceShield shield = shields.get(i);
            if (!targetId.equals(shield.targetId)) {
                continue;
            }
            currentStacks++;
            if (shield.expiryTick < earliestExpiry) {
                earliestExpiry = shield.expiryTick;
                replaceIndex = i;
            }
        }
        if (currentStacks >= maxStacks && replaceIndex >= 0) {
            ActiveGraceShield replaced = shields.remove(replaceIndex);
            discardVisual(world, replaced.visualId);
        }

        Vec3d start = target.getPos().add(ORBIT_RADIUS, ORBIT_HEIGHT, 0.0);
        BeeGraceVisualEntity visual = new BeeGraceVisualEntity(world, start.x, start.y, start.z);
        visual.addCommandTag(GRACE_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }

        int duration = baseDurationTicks() + upgrades.stringLevel() * stringDurationBonusTicks();
        long now = world.getTime();
        int resistanceAmplifier = MathHelper.clamp(upgrades.frameLevel(), 0, 4);
        ActiveGraceShield shield = new ActiveGraceShield(
                targetId,
                visual.getUuid(),
                now,
                now + Math.max(20, duration),
                world.random.nextDouble() * Math.PI * 2.0,
                BASE_ORBIT_ANGULAR_SPEED + (world.random.nextDouble() * 2.0 - 1.0) * ORBIT_ANGULAR_SPEED_RANDOM_RANGE,
                resistanceAmplifier
        );
        shields.add(shield);

        Vec3d pos = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.7, 0.0);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BEE_POLLINATE, SoundCategory.PLAYERS, 0.55F, 1.2F + world.random.nextFloat() * 0.1F);
        world.spawnParticles(ParticleTypes.WAX_ON, pos.x, pos.y, pos.z, 7, 0.18, 0.14, 0.18, 0.01);
    }

    private static void updateOrbit(ServerWorld world, BeeGraceVisualEntity visual, LivingEntity target, ActiveGraceShield shield, int slotIndex, int slotCount) {
        long ticksRemaining = shield.expiryTick - world.getTime();
        float fade = MathHelper.clamp((float) ticksRemaining / FADE_OUT_TICKS, 0.0F, 1.0F);
        visual.setHeightScale(fade);

        double slotOffset = slotCount <= 1 ? 0.0 : (Math.PI * 2.0 / slotCount) * slotIndex;
        double t = (world.getTime() * shield.angularSpeed) + shield.angleOffset + slotOffset;
        double radius = ORBIT_RADIUS + (slotCount <= 1 ? 0.0 : 0.06 * (slotIndex % 2));
        double targetX = target.getX() + Math.cos(t) * radius;
        double targetZ = target.getZ() + Math.sin(t) * radius;
        double targetY = target.getY() + ORBIT_HEIGHT + Math.sin(t * 1.6) * ORBIT_BOB_HEIGHT + (slotCount <= 1 ? 0.0 : (slotIndex - (slotCount - 1) * 0.5) * 0.04);

        Vec3d currentPos = visual.getPos();
        Vec3d desiredPos = new Vec3d(targetX, targetY, targetZ);
        Vec3d smoothedPos = currentPos.lerp(desiredPos, ORBIT_POSITION_SMOOTHING);
        Vec3d motion = smoothedPos.subtract(currentPos);

        if (motion.horizontalLengthSquared() > MIN_HORIZONTAL_MOTION_SQ_FOR_YAW) {
            float movementYaw = (float) (Math.atan2(motion.x, motion.z) * (180.0F / Math.PI));
            float smoothedYaw = MathHelper.lerpAngleDegrees(ORBIT_YAW_SMOOTHING, visual.getYaw(), movementYaw);
            visual.setYaw(smoothedYaw);
        }

        visual.setPos(smoothedPos.x, smoothedPos.y, smoothedPos.z);
    }

    private static void pulseResistance(ServerWorld world, LivingEntity center, int amplifier) {
        Box box = center.getBoundingBox().expand(ORBIT_RADIUS, 1.25, ORBIT_RADIUS);
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, box, PlayerEntity::isAlive)) {
            if (player.squaredDistanceTo(center) > ORBIT_RADIUS * ORBIT_RADIUS) {
                continue;
            }
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, amplifier), center);
        }
    }

    private static int getMaxBeesForStringLevel(int stringLevel) {
        return Math.min(maxBeesPerTarget(), Math.max(1, stringLevel + 1));
    }

    private static int getCurrentStacksForTarget(ServerWorld world, UUID targetId) {
        List<ActiveGraceShield> shields = ACTIVE_SHIELDS.get(world);
        if (shields == null || shields.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ActiveGraceShield shield : shields) {
            if (targetId.equals(shield.targetId)) {
                count++;
            }
        }
        return count;
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID uuid) {
        Entity entity = world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BeeGraceVisualEntity && entity.getCommandTags().contains(GRACE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveGraceShield {
        private final UUID targetId;
        private final UUID visualId;
        private final long spawnTick;
        private final long expiryTick;
        private final double angleOffset;
        private final double angularSpeed;
        private final int resistanceAmplifier;

        private ActiveGraceShield(UUID targetId, UUID visualId, long spawnTick, long expiryTick, double angleOffset, double angularSpeed, int resistanceAmplifier) {
            this.targetId = targetId;
            this.visualId = visualId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.angleOffset = angleOffset;
            this.angularSpeed = angularSpeed;
            this.resistanceAmplifier = resistanceAmplifier;
        }
    }
}
