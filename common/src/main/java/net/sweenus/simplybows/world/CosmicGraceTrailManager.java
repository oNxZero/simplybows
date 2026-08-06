package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;
import net.sweenus.simplybows.entity.CosmicTetherVisualEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmicGraceTrailManager {

    private static final int BUFF_REFRESH_TICKS = 10;
    private static final int VISUAL_DURATION_PADDING_TICKS = 10;
    private static final int FIELD_DURATION_BONUS_PER_STRING = 40;
    private static final int FIELD_AMBIENT_SOUND_MIN_INTERVAL_TICKS = 32;
    private static final int FIELD_AMBIENT_SOUND_RANDOM_INTERVAL_TICKS = 26;
    private static final int COCOON_ROLL_INTERVAL_TICKS = 20;
    private static final double FIELD_ORBIT_RADIUS = 4.25;
    private static final double FIELD_ORBIT_SPEED = 0.105;
    private static final double FIELD_RADIUS_BONUS_PER_FRAME = 1.0;
    private static final Map<ServerWorld, List<ActiveGraceField>> ACTIVE_FIELDS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveCocoon>> ACTIVE_COCOONS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> FIELD_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private CosmicGraceTrailManager() {
    }

    private static int fieldDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldDurationTicks.get(); }
    private static int fieldCooldownTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldCooldownTicks.get(); }
    private static double fieldRadius() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldRadius.get(); }
    private static int buffDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldBuffDurationTicks.get(); }
    private static int buffAmplifier() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldBuffAmplifier.get(); }
    private static float cocoonChance() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldCocoonChance.get(); }
    private static float cocoonNightChanceBonus() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldCocoonNightChanceBonus.get(); }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveGraceField> fields = ACTIVE_FIELDS.get(world);
        List<ActiveCocoon> cocoons = ACTIVE_COCOONS.get(world);
        return (fields != null && !fields.isEmpty())
                || (cocoons != null && !cocoons.isEmpty())
                || !getCooldowns(world).isEmpty()
                || (world.getTime() % 20L == 0L);
    }

    public static void createField(ServerWorld world, Entity owner, Vec3d pos, BowUpgradeData upgrades) {
        if (world == null || owner == null || pos == null || owner.isRemoved()) {
            return;
        }
        UUID ownerId = owner.getUuid();
        if (!isFieldReady(world, ownerId)) {
            return;
        }

        BowUpgradeData fieldUpgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
        int duration = Math.max(1, fieldDurationTicks() + fieldUpgrades.stringLevel() * FIELD_DURATION_BONUS_PER_STRING);
        double radius = Math.max(0.1, fieldRadius() + fieldUpgrades.frameLevel() * FIELD_RADIUS_BONUS_PER_FRAME);
        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, pos.x, pos.y, pos.z);
        visual.setFieldMode(true);
        visual.setFieldRadius((float) radius);
        visual.setLifetimeTicks(duration + VISUAL_DURATION_PADDING_TICKS);
        if (!world.spawnEntity(visual)) {
            return;
        }

        ActiveGraceField field = new ActiveGraceField(UUID.randomUUID(), visual.getUuid(), pos, ownerId, radius, world.getTime(), world.getTime() + duration);
        ACTIVE_FIELDS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(field);

        startFieldCooldown(world, ownerId);
        if (owner instanceof ServerPlayerEntity player) {
            int cooldownTicks = Math.max(20, fieldCooldownTicks());
            SimplyBowItem.simplybows$sendCooldownPacket(player, "cosmic", System.currentTimeMillis() + cooldownTicks * 50L, cooldownTicks);
        }
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveGraceField> fields = ACTIVE_FIELDS.get(world);
        tickCocoons(world);
        if (fields == null || fields.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveGraceField> iterator = fields.iterator();
        while (iterator.hasNext()) {
            ActiveGraceField field = iterator.next();
            if (field.expiresAt <= now) {
                expireCocoonsForField(world, field.fieldId);
                iterator.remove();
                continue;
            }
            if (now >= field.nextBuffTick) {
                field.nextBuffTick = now + BUFF_REFRESH_TICKS;
                applyFieldBuffs(world, field);
            }
            if (now >= field.nextAmbientSoundTick) {
                playAmbientSound(world, field);
                field.nextAmbientSoundTick = now + randomAmbientSoundInterval(world);
            }
            if (now >= field.nextCocoonRollTick) {
                field.nextCocoonRollTick = now + COCOON_ROLL_INTERVAL_TICKS;
                tryApplyCocoons(world, field);
            }
        }

        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
        }
    }

    private static void applyFieldBuffs(ServerWorld world, ActiveGraceField field) {
        Entity owner = world.getEntity(field.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        if (ownerLiving == null || !ownerLiving.isAlive()) {
            return;
        }

        double radius = Math.max(0.1, field.radius);
        Box searchBox = Box.of(field.pos, radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && entity.squaredDistanceTo(field.pos) <= radius * radius
                        && CombatTargeting.isFriendlyTo(entity, ownerLiving));

        int duration = Math.max(BUFF_REFRESH_TICKS + 5, buffDurationTicks());
        int amplifier = Math.max(0, buffAmplifier());
        for (LivingEntity living : entities) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, amplifier, true, true, true), owner);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, true, true, true), owner);
        }
    }

    public static boolean consumeCocoon(ServerWorld world, LivingEntity target, float negatedDamage) {
        List<ActiveCocoon> cocoons = ACTIVE_COCOONS.get(world);
        if (world == null || target == null || cocoons == null || cocoons.isEmpty()) {
            return false;
        }

        for (int i = 0; i < cocoons.size(); i++) {
            ActiveCocoon cocoon = cocoons.get(i);
            if (!target.getUuid().equals(cocoon.targetId)) {
                continue;
            }
            cocoons.remove(i);
            discardVisual(world, cocoon.visualId);
            if (negatedDamage > 0.0F && target.isAlive()) {
                target.heal(negatedDamage);
            }
            playCocoonExpireSound(world, target);
            if (cocoons.isEmpty()) {
                ACTIVE_COCOONS.remove(world);
            }
            return true;
        }
        return false;
    }

    private static void tryApplyCocoons(ServerWorld world, ActiveGraceField field) {
        Entity owner = world.getEntity(field.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        if (ownerLiving == null || !ownerLiving.isAlive()) {
            return;
        }

        double radius = Math.max(0.1, field.radius);
        Box searchBox = Box.of(field.pos, radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && entity.squaredDistanceTo(field.pos) <= radius * radius
                        && CombatTargeting.isFriendlyTo(entity, ownerLiving));
        float chance = effectiveCocoonChance(world);
        for (LivingEntity living : entities) {
            if (hasCocoon(world, living.getUuid()) || world.random.nextFloat() >= chance) {
                continue;
            }
            applyCocoon(world, field, living);
        }
    }

    private static void applyCocoon(ServerWorld world, ActiveGraceField field, LivingEntity target) {
        Vec3d targetPos = target.getBoundingBox().getCenter();
        Vec3d start = tetherStart(world, field);
        CosmicTetherVisualEntity visual = new CosmicTetherVisualEntity(world, targetPos, start);
        visual.setCocoonMode(true);
        if (!world.spawnEntity(visual)) {
            return;
        }

        ACTIVE_COCOONS.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new ActiveCocoon(field.fieldId, target.getUuid(), visual.getUuid()));
        playCocoonApplySound(world, target);
    }

    private static void tickCocoons(ServerWorld world) {
        List<ActiveCocoon> cocoons = ACTIVE_COCOONS.get(world);
        if (cocoons == null || cocoons.isEmpty()) {
            return;
        }

        Iterator<ActiveCocoon> iterator = cocoons.iterator();
        while (iterator.hasNext()) {
            ActiveCocoon cocoon = iterator.next();
            ActiveGraceField field = getField(world, cocoon.fieldId);
            LivingEntity target = getLivingEntity(world, cocoon.targetId);
            Entity visualEntity = world.getEntity(cocoon.visualId);
            if (field == null || target == null || !target.isAlive() || !(visualEntity instanceof CosmicTetherVisualEntity visual)) {
                if (target != null) {
                    playCocoonExpireSound(world, target);
                }
                discardVisual(world, cocoon.visualId);
                iterator.remove();
                continue;
            }
            double radius = Math.max(0.1, field.radius);
            if (target.squaredDistanceTo(field.pos) > radius * radius) {
                playCocoonExpireSound(world, target);
                discardVisual(world, cocoon.visualId);
                iterator.remove();
                continue;
            }

            Vec3d targetPos = target.getBoundingBox().getCenter();
            Vec3d start = tetherStart(world, field);
            visual.setPos(targetPos.x, targetPos.y, targetPos.z);
            visual.setEndPos(start);
            visual.setCocoonMode(true);
        }

        if (cocoons.isEmpty()) {
            ACTIVE_COCOONS.remove(world);
        }
    }

    private static void expireCocoonsForField(ServerWorld world, UUID fieldId) {
        List<ActiveCocoon> cocoons = ACTIVE_COCOONS.get(world);
        if (cocoons == null || cocoons.isEmpty()) {
            return;
        }

        Iterator<ActiveCocoon> iterator = cocoons.iterator();
        while (iterator.hasNext()) {
            ActiveCocoon cocoon = iterator.next();
            if (!fieldId.equals(cocoon.fieldId)) {
                continue;
            }
            LivingEntity target = getLivingEntity(world, cocoon.targetId);
            if (target != null) {
                playCocoonExpireSound(world, target);
            }
            discardVisual(world, cocoon.visualId);
            iterator.remove();
        }
        if (cocoons.isEmpty()) {
            ACTIVE_COCOONS.remove(world);
        }
    }

    private static boolean hasCocoon(ServerWorld world, UUID targetId) {
        List<ActiveCocoon> cocoons = ACTIVE_COCOONS.get(world);
        if (cocoons == null || cocoons.isEmpty()) {
            return false;
        }
        for (ActiveCocoon cocoon : cocoons) {
            if (targetId.equals(cocoon.targetId)) {
                return true;
            }
        }
        return false;
    }

    private static ActiveGraceField getField(ServerWorld world, UUID fieldId) {
        List<ActiveGraceField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        for (ActiveGraceField field : fields) {
            if (field.fieldId.equals(fieldId)) {
                return field;
            }
        }
        return null;
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID entityId) {
        Entity entity = world.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static Vec3d tetherStart(ServerWorld world, ActiveGraceField field) {
        Entity visualEntity = world.getEntity(field.visualId);
        int age = visualEntity == null ? (int) Math.max(0, world.getTime() - field.spawnTick) : visualEntity.age;
        double angle = age * FIELD_ORBIT_SPEED;
        double height = 0.45 + Math.sin(angle * 2.1) * 0.34;
        double radius = Math.max(FIELD_ORBIT_RADIUS, field.radius);
        return field.pos.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    private static float effectiveCocoonChance(ServerWorld world) {
        float chance = cocoonChance();
        if (world.isNight()) {
            chance += cocoonNightChanceBonus();
        }
        return Math.max(0.0F, Math.min(1.0F, chance));
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void playCocoonApplySound(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getBoundingBox().getCenter();
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.65F, 1.55F + world.random.nextFloat() * 0.16F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.28F, 1.35F + world.random.nextFloat() * 0.12F);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 8, 0.28, 0.42, 0.28, 0.0);
    }

    private static void playCocoonExpireSound(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getBoundingBox().getCenter();
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 0.55F, 1.25F + world.random.nextFloat() * 0.14F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.18F, 1.75F + world.random.nextFloat() * 0.12F);
        world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 6, 0.22, 0.28, 0.22, 0.01);
    }

    private static void playAmbientSound(ServerWorld world, ActiveGraceField field) {
        float chimePitch = 1.15F + world.random.nextFloat() * 0.35F;
        float beaconPitch = 0.75F + world.random.nextFloat() * 0.18F;
        world.playSound(null, field.pos.x, field.pos.y, field.pos.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.28F, chimePitch);
        world.playSound(null, field.pos.x, field.pos.y, field.pos.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.16F, beaconPitch);
    }

    private static int randomAmbientSoundInterval(ServerWorld world) {
        return FIELD_AMBIENT_SOUND_MIN_INTERVAL_TICKS + world.random.nextInt(FIELD_AMBIENT_SOUND_RANDOM_INTERVAL_TICKS + 1);
    }

    private static boolean isFieldReady(ServerWorld world, UUID ownerId) {
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startFieldCooldown(ServerWorld world, UUID ownerId) {
        getCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + Math.max(20, fieldCooldownTicks()));
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(FIELD_COOLDOWNS_BY_SERVER, world);
    }

    private static class ActiveGraceField {
        private final UUID fieldId;
        private final UUID visualId;
        private final Vec3d pos;
        private final UUID ownerId;
        private final double radius;
        private final long spawnTick;
        private final long expiresAt;
        private long nextBuffTick;
        private long nextAmbientSoundTick;
        private long nextCocoonRollTick;

        private ActiveGraceField(UUID fieldId, UUID visualId, Vec3d pos, UUID ownerId, double radius, long spawnTick, long expiresAt) {
            this.fieldId = fieldId;
            this.visualId = visualId;
            this.pos = pos;
            this.ownerId = ownerId;
            this.radius = radius;
            this.spawnTick = spawnTick;
            this.expiresAt = expiresAt;
            this.nextBuffTick = 0;
            this.nextAmbientSoundTick = 0;
            this.nextCocoonRollTick = 0;
        }
    }

    private record ActiveCocoon(UUID fieldId, UUID targetId, UUID visualId) {
    }
}
