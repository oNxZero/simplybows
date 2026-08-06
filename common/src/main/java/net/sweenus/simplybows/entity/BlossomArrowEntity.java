package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
import net.sweenus.simplybows.world.BlossomChaosKoiManager;
import net.sweenus.simplybows.world.BlossomStormManager;

public class BlossomArrowEntity extends ArrowEntity {

    private boolean spawnedStorm;
    private final BowUpgradeData upgrades;

    public BlossomArrowEntity(EntityType<? extends BlossomArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public BlossomArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.BLOSSOM_ARROW.get(), world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.upgrades = BowUpgradeData.from(weaponStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            spawnTrailParticles(serverWorld);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, entityHitResult.getPos().x, entityHitResult.getPos().y, entityHitResult.getPos().z, SoundEvents.BLOCK_SPORE_BLOSSOM_PLACE, SoundCategory.PLAYERS, 0.85F, 1.0F + this.random.nextFloat() * 0.2F);
        }
        if (entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
            trySpawnStorm(entityHitResult.getPos(), livingEntity);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, blockHitResult.getPos().x, blockHitResult.getPos().y, blockHitResult.getPos().z, SoundEvents.BLOCK_SPORE_BLOSSOM_PLACE, SoundCategory.PLAYERS, 0.8F, 0.95F + this.random.nextFloat() * 0.2F);
        }
        trySpawnStorm(blockHitResult.getPos(), null);
    }

    private void spawnTrailParticles(ServerWorld world) {
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.03, 0.03, 0.03, 0.0);
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, this.getX(), this.getY() + 0.08, this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void trySpawnStorm(Vec3d pos, LivingEntity directTarget) {
        if (this.spawnedStorm) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (this.upgrades.runeEtching() == RuneEtching.CHAOS) {
                BlossomChaosKoiManager.createEffect(serverWorld, pos, directTarget, this.getOwner(), this.upgrades);
            } else {
                BlossomStormManager.createStorm(serverWorld, pos, directTarget, this.getOwner(), this.upgrades);
            }
            this.spawnedStorm = true;
        }
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
