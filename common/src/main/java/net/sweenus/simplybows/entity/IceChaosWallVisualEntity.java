package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class IceChaosWallVisualEntity extends Entity {

    private static final TrackedData<Float> TARGET_HEIGHT = DataTracker.registerData(IceChaosWallVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(IceChaosWallVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> DRIPSTONE_STYLE = DataTracker.registerData(IceChaosWallVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final TrackedData<Boolean> PRISON_STYLE = DataTracker.registerData(IceChaosWallVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> PRISON_WIDTH = DataTracker.registerData(IceChaosWallVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> PRISON_DEPTH = DataTracker.registerData(IceChaosWallVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private int prisonLifetime;
    private int prisonElapsed;
    private float previousHeightScale;
    public float getAnimatedHeightScale(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(tickDelta, previousHeightScale, getHeightScale());
    }

    public void setPrisonStyle(float width, float depth) {
        dataTracker.set(PRISON_STYLE, true);
        dataTracker.set(PRISON_WIDTH, width);
        dataTracker.set(PRISON_DEPTH, depth);
    }
    public boolean isPrisonStyle() { return dataTracker.get(PRISON_STYLE); }
    public float getPrisonWidth() { return dataTracker.get(PRISON_WIDTH); }
    public float getPrisonDepth() { return dataTracker.get(PRISON_DEPTH); }
    public void setPrisonLifetime(int ticks) { prisonLifetime = ticks; }
    @Override public void tick() {
        super.tick();
        previousHeightScale = getHeightScale();
        if (!getWorld().isClient() && isPrisonStyle()) {
            prisonElapsed++;
            prisonLifetime--;
            float grow = Math.min(1.0F, prisonElapsed / 6.0F);
            float shrink = Math.min(1.0F, Math.max(0, prisonLifetime) / 8.0F);
            float scale = grow * shrink;
            setHeightScale(scale * scale * (3 - 2 * scale));
            if (prisonLifetime <= 0) {
                if (getWorld() instanceof net.minecraft.server.world.ServerWorld world) {
                    world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(
                            net.minecraft.particle.ParticleTypes.BLOCK, net.minecraft.block.Blocks.ICE.getDefaultState()),
                            getX(), getY()+getTargetHeight()*0.5, getZ(), 35,
                            getPrisonWidth()*0.5, getTargetHeight()*0.4, getPrisonDepth()*0.5, 0.12);
                    world.playSound(null, getX(), getY(), getZ(), net.minecraft.sound.SoundEvents.BLOCK_GLASS_BREAK,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.7F, 1.2F);
                }
                discard();
            }
        }
    }

    public IceChaosWallVisualEntity(EntityType<? extends IceChaosWallVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public IceChaosWallVisualEntity(World world, double x, double y, double z, float targetHeight) {
        this(EntityRegistry.ICE_CHAOS_WALL_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setTargetHeight(targetHeight);
        this.setHeightScale(0.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(TARGET_HEIGHT, 1.0F);
        builder.add(HEIGHT_SCALE, 0.0F);
        builder.add(DRIPSTONE_STYLE, false);
        builder.add(PRISON_STYLE, false);
        builder.add(PRISON_WIDTH, 1.0F);
        builder.add(PRISON_DEPTH, 1.0F);
    }

    public void setDripstoneStyle(boolean dripstoneStyle) {
        this.dataTracker.set(DRIPSTONE_STYLE, dripstoneStyle);
    }

    public boolean isDripstoneStyle() {
        return this.dataTracker.get(DRIPSTONE_STYLE);
    }

    public void setTargetHeight(float targetHeight) {
        this.dataTracker.set(TARGET_HEIGHT, targetHeight);
    }

    public float getTargetHeight() {
        return this.dataTracker.get(TARGET_HEIGHT);
    }

    public void setHeightScale(float heightScale) {
        this.dataTracker.set(HEIGHT_SCALE, heightScale);
    }

    public float getHeightScale() {
        return this.dataTracker.get(HEIGHT_SCALE);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.getBoolean("prison_style")) setPrisonStyle(nbt.getFloat("prison_width"), nbt.getFloat("prison_depth"));
        prisonLifetime = nbt.getInt("prison_lifetime");
        prisonElapsed = nbt.getInt("prison_elapsed");
        previousHeightScale = nbt.getFloat("height_scale");
        if (nbt.contains("target_height")) {
            this.setTargetHeight(nbt.getFloat("target_height"));
        }
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
        if (nbt.contains("dripstone_style")) {
            this.setDripstoneStyle(nbt.getBoolean("dripstone_style"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("prison_style", isPrisonStyle());
        nbt.putFloat("prison_width", getPrisonWidth());
        nbt.putFloat("prison_depth", getPrisonDepth());
        nbt.putInt("prison_lifetime", prisonLifetime);
        nbt.putInt("prison_elapsed", prisonElapsed);
        nbt.putFloat("target_height", this.getTargetHeight());
        nbt.putFloat("height_scale", this.getHeightScale());
        nbt.putBoolean("dripstone_style", this.isDripstoneStyle());
    }
}
