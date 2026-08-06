package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class CosmicBountyVisualEntity extends Entity {

    private static final TrackedData<Integer> CHARGE_TICKS =
            DataTracker.registerData(CosmicBountyVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> IMPLODE_TICKS =
            DataTracker.registerData(CosmicBountyVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MAX_IMPLODE_TICKS =
            DataTracker.registerData(CosmicBountyVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> EXPLODED =
            DataTracker.registerData(CosmicBountyVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(8.0F, 8.0F);

    public CosmicBountyVisualEntity(EntityType<? extends CosmicBountyVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public CosmicBountyVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.COSMIC_BOUNTY_VISUAL.get(), world);
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(CHARGE_TICKS, 0);
        this.dataTracker.startTracking(IMPLODE_TICKS, 0);
        this.dataTracker.startTracking(MAX_IMPLODE_TICKS, 40);
        this.dataTracker.startTracking(EXPLODED, false);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return DIMENSIONS;
    }

    public int getChargeTicks() {
        return this.dataTracker.get(CHARGE_TICKS);
    }

    public void setChargeTicks(int chargeTicks) {
        this.dataTracker.set(CHARGE_TICKS, Math.max(0, chargeTicks));
    }

    public int getImplodeTicks() {
        return this.dataTracker.get(IMPLODE_TICKS);
    }

    public void setImplodeTicks(int implodeTicks) {
        this.dataTracker.set(IMPLODE_TICKS, Math.max(0, implodeTicks));
    }

    public int getMaxImplodeTicks() {
        return this.dataTracker.get(MAX_IMPLODE_TICKS);
    }

    public void setMaxImplodeTicks(int maxImplodeTicks) {
        this.dataTracker.set(MAX_IMPLODE_TICKS, Math.max(1, maxImplodeTicks));
    }

    public boolean hasExploded() {
        return this.dataTracker.get(EXPLODED);
    }

    public void setExploded(boolean exploded) {
        this.dataTracker.set(EXPLODED, exploded);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setChargeTicks(nbt.getInt("charge_ticks"));
        this.setImplodeTicks(nbt.getInt("implode_ticks"));
        this.setMaxImplodeTicks(nbt.getInt("max_implode_ticks"));
        this.setExploded(nbt.getBoolean("exploded"));
        this.discard();
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("charge_ticks", this.getChargeTicks());
        nbt.putInt("implode_ticks", this.getImplodeTicks());
        nbt.putInt("max_implode_ticks", this.getMaxImplodeTicks());
        nbt.putBoolean("exploded", this.hasExploded());
    }
}
