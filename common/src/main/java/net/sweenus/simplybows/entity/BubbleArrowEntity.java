package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.GraceProjectile;
import net.sweenus.simplybows.world.BubbleColumnFieldManager;

import java.util.UUID;

public class BubbleArrowEntity extends ArrowEntity {

    private static final double WATER_DRAG_COMPENSATION = 1.0 / 0.6;
    private boolean spawnedBubbleColumn;
    private UUID columnOwnerId;
    private BowUpgradeData columnUpgrades = BowUpgradeData.none();

    public BubbleArrowEntity(EntityType<? extends BubbleArrowEntity> type, World world) {
        super(type, world);
    }

    public BubbleArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.BUBBLE_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.columnOwnerId = owner != null ? owner.getUuid() : null;
        this.columnUpgrades = BowUpgradeData.from(weaponStack);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isTouchingWater() && !this.inGround) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.multiply(WATER_DRAG_COMPENSATION));
            this.velocityDirty = true;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            spawnTrailParticles(serverWorld);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpactParticles(serverWorld, blockHitResult.getPos());
        }
        trySpawnBubbleColumn(blockHitResult.getPos());
        super.onBlockHit(blockHitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity living
                && isGraceSupportProjectile()
                && GraceProjectile.isSupportTarget(living)) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                spawnImpactParticles(serverWorld, living.getPos());
            }
            trySpawnBubbleColumn(living.getPos());
            this.discard();
            return;
        }
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpactParticles(serverWorld, entityHitResult.getPos());
        }
        trySpawnBubbleColumn(entityHitResult.getPos());
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
    }

    public boolean isGraceSupportProjectile() {
        return this.columnUpgrades.runeEtching() == RuneEtching.GRACE;
    }

    private void trySpawnBubbleColumn(Vec3d hitPos) {
        if (this.spawnedBubbleColumn) {
            return;
        }
        // Chaos is the wave only — never fall back to a bubble column while Chaos is etched.
        if (this.columnUpgrades.runeEtching() == RuneEtching.CHAOS) {
            this.spawnedBubbleColumn = true;
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            this.spawnedBubbleColumn = BubbleColumnFieldManager.createOrReplaceColumn(serverWorld, hitPos, this.columnOwnerId, this.columnUpgrades);
        }
    }

    private void spawnTrailParticles(ServerWorld world) {
        world.spawnParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.05, 0.04, 0.05, 0.0);
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnImpactParticles(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.BUBBLE, pos.x, pos.y + 0.2, pos.z, 14, 0.35, 0.2, 0.35, 0.0);
        world.spawnParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.1, pos.z, 10, 0.25, 0.12, 0.25, 0.0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("BowUpgrades", this.columnUpgrades.toProjectileNbt());
        nbt.putBoolean("AbilitySpawned", this.spawnedBubbleColumn);
        if (this.columnOwnerId != null) nbt.putUuid("ColumnOwner", this.columnOwnerId);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.columnUpgrades = BowUpgradeData.fromProjectileNbt(nbt.getCompound("BowUpgrades"));
        this.spawnedBubbleColumn = nbt.getBoolean("AbilitySpawned");
        this.columnOwnerId = nbt.containsUuid("ColumnOwner") ? nbt.getUuid("ColumnOwner") : null;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
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
