package net.sweenus.simplybows.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class LongFireworkParticle extends SpriteBillboardParticle {

    private static final int FADE_OUT_TICKS = 16;
    private static final int GRAVITY_DELAY_TICKS = 16;
    private static final int GRAVITY_RAMP_TICKS = 64;
    private static final double MAX_FALL_ACCELERATION = 0.0075;
    private static final double MAX_FALL_SPEED = -0.38;

    protected LongFireworkParticle(ClientWorld world, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.setSpriteForAge(sprites);
        this.maxAge = 152 + world.random.nextInt(44);
        this.scale = 0.12F + world.random.nextFloat() * 0.08F;
        this.velocityX = vx + (world.random.nextDouble() - 0.5) * 0.006;
        this.velocityY = vy;
        this.velocityZ = vz + (world.random.nextDouble() - 0.5) * 0.006;
        this.red = 0.62F + world.random.nextFloat() * 0.28F;
        this.green = 0.82F + world.random.nextFloat() * 0.16F;
        this.blue = 1.0F;
        this.alpha = 0.92F;
        this.collidesWithWorld = false;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }

    @Override
    public void tick() {
        super.tick();
        this.velocityX *= 0.98;
        this.velocityY *= 0.996;
        if (this.age > GRAVITY_DELAY_TICKS) {
            double ramp = Math.min(1.0, (this.age - GRAVITY_DELAY_TICKS) / (double) GRAVITY_RAMP_TICKS);
            this.velocityY = Math.max(MAX_FALL_SPEED, this.velocityY - MAX_FALL_ACCELERATION * ramp);
        }
        this.velocityZ *= 0.98;
        if (this.age >= this.maxAge - FADE_OUT_TICKS) {
            this.alpha = Math.max(0.0F, 0.92F * (this.maxAge - this.age) / (float) FADE_OUT_TICKS);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new LongFireworkParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
