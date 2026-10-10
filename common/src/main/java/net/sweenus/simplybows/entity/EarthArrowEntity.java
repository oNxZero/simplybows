package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
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
import net.sweenus.simplybows.util.GraceProjectile;
import net.sweenus.simplybows.world.EarthSpikeFieldManager;

public class EarthArrowEntity extends ArrowEntity {

    private static final String EARTH_VISUAL_TAG = "simplybows_earth_spike_visual";
    private BowUpgradeData upgrades;
    private boolean spawnedField;
    private boolean chaosSunderOnImpact;

    public EarthArrowEntity(EntityType<? extends EarthArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public EarthArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.EARTH_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.upgrades = BowUpgradeData.from(weaponStack);
        net.sweenus.simplybows.util.BowProjectileEnchantments.initialize(this, weaponStack, arrowStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()), this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity living
                && isGraceSupportProjectile()
                && GraceProjectile.isSupportTarget(living)) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                Vec3d pos = living.getPos();
                serverWorld.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.85F, 1.1F);
            }
            trySpawnField(living.getPos(), living);
            this.discard();
            return;
        }
        LivingEntity struck = entityHitResult.getEntity() instanceof LivingEntity living ? living : null;
        if (struck != null) {
            struck.hurtTime = 0;
            struck.timeUntilRegen = 0;
        }
        // Preserve the impact direction and create the effect even on lethal hits.
        trySpawnField(entityHitResult.getPos(), struck);
        super.onEntityHit(entityHitResult);
        if (struck != null) {
            struck.hurtTime = 0;
            struck.timeUntilRegen = 0;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = entityHitResult.getPos();
            serverWorld.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
            serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()), pos.x, pos.y + 0.1, pos.z, 12, 0.3, 0.1, 0.3, 0.01);
        }
    }

    public boolean isGraceSupportProjectile() {
        return this.upgrades.runeEtching() == RuneEtching.GRACE;
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = blockHitResult.getPos();
            serverWorld.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
            serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()), pos.x, pos.y + 0.1, pos.z, 12, 0.3, 0.1, 0.3, 0.01);
        }
        trySpawnField(blockHitResult.getPos(), null);
    }

    @Override
    public boolean canHit(Entity entity) {
        if (entity.getCommandTags().contains(EARTH_VISUAL_TAG)) {
            return false;
        }
        return super.canHit(entity);
    }

    private void trySpawnField(Vec3d pos, LivingEntity hitTarget) {
        if (this.spawnedField) {
            return;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Chaos replaces the spike field entirely — never fall back to the normal ability on CD.
            if (this.upgrades.runeEtching() == RuneEtching.CHAOS) {
                if (this.chaosSunderOnImpact) {
                    net.sweenus.simplybows.world.RuneEffectManager.cast(serverWorld,
                            net.sweenus.simplybows.util.RuneEffectRules.STONE, pos, getOwner(), hitTarget, upgrades, false);
                }
            } else {
                EarthSpikeFieldManager.createOrReplaceField(
                        serverWorld, pos, this.getOwner(), this.upgrades, this.getVelocity(), hitTarget);
            }
            this.spawnedField = true;
        }
    }

    public void setChaosSunderOnImpact(boolean chaosSunderOnImpact) {
        this.chaosSunderOnImpact = chaosSunderOnImpact;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("BowUpgrades", this.upgrades.toProjectileNbt());
        nbt.putBoolean("SpawnedField", this.spawnedField);
        nbt.putBoolean("ChaosSunderOnImpact", this.chaosSunderOnImpact);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.upgrades = BowUpgradeData.fromProjectileNbt(nbt.getCompound("BowUpgrades"));
        this.spawnedField = nbt.getBoolean("SpawnedField");
        this.chaosSunderOnImpact = nbt.getBoolean("ChaosSunderOnImpact");
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
