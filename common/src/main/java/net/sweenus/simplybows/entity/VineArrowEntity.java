package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.world.VineFlowerFieldManager;

public class VineArrowEntity extends ArrowEntity {

    private static double extraDragXZ() { return SimplyBowsConfig.INSTANCE.everbloom.extraDragXZ.get(); }
    private static double extraDragY() { return SimplyBowsConfig.INSTANCE.everbloom.extraDragY.get(); }
    private static final String FIELD_VISUAL_TAG = "simplybows_vine_field_visual";
    private final BowUpgradeData upgrades;
    private boolean spawnedFlowerField;
    private boolean plantsFlowerField;

    public VineArrowEntity(EntityType<? extends VineArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public VineArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.VINE_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
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

        if (!this.inGround) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.x * extraDragXZ(), velocity.y * extraDragY(), velocity.z * extraDragXZ());
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpactParticles(serverWorld, blockHitResult.getPos());
        }
        trySpawnFlowerField(blockHitResult.getPos());
        super.onBlockHit(blockHitResult);
    }

    @Override
    protected ProjectileDeflection hitOrDeflect(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity hit = ((EntityHitResult) hitResult).getEntity();
            if (hit instanceof PlayerEntity || hit instanceof IronGolemEntity || hit instanceof AnimalEntity) {
                this.setDamage(0.0);
                this.setCritical(false);
            }
        }
        return super.hitOrDeflect(hitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity hit = entityHitResult.getEntity();
        boolean playerHit = hit instanceof PlayerEntity;
        boolean golemHit = hit instanceof IronGolemEntity;
        boolean animalHit = hit instanceof AnimalEntity;
        if (!playerHit && !golemHit && !animalHit && hit instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpactParticles(serverWorld, entityHitResult.getPos());
        }
        trySpawnFlowerField(entityHitResult.getPos());
        if (playerHit && hit instanceof PlayerEntity player && !player.isSpectator()) {
            healDirectHit(player);
            return;
        }
        if (golemHit && hit instanceof IronGolemEntity golem) {
            healDirectHit(golem);
            return;
        }
        if (animalHit && hit instanceof AnimalEntity animal) {
            healDirectHit(animal);
            return;
        }
        super.onEntityHit(entityHitResult);
        if (hit instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
    }

    @Override
    public boolean canHit(Entity entity) {
        if (entity.getCommandTags().contains(FIELD_VISUAL_TAG)) {
            return false;
        }
        return super.canHit(entity);
    }

    public void setPlantsFlowerField(boolean plantsFlowerField) {
        this.plantsFlowerField = plantsFlowerField;
    }

    private void healDirectHit(LivingEntity living) {
        this.setDamage(0.0);
        this.setCritical(false);
        float before = living.getHealth();
        living.heal(2.0F + this.upgrades.frameLevel());
        if (living.getHealth() > before && this.getWorld() instanceof ServerWorld healedWorld) {
            VineFlowerFieldManager.spawnHealIntakeParticles(healedWorld, living);
        }
        this.discard();
    }

    private void trySpawnFlowerField(Vec3d hitPos) {
        if (!this.plantsFlowerField || this.spawnedFlowerField) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            VineFlowerFieldManager.createOrReplaceField(serverWorld, hitPos, this.getOwner(), this.upgrades);
            this.spawnedFlowerField = true;
        }
    }

    private void spawnTrailParticles(ServerWorld world) {
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.05, 0.03, 0.05, 0.0);
        if (this.age % 3 == 0) {
            world.spawnParticles(ParticleTypes.COMPOSTER, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.04, 0.02, 0.04, 0.0);
        }
    }

    private void spawnImpactParticles(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, pos.x, pos.y + 0.2, pos.z, 12, 0.35, 0.15, 0.35, 0.0);
        world.spawnParticles(ParticleTypes.COMPOSTER, pos.x, pos.y + 0.12, pos.z, 10, 0.3, 0.12, 0.3, 0.0);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.SHORT_GRASS.getDefaultState()), pos.x, pos.y + 0.1, pos.z, 8, 0.28, 0.08, 0.28, 0.01);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("PlantsFlowerField", this.plantsFlowerField);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.plantsFlowerField = nbt.getBoolean("PlantsFlowerField");
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
