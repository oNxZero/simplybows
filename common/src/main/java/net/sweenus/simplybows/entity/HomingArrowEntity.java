package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.GraceProjectile;
import net.sweenus.simplybows.world.IceChaosWallManager;
import net.sweenus.simplybows.world.IceFrostBloomManager;

import java.util.List;
import java.util.UUID;

public class HomingArrowEntity extends ArrowEntity {

    private static double homingRadius() { return SimplyBowsConfig.INSTANCE.winterfang.homingRadius.get(); }
    private static double homingAccel() { return SimplyBowsConfig.INSTANCE.winterfang.homingAccel.get(); }
    private static int homingStartTicks() { return SimplyBowsConfig.INSTANCE.winterfang.homingStartTicks.get(); }
    private static float initialSpreadYawRadians() { return SimplyBowsConfig.INSTANCE.winterfang.initialSpreadYaw.get(); }
    private static float initialSpreadPitchRadians() { return SimplyBowsConfig.INSTANCE.winterfang.initialSpreadPitch.get(); }
    private static double startSpeed() { return SimplyBowsConfig.INSTANCE.winterfang.startSpeed.get(); }
    private static double maxSpeed() { return SimplyBowsConfig.INSTANCE.winterfang.maxSpeed.get(); }
    private static int speedRampTicks() { return SimplyBowsConfig.INSTANCE.winterfang.speedRampTicks.get(); }
    private LivingEntity target;
    private boolean initialSpreadApplied;
    private boolean lockSingleTarget;
    private boolean stackingSlowness;
    private boolean chaosWallOnImpact;
    private boolean painFrostBloom;
    private boolean bountyFrostBloom;
    private int frostStringLevel;
    private int frostFrameLevel;
    private boolean spawnedFrostBloom;
    private boolean homingEnabled = true;
    private int chaosWallStringLevel;
    private int chaosWallFrameLevel;
    private boolean spawnedChaosWall;
    private UUID lockedTargetUuid;
    public HomingArrowEntity(EntityType<? extends HomingArrowEntity> type, World world) {
        super(type, world);
    }

    public HomingArrowEntity(World world, LivingEntity owner) {
        super(EntityRegistry.HOMING_ARROW.get(), world);
        this.setOwner(owner);
    }

