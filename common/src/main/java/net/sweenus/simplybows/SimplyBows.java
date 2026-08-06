package net.sweenus.simplybows;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.utils.Env;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.client.ClientAbilityCooldownCache;
import net.sweenus.simplybows.client.particle.LongEndRodParticle;
import net.sweenus.simplybows.client.particle.LongFireworkParticle;
import net.sweenus.simplybows.client.particle.WaveParticle;
import net.sweenus.simplybows.network.AbilityCooldownPayload;
import net.sweenus.simplybows.network.CelestialSwiftnessPayload;
import net.sweenus.simplybows.client.renderer.BeeArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.KoiFishVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeGraceVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeHiveVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleBountyVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleChaosWaveVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleGraceVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubblePainArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.EarthSpikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.EchoChaosBlackHoleVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingSpectralArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.IceChaosWallVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.ShoulderBowEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicBountyVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicOrbitVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicStrikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicTetherVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.SimplyBowsArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
import net.sweenus.simplybows.command.SimplyBowsCommands;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.registry.ParticleRegistry;
import net.sweenus.simplybows.registry.SimplyBowsCreativeTabRegistry;
import net.sweenus.simplybows.registry.SimplyBowsItemProperties;
import net.sweenus.simplybows.util.CelestialSwiftnessTracker;
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
        LOGGER.info("Simply Bows config loaded: {}", SimplyBowsConfig.INSTANCE.getId());

        ItemRegistry.ITEM.register();
        SimplyBowsCreativeTabRegistry.register();
        EntityRegistry.registerEntities();
        ParticleRegistry.registerParticles();
        SimplyBowsCommands.register();
    }

    @Environment(EnvType.CLIENT)
    public static class Client {

        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            SimplyBowsItemProperties.addSimplyBowsItemProperties();
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
                ClientAbilityCooldownCache.clearAll();
                CelestialSwiftnessTracker.clearAll();
                CosmicArrowEntityRenderer.clearTrails();
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
            EntityRendererRegistry.register(EntityRegistry.ECHO_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, bowArrowTexture("echo")));
            EntityRendererRegistry.register(EntityRegistry.COSMIC_ARROW, context ->
                    new CosmicArrowEntityRenderer<>(context, bowArrowTexture("cosmic")));
            EntityRendererRegistry.register(EntityRegistry.COSMIC_ORBIT_VISUAL, CosmicOrbitVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.COSMIC_STRIKE_VISUAL, CosmicStrikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.COSMIC_TETHER_VISUAL, CosmicTetherVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.COSMIC_BOUNTY_VISUAL, CosmicBountyVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SHOULDER_BOW, ShoulderBowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.EARTH_SPIKE_VISUAL, EarthSpikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ICE_CHAOS_WALL_VISUAL, IceChaosWallVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ECHO_CHAOS_BLACK_HOLE_VISUAL, EchoChaosBlackHoleVisualEntityRenderer::new);
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
                    NetworkManager.s2c(),
                    AbilityCooldownPayload.CHANNEL_ID,
                    (buf, context) -> {
                        AbilityCooldownPayload payload = AbilityCooldownPayload.decode(buf);
                        context.queue(() -> ClientAbilityCooldownCache.update(
                                payload.bowKey, payload.endMs, payload.totalTicks));
                    }
            );

            LongSupplier clientWorldTickReader = () -> {
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client == null || client.world == null) {
                    return 0L;
                }
                return client.world.getTime();
            };

            NetworkManager.registerReceiver(
                    NetworkManager.s2c(),
                    CelestialSwiftnessPayload.CHANNEL_ID,
                    (buf, context) -> {
                        CelestialSwiftnessPayload payload = CelestialSwiftnessPayload.decode(buf);
                        context.queue(() -> CelestialSwiftnessTracker.set(
                                payload.playerId,
                                payload.stacks,
                                clientWorldTickReader.getAsLong() + Math.max(1, payload.durationTicks)));
                    }
            );

            ClientAbilityCooldownCache.setGameTickReader(clientWorldTickReader);

            SimplyBowItem.CLIENT_COOLDOWN_READER = ClientAbilityCooldownCache::get;
            SimplyBowItem.CLIENT_COOLDOWN_TICK_READER = clientWorldTickReader;

            net.sweenus.simplytooltips.api.TooltipProviderRegistry.register(
                    new net.sweenus.simplybows.client.tooltip.SimplyBowsTooltipProvider(), 100);
            LOGGER.info("Registered SimplyBowsTooltipProvider with Simply Tooltips");

        }

        private static Identifier bowArrowTexture(String bowName) {
            return new Identifier(SimplyBows.MOD_ID, "textures/item/" + bowName + "_bow/" + bowName + "_bow_arrow.png");
        }
    }

}
