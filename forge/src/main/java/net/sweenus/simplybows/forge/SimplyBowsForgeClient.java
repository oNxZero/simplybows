package net.sweenus.simplybows.forge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.client.particle.LongEndRodParticle;
import net.sweenus.simplybows.client.particle.LongFireworkParticle;
import net.sweenus.simplybows.client.particle.WaveParticle;
import net.sweenus.simplybows.client.renderer.CosmicArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicBountyVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicOrbitVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicStrikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicTetherVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeArrowEntityRenderer;
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
import net.sweenus.simplybows.client.renderer.KoiFishVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.ShoulderBowEntityRenderer;
import net.sweenus.simplybows.client.renderer.SimplyBowsArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.registry.ParticleRegistry;

public final class SimplyBowsForgeClient {
    private SimplyBowsForgeClient() {
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimplyBows.Client.initializeClient();
            MinecraftForge.EVENT_BUS.addListener(SimplyBowsForgeClient::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(SimplyBowsForgeClient::onRenderLevelStage);
            SimplyBows.LOGGER.info("Registered Forge client setup (renderers + particles)");
        });
    }

    // Forge 47 has no split ClientTickEvent.Post; filter on the END phase instead.
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            CosmicArrowEntityRenderer.clientTick(client.world.getTime());
        } else {
            CosmicArrowEntityRenderer.clearTrails();
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        CosmicArrowEntityRenderer.renderOrphanTrails(
                client.world.getTime(),
                event.getCamera().getPos(),
                event.getPoseStack(),
                consumers
        );
        consumers.draw(RenderLayer.getLines());
        consumers.draw(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
    }

    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.HOMING_ARROW.get(), HomingArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HOMING_SPECTRAL_ARROW.get(), HomingSpectralArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VINE_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, new Identifier(SimplyBows.MOD_ID, "textures/item/vine_bow/vine_bow.png")));
        event.registerEntityRenderer(EntityRegistry.BUBBLE_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, new Identifier(SimplyBows.MOD_ID, "textures/item/bubble_bow/bubble_bow.png")));
        event.registerEntityRenderer(EntityRegistry.BUBBLE_PAIN_ARROW.get(), BubblePainArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_ARROW.get(), BeeArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BLOSSOM_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, new Identifier(SimplyBows.MOD_ID, "textures/item/blossom_bow/blossom_bow.png")));
        event.registerEntityRenderer(EntityRegistry.EARTH_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, new Identifier(SimplyBows.MOD_ID, "textures/item/earth_bow/earth_bow.png")));
        event.registerEntityRenderer(EntityRegistry.ECHO_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, new Identifier(SimplyBows.MOD_ID, "textures/item/echo_bow/echo_bow.png")));
        event.registerEntityRenderer(EntityRegistry.COSMIC_ARROW.get(), context ->
                new CosmicArrowEntityRenderer<>(context, new Identifier(SimplyBows.MOD_ID, "textures/item/echo_bow/echo_bow.png")));
        event.registerEntityRenderer(EntityRegistry.COSMIC_ORBIT_VISUAL.get(), CosmicOrbitVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COSMIC_STRIKE_VISUAL.get(), CosmicStrikeVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COSMIC_TETHER_VISUAL.get(), CosmicTetherVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COSMIC_BOUNTY_VISUAL.get(), CosmicBountyVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SHOULDER_BOW.get(), ShoulderBowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.EARTH_SPIKE_VISUAL.get(), EarthSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ICE_CHAOS_WALL_VISUAL.get(), IceChaosWallVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ECHO_CHAOS_BLACK_HOLE_VISUAL.get(), EchoChaosBlackHoleVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VINE_FLOWER_VISUAL.get(), VineFlowerVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_HIVE_VISUAL.get(), BeeHiveVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_GRACE_VISUAL.get(), BeeGraceVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_BOUNTY_VISUAL.get(), BubbleBountyVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_GRACE_VISUAL.get(), BubbleGraceVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_CHAOS_WAVE_VISUAL.get(), BubbleChaosWaveVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.KOI_FISH_VISUAL.get(), KoiFishVisualEntityRenderer::new);
    }

    public static void onRegisterParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.JAPANESE_WAVE.get(), WaveParticle.Factory::new);
        event.registerSpriteSet(ParticleRegistry.LONG_END_ROD.get(), LongEndRodParticle.Factory::new);
        event.registerSpriteSet(ParticleRegistry.LONG_FIREWORK.get(), LongFireworkParticle.Factory::new);
        SimplyBows.LOGGER.info("Registered Forge particle providers for Simply Bows");
    }
}
