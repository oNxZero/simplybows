package net.sweenus.simplybows;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.utils.Env;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.client.ClientAbilityCooldownCache;
import net.sweenus.simplybows.client.particle.LongEndRodParticle;
import net.sweenus.simplybows.client.particle.LongFireworkParticle;
import net.sweenus.simplybows.client.particle.WaveParticle;
import net.sweenus.simplybows.network.AbilityCooldownPayload;
import net.sweenus.simplybows.client.renderer.BeeArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.KoiFishVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeGraceVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeHiveVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleBountyVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleChaosWaveVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleGraceVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubblePainArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.EarthSpikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingSpectralArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.IceChaosWallVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.SimplyBowsArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
import net.sweenus.simplybows.command.SimplyBowsCommands;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.world.RuneUseCooldown;
import net.sweenus.simplybows.registry.ComponentRegistry;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.registry.ParticleRegistry;
import net.sweenus.simplybows.registry.SimplyBowsCreativeTabRegistry;
import net.sweenus.simplybows.registry.SimplyBowsItemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;

public final class SimplyBows {
    public static final String MOD_ID = "simplybows";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean debugMode() {
        return SimplyBowsConfig.INSTANCE.general.debugMode.get();
    }

    public static boolean modernTooltipsEnabled() {
        return SimplyBowsConfig.INSTANCE.general.modernTooltipsEnabled.get();
    }

    public static void init() {
        // Register content first. A bad/outdated config.toml must never prevent item/entity IDs
        // from existing — that bricks saved worlds with "Unknown registry key" on reload.
        ComponentRegistry.register();
        ItemRegistry.ITEM.register();
        SimplyBowsCreativeTabRegistry.register();
        EntityRegistry.registerEntities();
        ParticleRegistry.registerParticles();
        SimplyBowsCommands.register();

        try {
            LOGGER.info("Simply Bows config loaded: {}", SimplyBowsConfig.INSTANCE.getId());
        } catch (Throwable t) {
            LOGGER.error("Simply Bows config failed to load. Items/entities are still registered; fix or delete config/simplybows/config.toml", t);
        }

        // Wire global ability CD overlay without a class-init cycle.
        RuneUseCooldown.CLIENT_SYNC = (player, ticks) ->
                SimplyBowItem.simplybows$sendCooldownPacket(
                        player,
                        RuneUseCooldown.GLOBAL_BOW_KEY,
                        System.currentTimeMillis() + (long) ticks * 50L,
                        ticks);

        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (player.getWorld().isClient()) {
                return;
            }
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                BowUpgradeData.migrateLegacy(inventory.getStack(slot));
            }
        });
        if (Platform.getEnvironment() != Env.CLIENT) {
            NetworkManager.registerS2CPayloadType(AbilityCooldownPayload.ID, AbilityCooldownPayload.CODEC);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Client {

        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            SimplyBowsItemProperties.addSimplyBowsItemProperties();
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
                ClientAbilityCooldownCache.clearAll();
            });

            EntityRendererRegistry.register(EntityRegistry.HOMING_ARROW, HomingArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.HOMING_SPECTRAL_ARROW, HomingSpectralArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VINE_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, bowArrowTexture("vine")));
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, bowArrowTexture("bubble")));
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_PAIN_ARROW, BubblePainArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_ARROW, BeeArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BLOSSOM_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, bowArrowTexture("blossom")));
            EntityRendererRegistry.register(EntityRegistry.EARTH_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, bowArrowTexture("earth")));
            EntityRendererRegistry.register(EntityRegistry.EARTH_SPIKE_VISUAL, EarthSpikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ICE_CHAOS_WALL_VISUAL, IceChaosWallVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VINE_FLOWER_VISUAL, VineFlowerVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_HIVE_VISUAL, BeeHiveVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_GRACE_VISUAL, BeeGraceVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_BOUNTY_VISUAL, BubbleBountyVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_GRACE_VISUAL, BubbleGraceVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_CHAOS_WAVE_VISUAL, BubbleChaosWaveVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.KOI_FISH_VISUAL, KoiFishVisualEntityRenderer::new);

            ParticleProviderRegistry.register(ParticleRegistry.JAPANESE_WAVE, WaveParticle.Factory::new);
            ParticleProviderRegistry.register(ParticleRegistry.LONG_END_ROD, LongEndRodParticle.Factory::new);
            ParticleProviderRegistry.register(ParticleRegistry.LONG_FIREWORK, LongFireworkParticle.Factory::new);
            LOGGER.info("Registered Architectury particle providers for Simply Bows");

            NetworkManager.registerReceiver(
                    NetworkManager.Side.S2C,
                    AbilityCooldownPayload.ID,
                    AbilityCooldownPayload.CODEC,
                    (payload, context) -> context.queue(() ->
                            ClientAbilityCooldownCache.update(payload.bowKey(), payload.endMs(), payload.totalTicks()))
            );

            LongSupplier clientWorldTickReader = () -> {
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client == null || client.world == null) {
                    return 0L;
                }
                return client.world.getTime();
            };

            ClientAbilityCooldownCache.setGameTickReader(clientWorldTickReader);

            SimplyBowItem.CLIENT_COOLDOWN_READER = ClientAbilityCooldownCache::get;
            SimplyBowItem.CLIENT_COOLDOWN_TICK_READER = clientWorldTickReader;

            net.sweenus.simplytooltips.api.TooltipProviderRegistry.register(
                    new net.sweenus.simplybows.client.tooltip.SimplyBowsTooltipProvider(), 100);
            LOGGER.info("Registered SimplyBowsTooltipProvider with Simply Tooltips");
        }

        private static Identifier bowArrowTexture(String bowName) {
            return Identifier.of(SimplyBows.MOD_ID, "textures/item/" + bowName + "_bow/" + bowName + "_bow_arrow.png");
        }
    }

}
