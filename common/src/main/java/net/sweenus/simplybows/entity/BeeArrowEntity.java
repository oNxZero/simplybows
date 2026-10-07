package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

import java.util.List;

public class BeeArrowEntity extends ArrowEntity {

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
    private final BowUpgradeData upgrades;
    private boolean spawnSoundPlayed;
    private boolean spawnedBountyHive;
    private boolean chaosHoneyStormOnImpact;
    private boolean spawnedChaosHoneyStorm;
    private boolean chaosDiveBomb;
    private float chaosDiveBombDamage;
    private double chaosDiveBombRadius;
    private LivingEntity homingTarget;

    public BeeArrowEntity(EntityType<? extends BeeArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public BeeArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        this(world, owner, arrowStack, BowUpgradeData.from(weaponStack));
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
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient() && this.upgrades.runeEtching() == RuneEtching.PAIN && !this.inGround) {
            updatePainHoming();
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
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_BEE_HURT, SoundCategory.PLAYERS, 0.6F, 1.0F + this.random.nextFloat() * 0.2F);
        }

        if (this.inGround && this.getWorld() instanceof ServerWorld serverWorld) {
            spawnPoofAndDiscard(serverWorld);
            return;
        }
    }

    private void updatePainHoming() {
        if (this.age < painHomingStartTicks()) {
            return;
        }

        if (this.homingTarget == null || !this.homingTarget.isAlive()) {
            this.homingTarget = findNearestPainTarget();
        }
        if (this.homingTarget == null) {
            return;
        }

        Vec3d targetPos = this.homingTarget.getPos().add(0.0, this.homingTarget.getStandingEyeHeight(), 0.0);
        Vec3d direction = targetPos.subtract(this.getPos());
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }

        Vec3d newVelocity = this.getVelocity().add(direction.normalize().multiply(painHomingAccel()));
        if (newVelocity.lengthSquared() > painMaxSpeed() * painMaxSpeed()) {
            newVelocity = newVelocity.normalize().multiply(painMaxSpeed());
        }
        this.setVelocity(newVelocity);
        this.velocityDirty = true;
    }

    private LivingEntity findNearestPainTarget() {
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return null;
        }

        Box searchBox = this.getBoundingBox().expand(painHomingRadius());
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
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.chaosDiveBomb) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                performChaosDiveBombImpact(serverWorld, entityHitResult.getPos());
            } else {
                this.discard();
            }
            return;
        }

        if (entityHitResult.getEntity() instanceof LivingEntity livingEntity && shouldIgnoreGraceDamage(livingEntity)) {
            tryApplyGraceShield(entityHitResult.getPos());
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_BEE_HURT, SoundCategory.PLAYERS, 0.8F, 1.0F + this.random.nextFloat() * 0.2F);
                spawnPoofAndDiscard(serverWorld);
            } else {
                this.discard();
            }
            return;
        }

        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }

        if (!(entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            trySpawnChaosHoneyStorm(entityHitResult.getPos());
            return;
        }

        tryApplyGraceShield(entityHitResult.getPos());

        if (!isFriendlyToOwner(livingEntity)) {
            applyStackingPoison(livingEntity);
        }
        trySpawnChaosHoneyStorm(entityHitResult.getPos());
        trySpawnBountyHive(entityHitResult.getPos());

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_BEE_HURT, SoundCategory.PLAYERS, 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
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
        tryApplyGraceShield(blockHitResult.getPos());
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
        BeeHiveSwarmManager.createHive(serverWorld, hitPos, ownerLiving, this.upgrades);
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

    private void tryApplyGraceShield(Vec3d hitPos) {
        if (this.upgrades.runeEtching() != RuneEtching.GRACE) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return;
        }
        BeeGraceShieldManager.tryApplyFromImpact(serverWorld, hitPos, ownerLiving, this.upgrades);
    }

    private boolean isFriendlyToOwner(LivingEntity entity) {
        if (!(this.getOwner() instanceof LivingEntity ownerLiving)) {
            return false;
        }
        return CombatTargeting.isFriendlyTo(entity, ownerLiving);
    }

    private boolean shouldIgnoreGraceDamage(LivingEntity livingEntity) {
        return this.upgrades.runeEtching() == RuneEtching.GRACE && livingEntity instanceof VillagerEntity;
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
        return SoundEvents.ENTITY_BEE_HURT;
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
