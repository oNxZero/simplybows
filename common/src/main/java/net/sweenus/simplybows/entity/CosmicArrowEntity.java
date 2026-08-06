package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.CosmicChaosSunManager;
import net.sweenus.simplybows.world.CosmicGraceTrailManager;
import net.sweenus.simplybows.world.CosmicOrbitManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CosmicArrowEntity extends ArrowEntity {

    private static final TrackedData<Boolean> GRACE_MODE =
            DataTracker.registerData(CosmicArrowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BOUNTY_MODE =
            DataTracker.registerData(CosmicArrowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> BOUNTY_CHARGE_TICKS =
            DataTracker.registerData(CosmicArrowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final BowUpgradeData upgrades;
    @Nullable
    private final UUID ownerIdSnapshot;

    public CosmicArrowEntity(EntityType<? extends CosmicArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
        this.ownerIdSnapshot = null;
    }

    public CosmicArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.COSMIC_ARROW.get(), world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.upgrades = BowUpgradeData.from(weaponStack);
        this.ownerIdSnapshot = owner.getUuid();
        this.setGraceMode(this.upgrades.runeEtching() == RuneEtching.GRACE);
        this.setBountyMode(this.upgrades.runeEtching() == RuneEtching.BOUNTY);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(GRACE_MODE, false);
        this.dataTracker.startTracking(BOUNTY_MODE, false);
        this.dataTracker.startTracking(BOUNTY_CHARGE_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isBountyMode() && !this.isOnGround() && !this.isRemoved()) {
            this.setBountyChargeTicks(this.getBountyChargeTicks() + 1);
            if (!this.getWorld().isClient) {
                Vec3d velocity = this.getVelocity();
                double horizontalDrag = this.age <= 4 ? 0.92 : 0.972;
                double verticalBoost = this.age <= 4 ? 0.055 : -0.0015;
                this.setVelocity(velocity.x * horizontalDrag, velocity.y + verticalBoost, velocity.z * horizontalDrag);
            }
        }
    }

    public boolean isGraceMode() {
        return this.dataTracker.get(GRACE_MODE);
    }

    private void setGraceMode(boolean graceMode) {
        this.dataTracker.set(GRACE_MODE, graceMode);
    }

    public boolean isBountyMode() {
        return this.dataTracker.get(BOUNTY_MODE);
    }

    private void setBountyMode(boolean bountyMode) {
        this.dataTracker.set(BOUNTY_MODE, bountyMode);
    }

    public int getBountyChargeTicks() {
        return this.dataTracker.get(BOUNTY_CHARGE_TICKS);
    }

    public BowUpgradeData getUpgrades() {
        return this.upgrades;
    }

    private void setBountyChargeTicks(int chargeTicks) {
        this.dataTracker.set(BOUNTY_CHARGE_TICKS, Math.max(0, chargeTicks));
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = entityHitResult.getPos();
            serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, 10, 0.15, 0.15, 0.15, 0.0);
            playImpactSound(serverWorld, pos);
            if (this.isBountyMode()) {
                net.sweenus.simplybows.world.CosmicBountyManager.createImplosion(serverWorld, this.getOwner(), pos, this.getBountyChargeTicks(), this.upgrades);
                this.discard();
                return;
            }
            if (this.upgrades.runeEtching() == RuneEtching.CHAOS) {
                CosmicChaosSunManager.createSun(serverWorld, this.getOwner(), this.ownerIdSnapshot, pos, this.upgrades);
                this.discard();
                return;
            }
            if (this.isGraceMode()) {
                CosmicGraceTrailManager.createField(serverWorld, this.getOwner(), pos, this.upgrades);
            }
            if (entityHitResult.getEntity() instanceof LivingEntity target) {
                CosmicOrbitManager.createOrRefresh(serverWorld, target, this.getOwner(), this.upgrades);
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = blockHitResult.getPos();
            serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, 8, 0.12, 0.12, 0.12, 0.0);
            playImpactSound(serverWorld, pos);
            if (this.isBountyMode()) {
                net.sweenus.simplybows.world.CosmicBountyManager.createImplosion(serverWorld, this.getOwner(), pos, this.getBountyChargeTicks(), this.upgrades);
                this.discard();
                return;
            }
            if (this.upgrades.runeEtching() == RuneEtching.CHAOS) {
                CosmicChaosSunManager.createSun(serverWorld, this.getOwner(), this.ownerIdSnapshot, pos, this.upgrades);
                this.discard();
                return;
            }
            if (this.isGraceMode()) {
                CosmicGraceTrailManager.createField(serverWorld, this.getOwner(), pos, this.upgrades);
            }
        }
    }

    private void playImpactSound(ServerWorld world, Vec3d pos) {
        float chimePitch = 0.95F + this.random.nextFloat() * 0.18F;
        float anchorPitch = 1.65F + this.random.nextFloat() * 0.16F;
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.75F, chimePitch);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.28F, anchorPitch);
    }

    @Override
    public ItemStack asItemStack() {
        return new ItemStack(Items.ARROW);
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
