package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.upgrade.BowUpgradeData;

public final class ComponentRegistry {

    public static final DeferredRegister<ComponentType<?>> COMPONENTS =
            DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<ComponentType<BowUpgradeData>> UPGRADES = COMPONENTS.register("upgrades", () ->
            ComponentType.<BowUpgradeData>builder()
                    .codec(BowUpgradeData.CODEC)
                    .packetCodec(PacketCodecs.registryCodec(BowUpgradeData.CODEC))
                    .build());

    private ComponentRegistry() {
    }

    public static void register() {
        COMPONENTS.register();
    }
}
