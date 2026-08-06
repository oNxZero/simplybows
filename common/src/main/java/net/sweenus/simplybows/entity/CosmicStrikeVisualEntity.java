package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class CosmicStrikeVisualEntity extends Entity {

    private static final TrackedData<Float> END_X =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Y =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Z =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME_TICKS =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> POINT_COUNT =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> PASSIVE_MODE =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PASSIVE_OWNER_ID =
            DataTracker.registerData(CosmicStrikeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int PASSIVE_REVEAL_INTERVAL_TICKS = 8;

    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(12.0F, 12.0F);

    public CosmicStrikeVisualEntity(EntityType<? extends CosmicStrikeVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public CosmicStrikeVisualEntity(World world, Vec3d start, Vec3d end) {
        this(world, start, end, 7);
    }

    public CosmicStrikeVisualEntity(World world, Vec3d start, Vec3d end, int lifetimeTicks) {
        this(EntityRegistry.COSMIC_STRIKE_VISUAL.get(), world);
        this.setPosition(start.x, start.y, start.z);
        this.setEndPos(end);
        this.setLifetimeTicks(lifetimeTicks);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(END_X, 0.0F);
        this.dataTracker.startTracking(END_Y, 0.0F);
        this.dataTracker.startTracking(END_Z, 0.0F);
        this.dataTracker.startTracking(LIFETIME_TICKS, 7);
        this.dataTracker.startTracking(POINT_COUNT, 0);
        this.dataTracker.startTracking(PASSIVE_MODE, false);
        this.dataTracker.startTracking(PASSIVE_OWNER_ID, -1);
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

    public int getLifetimeTicks() {
        return Math.max(1, this.dataTracker.get(LIFETIME_TICKS));
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.dataTracker.set(LIFETIME_TICKS, Math.max(1, lifetimeTicks));
    }

    public int getPointCount() {
        return this.dataTracker.get(POINT_COUNT);
    }

    public void setPointCount(int pointCount) {
        this.dataTracker.set(POINT_COUNT, Math.max(0, pointCount));
    }

    public boolean isPassiveMode() {
        return this.dataTracker.get(PASSIVE_MODE);
    }

    public void setPassiveMode(boolean passiveMode) {
        this.dataTracker.set(PASSIVE_MODE, passiveMode);
    }

    public int getPassiveOwnerId() {
        return this.dataTracker.get(PASSIVE_OWNER_ID);
    }

    public void setPassiveOwnerId(int passiveOwnerId) {
        this.dataTracker.set(PASSIVE_OWNER_ID, passiveOwnerId);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isPassiveMode() && this.age <= this.getPassiveRevealTicks()) {
            Vec3d playerPos = this.getPassiveOwnerCenterPos();
            if (playerPos != null) {
                this.setPosition(playerPos.x, playerPos.y, playerPos.z);
                this.setEndPos(playerPos);
            }
        }
        if (!this.getWorld().isClient && this.age > this.getLifetimeTicks()) {
            this.discard();
        }
    }

    private int getPassiveRevealTicks() {
        return Math.max(1, this.getPointCount()) * PASSIVE_REVEAL_INTERVAL_TICKS;
    }

    private Vec3d getPassiveOwnerCenterPos() {
        Entity owner = this.getWorld().getEntityById(this.getPassiveOwnerId());
        if (!(owner instanceof PlayerEntity player)) {
            return null;
        }
        return player.getBoundingBox().getCenter();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setEndPos(new Vec3d(nbt.getFloat("end_x"), nbt.getFloat("end_y"), nbt.getFloat("end_z")));
        this.setLifetimeTicks(nbt.getInt("lifetime_ticks"));
        this.setPointCount(nbt.getInt("point_count"));
        this.setPassiveMode(nbt.getBoolean("passive_mode"));
        this.setPassiveOwnerId(nbt.getInt("passive_owner_id"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        Vec3d end = this.getEndPos();
        nbt.putFloat("end_x", (float) end.x);
        nbt.putFloat("end_y", (float) end.y);
        nbt.putFloat("end_z", (float) end.z);
        nbt.putInt("lifetime_ticks", this.getLifetimeTicks());
        nbt.putInt("point_count", this.getPointCount());
        nbt.putBoolean("passive_mode", this.isPassiveMode());
        nbt.putInt("passive_owner_id", this.getPassiveOwnerId());
    }
}
