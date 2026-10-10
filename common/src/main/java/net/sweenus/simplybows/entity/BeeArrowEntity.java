package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.world.BeeChaosHoneyStormManager;
import net.sweenus.simplybows.world.BeeGraceShieldManager;
import net.sweenus.simplybows.world.BeeHiveSwarmManager;
import net.sweenus.simplybows.world.RuneUseCooldown;

import java.util.List;

public class BeeArrowEntity extends ArrowEntity {
    private final net.sweenus.simplybows.util.HomingPursuit pursuit = new net.sweenus.simplybows.util.HomingPursuit();

    private static int basePoisonDuration() { return SimplyBowsConfig.INSTANCE.buzzkill.basePoisonDuration.get(); }
    private static int stringPoisonDurationBonus() { return SimplyBowsConfig.INSTANCE.buzzkill.stringPoisonDurationBonus.get(); }
    private static final int MAX_POISON_LEVELS = 5;
    private static final int MAX_POISON_AMPLIFIER = MAX_POISON_LEVELS - 1;
    private static final int STRING_LEVELS_PER_POISON_STEP = 2;
    private static final double MIN_HORIZONTAL_SPEED_SQ_FOR_YAW = 1.0E-4;
    private static final float ROTATION_SMOOTHING = 0.35F;
    private static double painHomingRadius() { return SimplyBowsConfig.INSTANCE.buzzkill.painHomingRadius.get(); }
    private static int painHomingStartTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.painHomingStartTicks.get(); }
    private static double painHomingAccel() { return SimplyBowsConfig.INSTANCE.buzzkill.painHomingAccel.get(); }
    private static double painMaxSpeed() { return SimplyBowsConfig.INSTANCE.buzzkill.painMaxSpeed.get(); }
    private static final String HIVE_VISUAL_TAG = "simplybows_bee_hive_visual";
    private BowUpgradeData upgrades;
    private boolean spawnSoundPlayed;
    private boolean spawnedBountyHive;
    private boolean chaosHoneyStormOnImpact;
    private boolean spawnedChaosHoneyStorm;
    private boolean chaosDiveBomb;
    private float chaosDiveBombDamage;
    private double chaosDiveBombRadius;
    private boolean hiveBee;
    private boolean fullyDrawnShot;

    public void setFullyDrawnShot(boolean fullyDrawnShot) {
        this.fullyDrawnShot = fullyDrawnShot;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("FullyDrawnShot", this.fullyDrawnShot);
        nbt.put("BowUpgrades", this.upgrades.toProjectileNbt());
        nbt.putBoolean("spawnSoundPlayed", this.spawnSoundPlayed);
        nbt.putBoolean("spawnedBountyHive", this.spawnedBountyHive);
        nbt.putBoolean("chaosHoneyStormOnImpact", this.chaosHoneyStormOnImpact);
        nbt.putBoolean("spawnedChaosHoneyStorm", this.spawnedChaosHoneyStorm);
        nbt.putBoolean("chaosDiveBomb", this.chaosDiveBomb);
        nbt.putFloat("chaosDiveBombDamage", this.chaosDiveBombDamage);
        nbt.putDouble("chaosDiveBombRadius", this.chaosDiveBombRadius);
        nbt.putBoolean("hiveBee", this.hiveBee);
        nbt.putBoolean("painHoming", this.painHoming);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.fullyDrawnShot = nbt.getBoolean("FullyDrawnShot");
        this.upgrades = BowUpgradeData.fromProjectileNbt(nbt.getCompound("BowUpgrades"));
        this.spawnSoundPlayed = nbt.getBoolean("spawnSoundPlayed");
        this.spawnedBountyHive = nbt.getBoolean("spawnedBountyHive");
        this.chaosHoneyStormOnImpact = nbt.getBoolean("chaosHoneyStormOnImpact");
        this.spawnedChaosHoneyStorm = nbt.getBoolean("spawnedChaosHoneyStorm");
        this.chaosDiveBomb = nbt.getBoolean("chaosDiveBomb");
        this.chaosDiveBombDamage = nbt.getFloat("chaosDiveBombDamage");
        this.chaosDiveBombRadius = nbt.getDouble("chaosDiveBombRadius");
        this.hiveBee = nbt.getBoolean("hiveBee");
        this.painHoming = nbt.getBoolean("painHoming");
    }
    private static final ThreadLocal<Boolean> ENABLE_PAIN_HOMING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Float> PAIN_DAMAGE_SCALE = ThreadLocal.withInitial(() -> 1.0F);
    private boolean painHoming;
    private LivingEntity homingTarget;

    public static void setPainHoming(boolean enabled) {
        setPainHoming(enabled, 1.0F);
    }

    public static void setPainHoming(boolean enabled, float damageScale) {
        ENABLE_PAIN_HOMING.set(enabled);
        PAIN_DAMAGE_SCALE.set(enabled ? Math.max(0.05F, damageScale) : 1.0F);
    }

    public static float getPainDamageScale() {
        return PAIN_DAMAGE_SCALE.get();
    }

    public BeeArrowEntity(EntityType<? extends BeeArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
        this.painHoming = false;
    }

    public BeeArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        this(world, owner, arrowStack, BowUpgradeData.from(weaponStack));
        net.sweenus.simplybows.util.BowProjectileEnchantments.initialize(this, weaponStack, arrowStack);
    }

    public BeeArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, BowUpgradeData upgrades) {
        super(EntityRegistry.BEE_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.upgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
        this.painHoming = ENABLE_PAIN_HOMING.get();
    }

    @Override
    public void tick() {
        // Also bound pursuit when teleports keep preventing any collision at all.
        if (!this.inGround && this.painHoming && this.age >= 200) {
            this.discard();
            return;
        }
        super.tick();

        if (!this.getWorld().isClient() && this.painHoming && !this.inGround) {
            updatePainHoming();
            if (this.hiveBee) {
                tryHiveSplashDetonate();
            }
        }

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 1.0E-6) {
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();

            float targetPitch = (float)(-(Math.atan2(velocity.y, velocity.horizontalLength()) * (180F / Math.PI)));
            float pitchDelta = MathHelper.wrapDegrees(targetPitch - this.getPitch());
            this.setPitch(this.getPitch() + pitchDelta * ROTATION_SMOOTHING);

            if (velocity.horizontalLengthSquared() > MIN_HORIZONTAL_SPEED_SQ_FOR_YAW) {
                float targetYaw = (float)(Math.atan2(velocity.z, velocity.x) * (180F / Math.PI)) - 90.0F;
                float yawDelta = MathHelper.wrapDegrees(targetYaw - this.getYaw());
                this.setYaw(this.getYaw() + yawDelta * ROTATION_SMOOTHING);
            }
            this.velocityDirty = true;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld && !this.spawnSoundPlayed) {
            this.spawnSoundPlayed = true;
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_BEE_POLLINATE, SoundCategory.PLAYERS, 0.4F, 1.0F + this.random.nextFloat() * 0.2F);
        }

        if (this.inGround && this.getWorld() instanceof ServerWorld serverWorld) {
            spawnPoofAndDiscard(serverWorld);
            return;
        }
    }

    private void updatePainHoming() {
        if (this.age < 2) {
            return;
        }
        // Hard lifetime so missed bees cannot orbit forever / spam particles.
        int maxAge = this.hiveBee ? 100 : 45;
        if (this.age > maxAge) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                if (this.hiveBee) {
                    performHiveSplash(serverWorld, this.getPos());
                } else {
                    spawnPoofAndDiscard(serverWorld);
                }
            } else {
                this.discard();
            }
            return;
        }

        if (this.homingTarget == null || !this.homingTarget.isAlive()) {
            this.homingTarget = findNearestPainTarget();
        }
        if (this.homingTarget == null) {
            if (this.hiveBee) {
                wanderHiveBee();
            } else {
                this.setNoGravity(false);
            }
            return;
        }

        if (pursuit.targetTeleported(this.homingTarget)) { this.discard(); return; }
        this.setNoGravity(true);
        Vec3d targetPos = this.homingTarget.getPos().add(0.0, this.homingTarget.getStandingEyeHeight() * 0.65, 0.0);
        Vec3d direction = targetPos.subtract(this.getPos());
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }

        double speedCap = painMaxSpeed();
        double speed = MathHelper.clamp(Math.min(this.getVelocity().length(), speedCap), 0.45, speedCap);
        Vec3d desired = direction.normalize().multiply(speed);
        Vec3d steered = this.getVelocity().lerp(desired, painHomingAccel());
        this.setVelocity(steered);
        this.velocityDirty = true;
    }

    public void lockHiveTarget(LivingEntity target) {
        this.homingTarget = target;
    }

    /** No target yet — fly in a lazy curve and keep scanning. */
    private void wanderHiveBee() {
        this.setNoGravity(true);
        Vec3d vel = this.getVelocity();
        if (vel.lengthSquared() < 0.04) {
            double ang = this.random.nextDouble() * Math.PI * 2.0;
            vel = new Vec3d(Math.cos(ang), 0.12, Math.sin(ang)).multiply(0.55);
        }
        if (this.age % 8 == 0) {
            double yaw = (this.random.nextDouble() - 0.5) * 0.9;
            double pitch = (this.random.nextDouble() - 0.5) * 0.35;
            Vec3d turn = vel.normalize()
                    .add(Math.cos(yaw) * 0.35, pitch, Math.sin(yaw) * 0.35)
                    .normalize()
                    .multiply(MathHelper.clamp(vel.length(), 0.4, painMaxSpeed() * 0.75));
            this.setVelocity(turn);
        } else {
            this.setVelocity(vel.multiply(0.98).add(0.0, Math.sin(this.age * 0.25) * 0.01, 0.0));
        }
        this.velocityDirty = true;
    }

    private LivingEntity findNearestPainTarget() {
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return null;
        }

        double radius = this.hiveBee
                ? SimplyBowsConfig.INSTANCE.buzzkill.bountyTargetRadius.get()
                : painHomingRadius();
        Box searchBox = this.getBoundingBox().expand(radius);
        List<LivingEntity> candidates = this.getWorld().getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity)
                        && entity != ownerLiving
                        && CombatTargeting.checkFriendlyFire(entity, ownerLiving));

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double dist = this.squaredDistanceTo(candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);
        if (painHoming && !hiveBee && getWorld() instanceof ServerWorld world) {
            net.sweenus.simplybows.world.RuneEffectManager.cast(world, net.sweenus.simplybows.util.RuneEffectRules.SWARM,
                    target.getPos(), getOwner(), target, upgrades, false);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.chaosDiveBomb) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                performChaosDiveBombImpact(serverWorld, entityHitResult.getPos());
            } else {
                this.discard();
            }
            return;
        }

        if (!(entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            trySpawnChaosHoneyStorm(entityHitResult.getPos());
            trySpawnBountyHive(entityHitResult.getPos());
            return;
        }

        if (isGraceSupportTarget(livingEntity)) {
            tryApplyGraceShield(entityHitResult.getPos(), livingEntity);
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_BEE_POLLINATE, SoundCategory.PLAYERS, 0.85F, 1.1F + this.random.nextFloat() * 0.15F);
                serverWorld.spawnParticles(ParticleTypes.WAX_ON, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 10, 0.2, 0.18, 0.2, 0.01);
                spawnPoofAndDiscard(serverWorld);
            } else {
                this.discard();
            }
            return;
        }

        // Create the impact ability before vanilla damage can kill the target.
        if (!this.hiveBee) {
            trySpawnChaosHoneyStorm(entityHitResult.getPos());
            trySpawnBountyHive(entityHitResult.getPos());
        }
        if (!this.painHoming) {
            livingEntity.hurtTime = 0;
            livingEntity.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
        // A homing shot gets one impact attempt, including rejected hits on modded enemies.
        if (this.painHoming) {
            this.discard();
            return;
        }
        if (!this.painHoming) {
            livingEntity.hurtTime = 0;
            livingEntity.timeUntilRegen = 0;
        }

        if (this.hiveBee) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                performHiveSplash(serverWorld, livingEntity.getPos().add(0.0, livingEntity.getStandingEyeHeight() * 0.4, 0.0));
            } else {
                this.discard();
            }
            return;
        }

        // Full-draw poison stacks.
        if ((this.fullyDrawnShot || this.isCritical()) && livingEntity.isAlive()
                && this.upgrades.runeEtching() != RuneEtching.GRACE && !isFriendlyToOwner(livingEntity)) {
            applyStackingPoison(livingEntity);
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
            serverWorld.spawnParticles(ParticleTypes.POOF, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 10, 0.15, 0.12, 0.15, 0.02);
            serverWorld.spawnParticles(ParticleTypes.FALLING_HONEY, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
            serverWorld.spawnParticles(ParticleTypes.CRIT, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 6, 0.15, 0.15, 0.15, 0.02);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.chaosDiveBomb) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                performChaosDiveBombImpact(serverWorld, blockHitResult.getPos());
            } else {
                this.discard();
            }
            return;
        }

        super.onBlockHit(blockHitResult);
        tryApplyGraceShield(blockHitResult.getPos(), null);
        trySpawnChaosHoneyStorm(blockHitResult.getPos());
        trySpawnBountyHive(blockHitResult.getPos());
    }

    @Override
    public boolean canHit(net.minecraft.entity.Entity entity) {
        if (entity.getCommandTags().contains(HIVE_VISUAL_TAG) || entity.getCommandTags().contains(BeeGraceShieldManager.GRACE_VISUAL_TAG)) {
            return false;
        }
        return super.canHit(entity);
    }

    private void trySpawnBountyHive(Vec3d hitPos) {
        if (this.spawnedBountyHive || this.upgrades.runeEtching() != RuneEtching.BOUNTY) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return;
        }
        if (!RuneUseCooldown.isReady(serverWorld, ownerLiving.getUuid(), "bee-bounty")) {
            return;
        }
        BeeHiveSwarmManager.createHive(serverWorld, hitPos, ownerLiving, this.upgrades);
        int hiveTicks = BeeHiveSwarmManager.hiveDurationTicks(this.upgrades);
        RuneUseCooldown.startForEffect(serverWorld, ownerLiving.getUuid(), "bee-bounty", "bee", hiveTicks);
        this.spawnedBountyHive = true;
    }

    private void trySpawnChaosHoneyStorm(Vec3d hitPos) {
        if (this.spawnedChaosHoneyStorm || !this.chaosHoneyStormOnImpact) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        BeeChaosHoneyStormManager.spawnAtImpact(
                serverWorld,
                hitPos,
                this.getOwner() != null ? this.getOwner().getUuid() : null,
                this.upgrades.stringLevel(),
                this.upgrades.frameLevel()
        );
        this.spawnedChaosHoneyStorm = true;
        this.chaosHoneyStormOnImpact = false;
    }

    public void setChaosHoneyStormOnImpact(boolean chaosHoneyStormOnImpact) {
        this.chaosHoneyStormOnImpact = chaosHoneyStormOnImpact;
    }

    public void setChaosDiveBomb(float damage, double radius) {
        this.chaosDiveBomb = true;
        this.chaosDiveBombDamage = Math.max(0.0F, damage);
        this.chaosDiveBombRadius = Math.max(0.25, radius);
    }

    public void setHiveBee(boolean hiveBee) {
        this.hiveBee = hiveBee;
    }

    private void tryHiveSplashDetonate() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (this.homingTarget != null && this.homingTarget.isAlive()
                && this.squaredDistanceTo(this.homingTarget) <= 1.6 * 1.6) {
            performHiveSplash(serverWorld, this.homingTarget.getPos().add(0.0, this.homingTarget.getStandingEyeHeight() * 0.4, 0.0));
            return;
        }
        // Proximity splash even without a locked target.
        Box box = this.getBoundingBox().expand(1.35);
        for (LivingEntity candidate : serverWorld.getEntitiesByClass(LivingEntity.class, box, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity) && !isFriendlyToOwner(entity))) {
            if (this.squaredDistanceTo(candidate) <= 1.45 * 1.45) {
                performHiveSplash(serverWorld, candidate.getPos().add(0.0, candidate.getStandingEyeHeight() * 0.4, 0.0));
                return;
            }
        }
    }

    private void performHiveSplash(ServerWorld world, Vec3d impactPos) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        double radius = 1.65;
        float damage = (float) Math.max(1.0, this.getDamage());
        Box box = Box.of(impactPos, radius * 2.0, 2.8, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity))) {
            if (candidate.squaredDistanceTo(impactPos) > radius * radius) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            candidate.hurtTime = 0;
            candidate.timeUntilRegen = 0;
            CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            applyHivePoison(candidate);
        }
        world.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.95F, 0.9F + this.random.nextFloat() * 0.15F);
        world.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4F, 1.55F);
        world.spawnParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.POOF, impactPos.x, impactPos.y + 0.1, impactPos.z, 14, radius * 0.35, 0.12, radius * 0.35, 0.02);
        world.spawnParticles(ParticleTypes.FALLING_HONEY, impactPos.x, impactPos.y + 0.15, impactPos.z, 12, radius * 0.3, 0.12, radius * 0.3, 0.0);
        world.spawnParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y + 0.1, impactPos.z, 10, radius * 0.25, 0.15, radius * 0.25, 0.02);
        this.discard();
    }

    /** Frame sets poison level (I–III). Hits stack the amplifier further. */
    private void applyHivePoison(LivingEntity target) {
        int frameAmp = Math.min(2, Math.max(0, this.upgrades.frameLevel())); // Frame 0=I, 1=II, 2+=III
        int amplifier = frameAmp;
        StatusEffectInstance existing = target.getStatusEffect(StatusEffects.POISON);
        if (existing != null) {
            amplifier = Math.min(MAX_POISON_AMPLIFIER, existing.getAmplifier() + 1 + frameAmp / 2);
        }
        int duration = basePoisonDuration() + 20 + this.upgrades.frameLevel() * 15;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, amplifier), this.getOwner());
    }

    private void performChaosDiveBombImpact(ServerWorld world, Vec3d impactPos) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;

        Box box = Box.of(impactPos, this.chaosDiveBombRadius * 2.0, 3.0, this.chaosDiveBombRadius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity))) {
            if (candidate.squaredDistanceTo(impactPos) > this.chaosDiveBombRadius * this.chaosDiveBombRadius) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            CombatTargeting.applyDamage(world, owner, candidate, this.chaosDiveBombDamage, true, false);
        }

        world.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.95F, 0.85F + this.random.nextFloat() * 0.2F);
        world.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5F, 1.5F + this.random.nextFloat() * 0.1F);
        world.spawnParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.POOF, impactPos.x, impactPos.y + 0.1, impactPos.z, 12, this.chaosDiveBombRadius * 0.4, 0.1, this.chaosDiveBombRadius * 0.4, 0.01);
        world.spawnParticles(ParticleTypes.FALLING_HONEY, impactPos.x, impactPos.y + 0.2, impactPos.z, 9, this.chaosDiveBombRadius * 0.35, 0.1, this.chaosDiveBombRadius * 0.35, 0.0);
        this.discard();
    }

    private void applyStackingPoison(LivingEntity target) {
        int poisonStep = 1 + (this.upgrades.stringLevel() / STRING_LEVELS_PER_POISON_STEP);
        int amplifier = Math.min(MAX_POISON_AMPLIFIER, poisonStep - 1);
        StatusEffectInstance existing = target.getStatusEffect(StatusEffects.POISON);
        if (existing != null) {
            amplifier = Math.min(MAX_POISON_AMPLIFIER, existing.getAmplifier() + poisonStep);
        }
        int duration = basePoisonDuration() + this.upgrades.stringLevel() * stringPoisonDurationBonus();
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, amplifier), this.getOwner());
    }

    private void tryApplyGraceShield(Vec3d hitPos, LivingEntity struck) {
        if (this.upgrades.runeEtching() != RuneEtching.GRACE) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return;
        }
        BeeGraceShieldManager.tryApplyFromImpact(serverWorld, hitPos, ownerLiving, this.upgrades, struck);
    }

    private boolean isFriendlyToOwner(LivingEntity entity) {
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return false;
        }
        return CombatTargeting.isFriendlyTo(entity, ownerLiving);
    }

    private boolean isGraceSupportTarget(LivingEntity livingEntity) {
        if (this.upgrades.runeEtching() != RuneEtching.GRACE || livingEntity == null) {
            return false;
        }
        return livingEntity instanceof PlayerEntity
                || livingEntity instanceof AnimalEntity
                || livingEntity instanceof IronGolemEntity
                || livingEntity instanceof VillagerEntity;
    }

    public boolean isGraceSupportProjectile() {
        return this.upgrades.runeEtching() == RuneEtching.GRACE;
    }

    private void spawnPoofAndDiscard(ServerWorld world) {
        world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.1, this.getZ(), 10, 0.15, 0.12, 0.15, 0.02);
        world.spawnParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 0.1, this.getZ(), 4, 0.12, 0.08, 0.12, 0.0);
        this.discard();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected SoundEvent getHitSound() {
        return SoundEvents.ENTITY_BEE_STING;
    }

    private static ItemStack sanitizeArrowStack(ItemStack arrowStack) {
        if (arrowStack == null || arrowStack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }
        ItemStack copy = arrowStack.copy();
        copy.setCount(1);
        return copy;
    }
}
