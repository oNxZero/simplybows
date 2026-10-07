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
import net.sweenus.simplybows.entity.EarthArrowEntity;
import net.sweenus.simplybows.entity.EarthSpikeVisualEntity;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.entity.IceChaosWallVisualEntity;
import net.sweenus.simplybows.entity.VineArrowEntity;
import net.sweenus.simplybows.entity.VineFlowerVisualEntity;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.ENTITY_TYPE);

    // Register Homing Arrow
    public static final RegistrySupplier<EntityType<HomingArrowEntity>> HOMING_ARROW = ENTITY_TYPES.register("homing_arrow",
            () -> EntityType.Builder.<HomingArrowEntity>create(HomingArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F) // Arrow dimensions
                    .build(SimplyBows.MOD_ID + ":homing_arrow"));

    public static final RegistrySupplier<EntityType<HomingSpectralArrowEntity>> HOMING_SPECTRAL_ARROW = ENTITY_TYPES.register("homing_spectral_arrow",
            () -> EntityType.Builder.<HomingSpectralArrowEntity>create(HomingSpectralArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":homing_spectral_arrow"));

    public static final RegistrySupplier<EntityType<VineArrowEntity>> VINE_ARROW = ENTITY_TYPES.register("vine_arrow",
            () -> EntityType.Builder.<VineArrowEntity>create(VineArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":vine_arrow"));

    public static final RegistrySupplier<EntityType<BubbleArrowEntity>> BUBBLE_ARROW = ENTITY_TYPES.register("bubble_arrow",
            () -> EntityType.Builder.<BubbleArrowEntity>create(BubbleArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":bubble_arrow"));

    public static final RegistrySupplier<EntityType<BubblePainArrowEntity>> BUBBLE_PAIN_ARROW = ENTITY_TYPES.register("bubble_pain_arrow",
            () -> EntityType.Builder.<BubblePainArrowEntity>create(BubblePainArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.9F, 0.6F)
                    .build(SimplyBows.MOD_ID + ":bubble_pain_arrow"));

    public static final RegistrySupplier<EntityType<BeeArrowEntity>> BEE_ARROW = ENTITY_TYPES.register("bee_arrow",
            () -> EntityType.Builder.<BeeArrowEntity>create(BeeArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.7F, 0.6F)
                    .build(SimplyBows.MOD_ID + ":bee_arrow"));

    public static final RegistrySupplier<EntityType<BlossomArrowEntity>> BLOSSOM_ARROW = ENTITY_TYPES.register("blossom_arrow",
            () -> EntityType.Builder.<BlossomArrowEntity>create(BlossomArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":blossom_arrow"));

    public static final RegistrySupplier<EntityType<EarthArrowEntity>> EARTH_ARROW = ENTITY_TYPES.register("earth_arrow",
            () -> EntityType.Builder.<EarthArrowEntity>create(EarthArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":earth_arrow"));

    public static final RegistrySupplier<EntityType<EarthSpikeVisualEntity>> EARTH_SPIKE_VISUAL = ENTITY_TYPES.register("earth_spike_visual",
            () -> EntityType.Builder.<EarthSpikeVisualEntity>create(EarthSpikeVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.9F, 2.5F)
                    .build(SimplyBows.MOD_ID + ":earth_spike_visual"));

    public static final RegistrySupplier<EntityType<IceChaosWallVisualEntity>> ICE_CHAOS_WALL_VISUAL = ENTITY_TYPES.register("ice_chaos_wall_visual",
            () -> EntityType.Builder.<IceChaosWallVisualEntity>create(IceChaosWallVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.95F, 3.2F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":ice_chaos_wall_visual"));

    public static final RegistrySupplier<EntityType<VineFlowerVisualEntity>> VINE_FLOWER_VISUAL = ENTITY_TYPES.register("vine_flower_visual",
            () -> EntityType.Builder.<VineFlowerVisualEntity>create(VineFlowerVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.8F, 1.2F)
                    .build(SimplyBows.MOD_ID + ":vine_flower_visual"));

    public static final RegistrySupplier<EntityType<BeeHiveVisualEntity>> BEE_HIVE_VISUAL = ENTITY_TYPES.register("bee_hive_visual",
            () -> EntityType.Builder.<BeeHiveVisualEntity>create(BeeHiveVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0F, 1.0F)
                    .build(SimplyBows.MOD_ID + ":bee_hive_visual"));

    public static final RegistrySupplier<EntityType<BeeGraceVisualEntity>> BEE_GRACE_VISUAL = ENTITY_TYPES.register("bee_grace_visual",
            () -> EntityType.Builder.<BeeGraceVisualEntity>create(BeeGraceVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bee_grace_visual"));

    public static final RegistrySupplier<EntityType<BubbleBountyVisualEntity>> BUBBLE_BOUNTY_VISUAL = ENTITY_TYPES.register("bubble_bounty_visual",
            () -> EntityType.Builder.<BubbleBountyVisualEntity>create(BubbleBountyVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bubble_bounty_visual"));

    public static final RegistrySupplier<EntityType<BubbleGraceVisualEntity>> BUBBLE_GRACE_VISUAL = ENTITY_TYPES.register("bubble_grace_visual",
            () -> EntityType.Builder.<BubbleGraceVisualEntity>create(BubbleGraceVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bubble_grace_visual"));

    public static final RegistrySupplier<EntityType<BubbleChaosWaveVisualEntity>> BUBBLE_CHAOS_WAVE_VISUAL = ENTITY_TYPES.register("bubble_chaos_wave_visual",
            () -> EntityType.Builder.<BubbleChaosWaveVisualEntity>create(BubbleChaosWaveVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0F, 1.0F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":bubble_chaos_wave_visual"));

    public static final RegistrySupplier<EntityType<KoiFishVisualEntity>> KOI_FISH_VISUAL = ENTITY_TYPES.register("koi_fish_visual",
            () -> EntityType.Builder.<KoiFishVisualEntity>create(KoiFishVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(3.6F, 1.5F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(SimplyBows.MOD_ID + ":koi_fish_visual"));

    public static void registerEntities() {
        ENTITY_TYPES.register();
    }
}
