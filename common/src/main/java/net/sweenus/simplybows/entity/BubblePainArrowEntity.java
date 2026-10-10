package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;

import java.util.UUID;

public class BubblePainArrowEntity extends net.minecraft.entity.projectile.ArrowEntity {

    private static final double WATER_DRAG_COMPENSATION = 1.0 / 0.6;
    private boolean spawnedBubbleColumn;
    private boolean firedSoundPlayed;
    private UUID columnOwnerId;
    private BowUpgradeData columnUpgrades = BowUpgradeData.none();

    public BubblePainArrowEntity(EntityType<? extends BubblePainArrowEntity> type, World world) {
        super(type, world);
    }

    public BubblePainArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        this(EntityRegistry.BUBBLE_PAIN_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.columnOwnerId = owner != null ? owner.getUuid() : null;
        this.columnUpgrades = BowUpgradeData.from(weaponStack);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        net.sweenus.simplybows.util.BowProjectileEnchantments.initialize(this, weaponStack, arrowStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!this.firedSoundPlayed) {
            this.firedSoundPlayed = true;
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.PLAYERS, 0.65F, 1.0F + this.random.nextFloat() * 0.15F);
        }

        if (!this.inGround) {
            if (this.isTouchingWater()) {
                Vec3d velocity = this.getVelocity();
                this.setVelocity(velocity.multiply(WATER_DRAG_COMPENSATION));
                this.velocityDirty = true;
            }
            serverWorld.spawnParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.05, 0.04, 0.05, 0.0);
            if (this.age % 2 == 0) {
                serverWorld.spawnParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpact(serverWorld, blockHitResult.getPos());
        }
        // Ground impacts also create the area raincloud.
        this.spawnedBubbleColumn = true;
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnPoofAndDiscard(serverWorld);
        } else {
            this.discard();
        }
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);

    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt); nbt.put("BowUpgrades", columnUpgrades.toProjectileNbt());
        nbt.putBoolean("ImpactUsed", spawnedBubbleColumn); nbt.putBoolean("FiredSound", firedSoundPlayed);
    }
    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt); columnUpgrades = BowUpgradeData.fromProjectileNbt(nbt.getCompound("BowUpgrades"));
        spawnedBubbleColumn = nbt.getBoolean("ImpactUsed"); firedSoundPlayed = nbt.getBoolean("FiredSound");
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpact(serverWorld, entityHitResult.getPos());
        }
        this.spawnedBubbleColumn = true;
        super.onEntityHit(entityHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnPoofAndDiscard(serverWorld);
        } else {
            this.discard();
        }
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected SoundEvent getHitSound() {
        return SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP;
    }

    private void spawnImpact(ServerWorld world, Vec3d pos) {
        if (!spawnedBubbleColumn) net.sweenus.simplybows.world.RuneEffectManager.cast(world,
                net.sweenus.simplybows.util.RuneEffectRules.VORTEX,pos,getOwner(),null,columnUpgrades,false);
        spawnedBubbleColumn = true;
        world.spawnParticles(ParticleTypes.BUBBLE, pos.x, pos.y + 0.2, pos.z, 14, 0.35, 0.2, 0.35, 0.0);
        world.spawnParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.1, pos.z, 10, 0.25, 0.12, 0.25, 0.0);
    }

    private void spawnPoofAndDiscard(ServerWorld world) {
        world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.08, this.getZ(), 12, 0.14, 0.1, 0.14, 0.02);
        world.spawnParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.08, this.getZ(), 6, 0.1, 0.08, 0.1, 0.0);
        this.discard();
    }

    private static ItemStack sanitizeArrowStack(ItemStack arrowStack) {
        if (arrowStack == null || arrowStack.isEmpty()) {
            return Items.ARROW.getDefaultStack();
        }
        ItemStack copy = arrowStack.copy();
        copy.setCount(1);
        return copy;
    }
}