    public HomingArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.HOMING_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("initialSpreadApplied", this.initialSpreadApplied);
        nbt.putBoolean("lockSingleTarget", this.lockSingleTarget);
        nbt.putBoolean("stackingSlowness", this.stackingSlowness);
        nbt.putBoolean("chaosWallOnImpact", this.chaosWallOnImpact);
        nbt.putBoolean("painFrostBloom", this.painFrostBloom);
        nbt.putBoolean("bountyFrostBloom", this.bountyFrostBloom);
        nbt.putInt("frostStringLevel", this.frostStringLevel);
        nbt.putInt("frostFrameLevel", this.frostFrameLevel);
        nbt.putBoolean("spawnedFrostBloom", this.spawnedFrostBloom);
        nbt.putBoolean("homingEnabled", this.homingEnabled);
        nbt.putInt("chaosWallStringLevel", this.chaosWallStringLevel);
        nbt.putInt("chaosWallFrameLevel", this.chaosWallFrameLevel);
        nbt.putBoolean("spawnedChaosWall", this.spawnedChaosWall);
        nbt.putInt("HomingFlightAge", this.age);
        if (this.lockedTargetUuid != null) nbt.putUuid("LockedTarget", this.lockedTargetUuid);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.initialSpreadApplied = nbt.getBoolean("initialSpreadApplied");
        this.lockSingleTarget = nbt.getBoolean("lockSingleTarget");
        this.stackingSlowness = nbt.getBoolean("stackingSlowness");
        this.chaosWallOnImpact = nbt.getBoolean("chaosWallOnImpact");
        this.painFrostBloom = nbt.getBoolean("painFrostBloom");
        this.bountyFrostBloom = nbt.getBoolean("bountyFrostBloom");
        this.frostStringLevel = nbt.getInt("frostStringLevel");
        this.frostFrameLevel = nbt.getInt("frostFrameLevel");
        this.spawnedFrostBloom = nbt.getBoolean("spawnedFrostBloom");
        this.homingEnabled = !nbt.contains("homingEnabled") || nbt.getBoolean("homingEnabled");
        this.chaosWallStringLevel = nbt.getInt("chaosWallStringLevel");
        this.chaosWallFrameLevel = nbt.getInt("chaosWallFrameLevel");
        this.spawnedChaosWall = nbt.getBoolean("spawnedChaosWall");
        this.age = Math.max(0, nbt.getInt("HomingFlightAge"));
        this.lockedTargetUuid = nbt.containsUuid("LockedTarget") ? nbt.getUuid("LockedTarget") : null;
        this.target = null; // Resolve a locked entity from its UUID on the next server tick.
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.homingEnabled) {
            this.setNoGravity(false);
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            spawnTrailParticles(serverWorld);
        }

        updateNoGravityState();

        if (!this.getWorld().isClient() && !this.initialSpreadApplied) {
            applyInitialSpread();
        }

        limitVelocityToCurrentMaxSpeed();

        // Keep model rotation aligned with velocity on both sides.
        Vec3d vel = this.getVelocity();
        if (vel.lengthSquared() > 1.0E-6) {
            float yaw = (float)(Math.atan2(vel.x, vel.z) * (180F / Math.PI));
            float pitch = (float)(Math.atan2(vel.y, vel.horizontalLength()) * (180F / Math.PI));
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
            this.setYaw(yaw);
            this.setPitch(pitch);
            this.velocityDirty = true;
        }

        if (!this.getWorld().isClient()) {
            if (this.lockSingleTarget && this.lockedTargetUuid != null && this.target == null && this.getWorld() instanceof ServerWorld serverWorld) {
                net.minecraft.entity.Entity e = serverWorld.getEntity(this.lockedTargetUuid);
                if (e instanceof LivingEntity living && living.isAlive()
                        && (!(this.getOwner() instanceof LivingEntity owner) || CombatTargeting.checkFriendlyFire(living, owner))) {
                    this.target = living;
                }
            }
            if (this.inGround) {
                this.target = null;
                return;
            }

            if (this.age < homingStartTicks()) {
                return;
            }

            // Locate a target if none exists
            if (target == null || !target.isAlive()) {
                if (this.lockSingleTarget) {
                    this.target = null;
                    return;
                }
                target = findNearestHostileMob();
            }

            // Adjust arrow trajectory toward the target
            if (target != null) {
                adjustTrajectoryTowardTarget();
            }
        }
    }

     // Finds the nearest hostile mob to the arrow within the homingRadius().
    private LivingEntity findNearestHostileMob() {
        Box searchBox = new Box(this.getX() - homingRadius(), this.getY() - homingRadius(), this.getZ() - homingRadius(),
                this.getX() + homingRadius(), this.getY() + homingRadius(), this.getZ() + homingRadius());
        LivingEntity owner = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;

        List<LivingEntity> entities = getEntityWorld().getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity)
                        && (owner == null || CombatTargeting.checkFriendlyFire(entity, owner)));

        if (!entities.isEmpty()) {
            // All fan arrows home the nearest hostile — avoid spreading onto empty air.
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity entity : entities) {
                double dist = this.squaredDistanceTo(entity);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entity;
                }
            }
            return best;
        }

        return null;
    }

     // Adjusts the arrow's velocity to steer toward the target.
    private void adjustTrajectoryTowardTarget() {
        Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getStandingEyeHeight(), target.getZ());
        Vec3d arrowPos = this.getPos();

        // Calculate the direction vector toward the target
        Vec3d direction = targetPos.subtract(arrowPos).normalize();

        // Smoothly adjust the velocity toward the target
        Vec3d newVelocity = this.getVelocity().add(direction.multiply(homingAccel()));
        double speedCap = getCurrentMaxSpeed();
        if (newVelocity.lengthSquared() > (speedCap * speedCap)) {
            newVelocity = newVelocity.normalize().multiply(speedCap);
        }

        this.setVelocity(newVelocity);

        // Update the pitch and yaw for proper rendering
        float yaw = (float)(Math.atan2(newVelocity.x, newVelocity.z) * (180F / Math.PI));
        float pitch = (float)(Math.atan2(newVelocity.y, newVelocity.horizontalLength()) * (180F / Math.PI));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.velocityDirty = true;
    }

    private double getCurrentMaxSpeed() {
        double ramp = Math.min(1.0, (double) this.age / speedRampTicks());
        return startSpeed() + (maxSpeed() - startSpeed()) * ramp;
    }

    private void applyInitialSpread() {
        this.initialSpreadApplied = true;
        if (this.age > 1) {
            return;
        }

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() <= 1.0E-6) {
            return;
        }

        float yawJitter = (this.random.nextFloat() * 2.0F - 1.0F) * initialSpreadYawRadians();
        float pitchJitter = (this.random.nextFloat() * 2.0F - 1.0F) * initialSpreadPitchRadians();
        Vec3d spreadVelocity = velocity.rotateY(yawJitter).rotateX(pitchJitter);
        this.setVelocity(spreadVelocity);
        this.velocityDirty = true;
    }

    private void updateNoGravityState() {
        this.setNoGravity(!this.inGround && this.age < homingStartTicks());
    }

    private void spawnTrailParticles(ServerWorld world) {
        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() <= 1.0E-6) {
            return;
        }

        world.spawnParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.03, 0.03, 0.03, 0.005);
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.04, 0.04, 0.04, 0.002);
        }
    }

    private void spawnImpactParticles(ServerWorld world, LivingEntity hitTarget) {
        double x = hitTarget.getX();
        double y = hitTarget.getBodyY(0.5);
        double z = hitTarget.getZ();
        world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 18, 0.25, 0.25, 0.25, 0.06);
        world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 10, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 6, 0.15, 0.15, 0.15, 0.01);
    }

    private void limitVelocityToCurrentMaxSpeed() {
        Vec3d velocity = this.getVelocity();
        double speedSq = velocity.lengthSquared();
        if (speedSq <= 1.0E-6) {
            return;
        }

        double speedCap = getCurrentMaxSpeed();
        if (speedSq > speedCap * speedCap) {
            this.setVelocity(velocity.normalize().multiply(speedCap));
            this.velocityDirty = true;
        }
    }

    @Override
    public boolean canHit(net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return super.canHit(entity);
        }
        if (this.getOwner() instanceof LivingEntity owner && !CombatTargeting.checkFriendlyFire(living, owner)) {
            return false;
        }
        return super.canHit(entity);
    }

    @Override
    protected void onHit(LivingEntity target) {
        if (isGraceSupportProjectile() && GraceProjectile.isSupportTarget(target)) {
            trySpawnGraceSanctuary(target.getPos());
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                spawnImpactParticles(serverWorld, target);
            }
            this.discard();
            return;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpactParticles(serverWorld, target);
        }
        super.onHit(target);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        trySpawnChaosWall(entityHitResult.getPos());
        trySpawnFrostBloom(entityHitResult.getPos());
        if (this.isRemoved()) {
            return;
        }
        super.onEntityHit(entityHitResult);
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        trySpawnChaosWall(blockHitResult.getPos());
        trySpawnFrostBloom(blockHitResult.getPos());
        if (this.isRemoved()) {
            return;
        }
        super.onBlockHit(blockHitResult);
    }

    private void trySpawnChaosWall(Vec3d pos) {
        if (this.spawnedChaosWall || !this.chaosWallOnImpact || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        IceChaosWallManager.spawnAtImpact(serverWorld, pos, this.getVelocity(),
                this.getOwner() != null ? this.getOwner().getUuid() : null, this.chaosWallStringLevel, this.chaosWallFrameLevel);
        this.spawnedChaosWall = true;
    }

    private void trySpawnFrostBloom(Vec3d pos) {
        if (this.spawnedFrostBloom || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        float base = (float) this.getDamage();
        if (this.painFrostBloom) {
            IceFrostBloomManager.spawnPainBloom(serverWorld, pos, owner, base, this.frostStringLevel);
            this.spawnedFrostBloom = true;
        } else if (this.bountyFrostBloom) {
            IceFrostBloomManager.spawnBountyBloom(serverWorld, pos, owner, base, this.frostStringLevel, this.frostFrameLevel);
            this.spawnedFrostBloom = true;
        } else if (this.stackingSlowness) {
            trySpawnGraceSanctuary(pos);
            this.discard();
        }
    }

    private void trySpawnGraceSanctuary(Vec3d pos) {
        if (this.spawnedFrostBloom || !this.stackingSlowness || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null) {
            return;
        }
        IceFrostBloomManager.spawnGraceSanctuary(serverWorld, pos, owner, this.frostStringLevel);
        this.spawnedFrostBloom = true;
        this.discard();
    }

    public void setPainFrostBloom(boolean painFrostBloom, int stringLevel) {
        this.painFrostBloom = painFrostBloom;
        this.frostStringLevel = stringLevel;
    }

    public void setBountyFrostBloom(boolean bountyFrostBloom, int stringLevel) {
        setBountyFrostBloom(bountyFrostBloom, stringLevel, 0);
    }

    public void setBountyFrostBloom(boolean bountyFrostBloom, int stringLevel, int frameLevel) {
        this.bountyFrostBloom = bountyFrostBloom;
        this.frostStringLevel = stringLevel;
        this.frostFrameLevel = Math.max(0, frameLevel);
    }

    LivingEntity getTargetEntity() {
        return this.target;
    }

    public void setLockSingleTarget(boolean lockSingleTarget) {
        this.lockSingleTarget = lockSingleTarget;
    }

    public void setLockedTargetUuid(UUID lockedTargetUuid) {
        this.lockedTargetUuid = lockedTargetUuid;
    }

    public boolean isGraceSupportProjectile() {
        return this.stackingSlowness;
    }

    public void setStackingSlowness(boolean stackingSlowness) {
        this.stackingSlowness = stackingSlowness;
    }

    public void setFrostStringLevel(int stringLevel) {
        this.frostStringLevel = Math.max(0, stringLevel);
    }

    public void setChaosWallOnImpact(boolean chaosWallOnImpact) {
        this.chaosWallOnImpact = chaosWallOnImpact;
    }

    public void setHomingEnabled(boolean homingEnabled) {
        this.homingEnabled = homingEnabled;
    }

    public void setChaosWallUpgradeLevels(int stringLevel, int frameLevel) {
        this.chaosWallStringLevel = Math.max(0, stringLevel);
        this.chaosWallFrameLevel = Math.max(0, frameLevel);
    }

    @Override
    public void setCritical(boolean critical) {
        // Winterfang never uses vanilla crit multiplier.
        super.setCritical(false);
    }

    @Override
    protected void onCollision(net.minecraft.util.hit.HitResult hitResult) {
        super.onCollision(hitResult);
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
