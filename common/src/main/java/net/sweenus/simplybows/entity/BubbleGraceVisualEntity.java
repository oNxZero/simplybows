package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class BubbleGraceVisualEntity extends Entity {

    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(BubbleGraceVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(BubbleGraceVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> COLUMN_HEIGHT = DataTracker.registerData(BubbleGraceVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public BubbleGraceVisualEntity(EntityType<? extends BubbleGraceVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BubbleGraceVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.BUBBLE_GRACE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setHeightScale(1.0F);
        this.setRadius(1.2F);
        this.setColumnHeight(2.6F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(HEIGHT_SCALE, 1.0F);
        builder.add(RADIUS, 1.2F);
        builder.add(COLUMN_HEIGHT, 2.6F);
    }

    public void setHeightScale(float value) {
        this.dataTracker.set(HEIGHT_SCALE, value);
    }

    public float getHeightScale() {
        return this.dataTracker.get(HEIGHT_SCALE);
    }

    public void setRadius(float value) {
        this.dataTracker.set(RADIUS, value);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setColumnHeight(float value) {
        this.dataTracker.set(COLUMN_HEIGHT, value);
    }

    public float getColumnHeight() {
        return this.dataTracker.get(COLUMN_HEIGHT);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
        if (nbt.contains("radius")) {
            this.setRadius(nbt.getFloat("radius"));
        }
        if (nbt.contains("column_height")) {
            this.setColumnHeight(nbt.getFloat("column_height"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("height_scale", this.getHeightScale());
        nbt.putFloat("radius", this.getRadius());
        nbt.putFloat("column_height", this.getColumnHeight());
    }
    @Override public void tick() {
        super.tick();
        if(getWorld().isClient && age%3==0) {
            double fade=Math.max(0,Math.min(1,getHeightScale()));
            for(int i=0;i<20;i++) {
                double angle=i*Math.PI/10+age*.025;
                double ring=getRadius()*fade*(i%2==0 ? 1 : .65);
                getWorld().addParticle(net.minecraft.particle.ParticleTypes.BUBBLE_POP,
                    getX()+Math.cos(angle)*ring,getY()+.08,getZ()+Math.sin(angle)*ring,0,.025,0);
                if(i%4==0) getWorld().addParticle(net.minecraft.particle.ParticleTypes.SPLASH,
                    getX()+Math.cos(angle)*ring,getY()+.12,getZ()+Math.sin(angle)*ring,0,.03,0);
            }
        }
    }

}
