package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.entity.KoiFishVisualEntity;
import net.sweenus.simplybows.entity.BeeGraceVisualEntity;
import net.sweenus.simplybows.entity.BeeHiveVisualEntity;
import net.sweenus.simplybows.entity.BlossomArrowEntity;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.entity.BubbleArrowEntity;
import net.sweenus.simplybows.entity.BubbleBountyVisualEntity;
import net.sweenus.simplybows.entity.BubbleChaosWaveVisualEntity;
import net.sweenus.simplybows.entity.BubbleGraceVisualEntity;
import net.sweenus.simplybows.entity.BubblePainArrowEntity;
import net.sweenus.simplybows.entity.CosmicArrowEntity;
import net.sweenus.simplybows.entity.CosmicBountyVisualEntity;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;
import net.sweenus.simplybows.entity.CosmicStrikeVisualEntity;
import net.sweenus.simplybows.entity.CosmicTetherVisualEntity;
import net.sweenus.simplybows.entity.EchoArrowEntity;
import net.sweenus.simplybows.entity.EchoChaosBlackHoleVisualEntity;
import net.sweenus.simplybows.entity.EarthArrowEntity;
import net.sweenus.simplybows.entity.EarthSpikeVisualEntity;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.entity.IceChaosWallVisualEntity;
import net.sweenus.simplybows.entity.ShoulderBowEntity;
import net.sweenus.simplybows.entity.VineArrowEntity;
import net.sweenus.simplybows.entity.VineFlowerVisualEntity;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.ENTITY_TYPE);

    // Register Homing Arrow
    public static final RegistrySupplier<EntityType<HomingArrowEntity>> HOMING_ARROW = ENTITY_TYPES.register("homing_arrow",
            () -> EntityType.Builder.<HomingArrowEntity>create(HomingArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F) // Arrow dimensions
                    .build(SimplyBows.MOD_ID + ":homing_arrow"));

    public static final RegistrySupplier<EntityType<HomingSpectralArrowEntity>> HOMING_SPECTRAL_ARROW = ENTITY_TYPES.register("homing_spectral_arrow",
            () -> EntityType.Builder.<HomingSpectralArrowEntity>create(HomingSpectralArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":homing_spectral_arrow"));

    public static final RegistrySupplier<EntityType<VineArrowEntity>> VINE_ARROW = ENTITY_TYPES.register("vine_arrow",
            () -> EntityType.Builder.<VineArrowEntity>create(VineArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":vine_arrow"));

    public static final RegistrySupplier<EntityType<BubbleArrowEntity>> BUBBLE_ARROW = ENTITY_TYPES.register("bubble_arrow",
            () -> EntityType.Builder.<BubbleArrowEntity>create(BubbleArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":bubble_arrow"));

    public static final RegistrySupplier<EntityType<BubblePainArrowEntity>> BUBBLE_PAIN_ARROW = ENTITY_TYPES.register("bubble_pain_arrow",
            () -> EntityType.Builder.<BubblePainArrowEntity>create(BubblePainArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.9F, 0.6F)
                    .build(SimplyBows.MOD_ID + ":bubble_pain_arrow"));

    public static final RegistrySupplier<EntityType<BeeArrowEntity>> BEE_ARROW = ENTITY_TYPES.register("bee_arrow",
            () -> EntityType.Builder.<BeeArrowEntity>create(BeeArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.7F, 0.6F)
                    .build(SimplyBows.MOD_ID + ":bee_arrow"));

    public static final RegistrySupplier<EntityType<BlossomArrowEntity>> BLOSSOM_ARROW = ENTITY_TYPES.register("blossom_arrow",
            () -> EntityType.Builder.<BlossomArrowEntity>create(BlossomArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":blossom_arrow"));

    public static final RegistrySupplier<EntityType<EarthArrowEntity>> EARTH_ARROW = ENTITY_TYPES.register("earth_arrow",
            () -> EntityType.Builder.<EarthArrowEntity>create(EarthArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":earth_arrow"));

    public static final RegistrySupplier<EntityType<EchoArrowEntity>> ECHO_ARROW = ENTITY_TYPES.register("echo_arrow",
            () -> EntityType.Builder.<EchoArrowEntity>create(EchoArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":echo_arrow"));

    public static final RegistrySupplier<EntityType<CosmicArrowEntity>> COSMIC_ARROW = ENTITY_TYPES.register("cosmic_arrow",
            () -> EntityType.Builder.<CosmicArrowEntity>create(CosmicArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":cosmic_arrow"));

    public static final RegistrySupplier<EntityType<CosmicOrbitVisualEntity>> COSMIC_ORBIT_VISUAL = ENTITY_TYPES.register("cosmic_orbit_visual",
            () -> EntityType.Builder.<CosmicOrbitVisualEntity>create(CosmicOrbitVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(4.0F, 4.0F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":cosmic_orbit_visual"));

    public static final RegistrySupplier<EntityType<CosmicStrikeVisualEntity>> COSMIC_STRIKE_VISUAL = ENTITY_TYPES.register("cosmic_strike_visual",
            () -> EntityType.Builder.<CosmicStrikeVisualEntity>create(CosmicStrikeVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(12.0F, 12.0F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":cosmic_strike_visual"));

    public static final RegistrySupplier<EntityType<CosmicTetherVisualEntity>> COSMIC_TETHER_VISUAL = ENTITY_TYPES.register("cosmic_tether_visual",
            () -> EntityType.Builder.<CosmicTetherVisualEntity>create(CosmicTetherVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(12.0F, 12.0F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":cosmic_tether_visual"));

    public static final RegistrySupplier<EntityType<CosmicBountyVisualEntity>> COSMIC_BOUNTY_VISUAL = ENTITY_TYPES.register("cosmic_bounty_visual",
            () -> EntityType.Builder.<CosmicBountyVisualEntity>create(CosmicBountyVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(8.0F, 8.0F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":cosmic_bounty_visual"));

    public static final RegistrySupplier<EntityType<ShoulderBowEntity>> SHOULDER_BOW = ENTITY_TYPES.register("shoulder_bow",
            () -> EntityType.Builder.<ShoulderBowEntity>create(ShoulderBowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.8F, 0.8F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":shoulder_bow"));

    public static final RegistrySupplier<EntityType<EarthSpikeVisualEntity>> EARTH_SPIKE_VISUAL = ENTITY_TYPES.register("earth_spike_visual",
            () -> EntityType.Builder.<EarthSpikeVisualEntity>create(EarthSpikeVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.9F, 2.5F)
                    .build(SimplyBows.MOD_ID + ":earth_spike_visual"));

    public static final RegistrySupplier<EntityType<IceChaosWallVisualEntity>> ICE_CHAOS_WALL_VISUAL = ENTITY_TYPES.register("ice_chaos_wall_visual",
            () -> EntityType.Builder.<IceChaosWallVisualEntity>create(IceChaosWallVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.95F, 3.2F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":ice_chaos_wall_visual"));

    public static final RegistrySupplier<EntityType<EchoChaosBlackHoleVisualEntity>> ECHO_CHAOS_BLACK_HOLE_VISUAL = ENTITY_TYPES.register("echo_chaos_black_hole_visual",
            () -> EntityType.Builder.<EchoChaosBlackHoleVisualEntity>create(EchoChaosBlackHoleVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.2F, 1.2F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":echo_chaos_black_hole_visual"));

    public static final RegistrySupplier<EntityType<VineFlowerVisualEntity>> VINE_FLOWER_VISUAL = ENTITY_TYPES.register("vine_flower_visual",
            () -> EntityType.Builder.<VineFlowerVisualEntity>create(VineFlowerVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.8F, 1.2F)
                    .build(SimplyBows.MOD_ID + ":vine_flower_visual"));

    public static final RegistrySupplier<EntityType<BeeHiveVisualEntity>> BEE_HIVE_VISUAL = ENTITY_TYPES.register("bee_hive_visual",
            () -> EntityType.Builder.<BeeHiveVisualEntity>create(BeeHiveVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.0F, 1.0F)
                    .build(SimplyBows.MOD_ID + ":bee_hive_visual"));

    public static final RegistrySupplier<EntityType<BeeGraceVisualEntity>> BEE_GRACE_VISUAL = ENTITY_TYPES.register("bee_grace_visual",
            () -> EntityType.Builder.<BeeGraceVisualEntity>create(BeeGraceVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bee_grace_visual"));

    public static final RegistrySupplier<EntityType<BubbleBountyVisualEntity>> BUBBLE_BOUNTY_VISUAL = ENTITY_TYPES.register("bubble_bounty_visual",
            () -> EntityType.Builder.<BubbleBountyVisualEntity>create(BubbleBountyVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bubble_bounty_visual"));

    public static final RegistrySupplier<EntityType<BubbleGraceVisualEntity>> BUBBLE_GRACE_VISUAL = ENTITY_TYPES.register("bubble_grace_visual",
            () -> EntityType.Builder.<BubbleGraceVisualEntity>create(BubbleGraceVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bubble_grace_visual"));

    public static final RegistrySupplier<EntityType<BubbleChaosWaveVisualEntity>> BUBBLE_CHAOS_WAVE_VISUAL = ENTITY_TYPES.register("bubble_chaos_wave_visual",
            () -> EntityType.Builder.<BubbleChaosWaveVisualEntity>create(BubbleChaosWaveVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.0F, 1.0F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bubble_chaos_wave_visual"));

    public static final RegistrySupplier<EntityType<KoiFishVisualEntity>> KOI_FISH_VISUAL = ENTITY_TYPES.register("koi_fish_visual",
            () -> EntityType.Builder.<KoiFishVisualEntity>create(KoiFishVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(3.6F, 1.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":koi_fish_visual"));

    public static void registerEntities() {
        ENTITY_TYPES.register();
    }
}
