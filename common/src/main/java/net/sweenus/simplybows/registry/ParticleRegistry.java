package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.SimplyBows;

public class ParticleRegistry {

    public static final DeferredRegister<net.minecraft.particle.ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final RegistrySupplier<DefaultParticleType> JAPANESE_WAVE =
            PARTICLE_TYPES.register("japanese_wave", () -> new DefaultParticleType(false) {});

    public static final RegistrySupplier<DefaultParticleType> LONG_END_ROD =
            PARTICLE_TYPES.register("long_end_rod", () -> new DefaultParticleType(false) {});

    public static final RegistrySupplier<DefaultParticleType> LONG_FIREWORK =
            PARTICLE_TYPES.register("long_firework", () -> new DefaultParticleType(false) {});

    public static void registerParticles() {
        PARTICLE_TYPES.register();
    }
}
