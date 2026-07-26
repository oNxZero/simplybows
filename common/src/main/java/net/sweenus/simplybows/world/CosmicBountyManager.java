package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicArrowEntity;
import net.sweenus.simplybows.entity.CosmicBountyVisualEntity;
import net.sweenus.simplybows.item.unique.CosmicBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.registry.ParticleRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmicBountyManager {

    private static final int BURST_VISUAL_TICKS = 28;
    private static final int STARDUST_DURATION_TICKS = 120;
    private static final int STARDUST_DAMAGE_INTERVAL_TICKS = 10;
    private static final double STARDUST_MIN_FALL_HEIGHT = 18.0;
    private static final double STARDUST_FALL_HEIGHT_RADIUS_MULTIPLIER = 2.75;
    private static final List<ActiveBountyStar> ACTIVE_STARS = new ArrayList<>();
    private static final List<ActiveStardustField> ACTIVE_STARDUST_FIELDS = new ArrayList<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> BOUNTY_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private CosmicBountyManager() {
    }

    private static int maxChargeTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyMaxChargeTicks.get(); }
    private static int implodeTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyImplodeTicks.get(); }
    private static double minRadius() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyMinRadius.get(); }
    private static double maxRadius() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyMaxRadius.get(); }
    private static float minDamage() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyMinDamage.get(); }
    private static float maxDamage() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyMaxDamage.get(); }
    private static double pullStrength() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyPullStrength.get(); }
    private static int cooldownTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.bountyCooldownTicks.get(); }

    public static boolean hasActive(ServerWorld world) {
        return !ACTIVE_STARS.isEmpty()
                || !ACTIVE_STARDUST_FIELDS.isEmpty()
                || !getCooldowns(world).isEmpty()
                || world.getTime() % 20L == 0L;
    }

    public static void createImplosion(ServerWorld world, Entity owner, Vec3d pos, int chargeTicks, BowUpgradeData upgrades) {
        createImplosion(world, owner, pos, chargeTicks, upgrades, false);
    }

    public static void createImplosion(ServerWorld world, Entity owner, Vec3d pos, int chargeTicks, BowUpgradeData upgrades, boolean createStardustField) {
        if (world == null || pos == null) {
            return;
        }
        UUID ownerId = owner == null ? null : owner.getUuid();
        if (ownerId != null && !isBountyReady(world, ownerId)) {
            return;
        }
        BowUpgradeData bountyUpgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
        int maxCharge = Math.max(1, maxChargeTicks());
        float charge = Math.min(1.0F, Math.max(0.0F, chargeTicks / (float) maxCharge));
        double radius = lerp(minRadius(), maxRadius(), charge) * 2.0;
        float damage = (float) lerp(minDamage(), maxDamage(), charge) * (float) bountyUpgrades.damageMultiplier();
        int implodeDuration = Math.max(1, implodeTicks());

        CosmicBountyVisualEntity visual = new CosmicBountyVisualEntity(world, pos.x, pos.y, pos.z);
        visual.setChargeTicks(Math.min(chargeTicks, maxCharge));
        visual.setImplodeTicks(0);
        visual.setMaxImplodeTicks(implodeDuration);
        visual.setExploded(false);
        if (!world.spawnEntity(visual)) {
            return;
        }

        ACTIVE_STARS.add(new ActiveBountyStar(world, visual.getUuid(), ownerId, pos, radius, damage, implodeDuration, createStardustField));
        if (ownerId != null) {
            startBountyCooldown(world, ownerId);
            if (owner instanceof ServerPlayerEntity player) {
                int ticks = Math.max(0, cooldownTicks());
                if (ticks > 0) {
                    SimplyBowItem.simplybows$sendCooldownPacket(player, "cosmic", System.currentTimeMillis() + ticks * 50L, ticks);
                }
            }
        }
        playImplodeStart(world, pos, charge);
    }

    public static boolean triggerAirborneDetonation(LivingEntity living) {
        if (living == null || !(living.getWorld() instanceof ServerWorld world) || !isHoldingBountyCosmicBow(living)) {
            return false;
        }
        if (!isBountyReady(world, living.getUuid())) {
            return false;
        }

        Box searchBox = living.getBoundingBox().expand(192.0);
        CosmicArrowEntity bestArrow = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (CosmicArrowEntity arrow : world.getEntitiesByClass(CosmicArrowEntity.class, searchBox, arrow ->
                arrow.isAlive()
                        && !arrow.isRemoved()
                        && arrow.isBountyMode()
                        && !arrow.isOnGround()
                        && living.equals(arrow.getOwner()))) {
            double distanceSq = arrow.squaredDistanceTo(living);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestArrow = arrow;
            }
        }

        if (bestArrow == null) {
            return false;
        }

        createImplosion(world, living, bestArrow.getPos(), bestArrow.getBountyChargeTicks(), bestArrow.getUpgrades(), true);
        bestArrow.discard();
        world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.55F, 1.75F);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        if (ACTIVE_STARS.isEmpty()) {
            tickStardustFields(world);
            return;
        }

        Iterator<ActiveBountyStar> iterator = ACTIVE_STARS.iterator();
        while (iterator.hasNext()) {
            ActiveBountyStar star = iterator.next();
            if (star.world != world) {
                continue;
            }
            Entity visualEntity = world.getEntity(star.visualId);
            if (!(visualEntity instanceof CosmicBountyVisualEntity visual)) {
                iterator.remove();
                continue;
            }

            star.age++;
            if (star.exploded) {
                visual.setImplodeTicks(star.implodeTicks + star.age);
                if (star.age >= BURST_VISUAL_TICKS) {
                    visual.discard();
                    iterator.remove();
                }
                continue;
            }

            visual.setImplodeTicks(star.age);
            pullTargets(world, star);
            if (star.age % 10 == 0) {
                playImplodePulse(world, star.pos, star.age / (float) star.implodeTicks);
            }

            if (star.age >= star.implodeTicks) {
                explode(world, star);
                visual.setExploded(true);
                visual.setImplodeTicks(star.implodeTicks);
                star.exploded = true;
                star.age = 0;
            }
        }
        tickStardustFields(world);
    }

    public static boolean isHoldingBountyCosmicBow(LivingEntity living) {
        return isBountyCosmicBow(living.getStackInHand(Hand.MAIN_HAND)) || isBountyCosmicBow(living.getStackInHand(Hand.OFF_HAND));
    }

    private static boolean isBountyCosmicBow(ItemStack stack) {
        return stack != null
                && stack.getItem() instanceof CosmicBowItem
                && BowUpgradeData.from(stack).runeEtching() == RuneEtching.BOUNTY;
    }

    private static void tickStardustFields(ServerWorld world) {
        if (ACTIVE_STARDUST_FIELDS.isEmpty()) {
            return;
        }

        Iterator<ActiveStardustField> iterator = ACTIVE_STARDUST_FIELDS.iterator();
        while (iterator.hasNext()) {
            ActiveStardustField field = iterator.next();
            if (field.world != world) {
                continue;
            }
            field.age++;
            if (field.age % STARDUST_DAMAGE_INTERVAL_TICKS == 0) {
                damageStardustTargets(world, field);
            }
            if (field.age >= field.durationTicks) {
                iterator.remove();
            }
        }
    }

    private static void pullTargets(ServerWorld world, ActiveBountyStar star) {
        Entity owner = star.ownerId == null ? null : world.getEntity(star.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        double radius = star.radius;
        Box box = Box.of(star.pos, radius * 2.0, radius * 2.0, radius * 2.0);
        float progress = Math.min(1.0F, star.age / (float) star.implodeTicks);
        double strength = pullStrength() * (0.45 + progress * 0.85);

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, target ->
                target.isAlive()
                        && target.squaredDistanceTo(star.pos) <= radius * radius
                        && CombatTargeting.isOffensiveTargetCandidate(target, ownerLiving))) {
            Vec3d toCenter = star.pos.subtract(target.getPos());
            double distance = Math.max(0.75, toCenter.length());
            Vec3d pull = toCenter.normalize().multiply(strength * (1.0 - Math.min(0.85, distance / (radius * 1.2))));
            target.addVelocity(pull.x, Math.max(0.015, pull.y * 0.35), pull.z);
            target.velocityModified = true;
        }

        if (star.age % 4 == 0) {
            double particleRadius = Math.max(0.4, radius * (1.0 - progress * 0.75));
            world.spawnParticles(ParticleTypes.END_ROD, star.pos.x, star.pos.y, star.pos.z, 5, particleRadius * 0.25, 0.18, particleRadius * 0.25, -0.02);
            world.spawnParticles(ParticleTypes.SMALL_FLAME, star.pos.x, star.pos.y, star.pos.z, 3, particleRadius * 0.18, 0.12, particleRadius * 0.18, 0.0);
        }
    }

    private static void spawnStardustBurst(ServerWorld world, ActiveStardustField field) {
        double radius = field.radius;
        int count = Math.max(120, (int) Math.min(520, radius * 20.0));
        double topY = field.pos.y + 1.5;
        double fallHeight = stardustFallHeight(radius);
        double topBandHeight = Math.min(2.5, fallHeight * 0.12);
        double visualRadius = radius * 0.45;
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(world.random.nextDouble()) * visualRadius;
            double x = field.pos.x + Math.cos(angle) * distance;
            double z = field.pos.z + Math.sin(angle) * distance;
            double y = topY - world.random.nextDouble() * topBandHeight;
            world.spawnParticles(ParticleRegistry.LONG_END_ROD.get(), x, y, z, 1, 0.003, 0.028, 0.003, -0.14);
            if (world.random.nextFloat() < 0.45F) {
                world.spawnParticles(ParticleRegistry.LONG_FIREWORK.get(), x, y + 0.15, z, 1, 0.002, 0.020, 0.002, -0.10);
            }
        }
        world.spawnParticles(ParticleRegistry.LONG_END_ROD.get(), field.pos.x, topY, field.pos.z, 40, radius * 0.06, 0.12, radius * 0.06, -0.12);
    }

    private static void damageStardustTargets(ServerWorld world, ActiveStardustField field) {
        Entity owner = field.ownerId == null ? null : world.getEntity(field.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        double radius = field.radius;
        double height = stardustFallHeight(radius);
        Box box = new Box(
                field.pos.x - radius,
                field.pos.y - height,
                field.pos.z - radius,
                field.pos.x + radius,
                field.pos.y + 1.5,
                field.pos.z + radius
        );
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, target ->
                target.isAlive()
                        && horizontalDistanceSq(target.getPos(), field.pos) <= radius * radius
                        && target.getY() <= field.pos.y + 1.5
                        && CombatTargeting.isOffensiveTargetCandidate(target, ownerLiving))) {
            CombatTargeting.applyDamage(world, owner, target, field.tickDamage, true, false);
        }
    }

    private static void explode(ServerWorld world, ActiveBountyStar star) {
        Entity owner = star.ownerId == null ? null : world.getEntity(star.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        Box box = Box.of(star.pos, star.radius * 2.0, star.radius * 2.0, star.radius * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, target ->
                target.isAlive()
                        && target.squaredDistanceTo(star.pos) <= star.radius * star.radius
                        && CombatTargeting.isOffensiveTargetCandidate(target, ownerLiving))) {
            double distance = Math.max(0.5, target.getPos().distanceTo(star.pos));
            float falloff = (float) (1.0 - Math.min(0.7, distance / star.radius * 0.55));
            CombatTargeting.applyDamage(world, owner, target, star.damage * falloff, true, false);
            Vec3d knockback = target.getPos().subtract(star.pos).normalize().multiply(1.05 + (star.radius * 0.08));
            target.addVelocity(knockback.x, 0.42, knockback.z);
            target.velocityModified = true;
        }

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, star.pos.x, star.pos.y, star.pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.END_ROD, star.pos.x, star.pos.y, star.pos.z, 45, star.radius * 0.22, star.radius * 0.12, star.radius * 0.22, 0.12);
        world.spawnParticles(ParticleTypes.FLAME, star.pos.x, star.pos.y, star.pos.z, 28, star.radius * 0.18, star.radius * 0.08, star.radius * 0.18, 0.08);
        if (star.createStardustField) {
            ActiveStardustField field = new ActiveStardustField(world, star.ownerId, star.pos, star.radius, star.damage * 0.22F, STARDUST_DURATION_TICKS);
            ACTIVE_STARDUST_FIELDS.add(field);
            spawnStardustBurst(world, field);
            playStardustStart(world, star.pos);
        }
        world.playSound(null, star.pos.x, star.pos.y, star.pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.05F, 1.2F);
        world.playSound(null, star.pos.x, star.pos.y, star.pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.55F, 0.72F);
        world.playSound(null, star.pos.x, star.pos.y, star.pos.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.85F, 0.58F);
    }

    private static void playImplodeStart(ServerWorld world, Vec3d pos, float charge) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.55F, 0.65F + charge * 0.25F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.48F, 1.45F + charge * 0.2F);
    }

    private static void playStardustStart(ServerWorld world, Vec3d pos) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.72F, 1.45F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.42F, 1.2F);
    }

    private static void playImplodePulse(ServerWorld world, Vec3d pos, float progress) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundCategory.PLAYERS, 0.32F, 0.75F + progress * 0.55F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.20F, 0.58F + progress * 0.18F);
    }

    private static double lerp(double min, double max, double t) {
        return min + (max - min) * t;
    }

    private static double horizontalDistanceSq(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static double stardustFallHeight(double radius) {
        return Math.max(STARDUST_MIN_FALL_HEIGHT, radius * STARDUST_FALL_HEIGHT_RADIUS_MULTIPLIER);
    }

    private static boolean isBountyReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startBountyCooldown(ServerWorld world, UUID ownerId) {
        int ticks = Math.max(0, cooldownTicks());
        if (ticks <= 0) {
            return;
        }
        getCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + ticks);
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(BOUNTY_COOLDOWNS_BY_SERVER, world);
    }

    private static class ActiveBountyStar {
        private final ServerWorld world;
        private final UUID visualId;
        private final UUID ownerId;
        private final Vec3d pos;
        private final double radius;
        private final float damage;
        private final int implodeTicks;
        private final boolean createStardustField;
        private int age;
        private boolean exploded;

        private ActiveBountyStar(ServerWorld world, UUID visualId, UUID ownerId, Vec3d pos, double radius, float damage, int implodeTicks, boolean createStardustField) {
            this.world = world;
            this.visualId = visualId;
            this.ownerId = ownerId;
            this.pos = pos;
            this.radius = radius;
            this.damage = damage;
            this.implodeTicks = implodeTicks;
            this.createStardustField = createStardustField;
            this.age = 0;
            this.exploded = false;
        }
    }

    private static class ActiveStardustField {
        private final ServerWorld world;
        private final UUID ownerId;
        private final Vec3d pos;
        private final double radius;
        private final float tickDamage;
        private final int durationTicks;
        private int age;

        private ActiveStardustField(ServerWorld world, UUID ownerId, Vec3d pos, double radius, float tickDamage, int durationTicks) {
            this.world = world;
            this.ownerId = ownerId;
            this.pos = pos;
            this.radius = radius;
            this.tickDamage = tickDamage;
            this.durationTicks = durationTicks;
            this.age = 0;
        }
    }
}
