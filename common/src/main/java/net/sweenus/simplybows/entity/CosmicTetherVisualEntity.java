package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class CosmicTetherVisualEntity extends Entity {

    private static final TrackedData<Float> END_X =
            DataTracker.registerData(CosmicTetherVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Y =
            DataTracker.registerData(CosmicTetherVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Z =
            DataTracker.registerData(CosmicTetherVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> COCOON_MODE =
            DataTracker.registerData(CosmicTetherVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(12.0F, 12.0F);

    public CosmicTetherVisualEntity(EntityType<? extends CosmicTetherVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public CosmicTetherVisualEntity(World world, Vec3d start, Vec3d end) {
        this(EntityRegistry.COSMIC_TETHER_VISUAL.get(), world);
        this.setPosition(start.x, start.y, start.z);
        this.setEndPos(end);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(END_X, 0.0F);
        this.dataTracker.startTracking(END_Y, 0.0F);
        this.dataTracker.startTracking(END_Z, 0.0F);
        this.dataTracker.startTracking(COCOON_MODE, false);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return DIMENSIONS;
    }

    public Vec3d getEndPos() {
        return new Vec3d(this.dataTracker.get(END_X), this.dataTracker.get(END_Y), this.dataTracker.get(END_Z));
    }

    public void setEndPos(Vec3d pos) {
        this.dataTracker.set(END_X, (float) pos.x);
        this.dataTracker.set(END_Y, (float) pos.y);
        this.dataTracker.set(END_Z, (float) pos.z);
    }

    public boolean isCocoonMode() {
        return this.dataTracker.get(COCOON_MODE);
    }

    public void setCocoonMode(boolean cocoonMode) {
        this.dataTracker.set(COCOON_MODE, cocoonMode);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setEndPos(new Vec3d(nbt.getFloat("end_x"), nbt.getFloat("end_y"), nbt.getFloat("end_z")));
        this.setCocoonMode(nbt.getBoolean("cocoon_mode"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        Vec3d end = this.getEndPos();
        nbt.putFloat("end_x", (float) end.x);
        nbt.putFloat("end_y", (float) end.y);
        nbt.putFloat("end_z", (float) end.z);
        nbt.putBoolean("cocoon_mode", this.isCocoonMode());
    }
}
