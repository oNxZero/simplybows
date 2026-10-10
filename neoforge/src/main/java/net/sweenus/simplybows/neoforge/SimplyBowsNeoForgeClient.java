package net.sweenus.simplybows.neoforge;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.client.renderer.BeeArrowEntityRenderer;
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
import net.sweenus.simplybows.client.renderer.KoiFishVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.SimplyBowsArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
import net.sweenus.simplybows.registry.EntityRegistry;

public final class SimplyBowsNeoForgeClient {
    private SimplyBowsNeoForgeClient() {
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimplyBows.Client.initializeClient();
            SimplyBows.LOGGER.info("Registered NeoForge client setup (renderers + particles)");
        });
    }

    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.HOMING_ARROW.get(), HomingArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HOMING_SPECTRAL_ARROW.get(), HomingSpectralArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VINE_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/vine_bow/vine_bow.png")));
        event.registerEntityRenderer(EntityRegistry.BUBBLE_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/bubble_bow/bubble_bow.png")));
        event.registerEntityRenderer(EntityRegistry.BUBBLE_PAIN_ARROW.get(), BubblePainArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_ARROW.get(), BeeArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BLOSSOM_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/blossom_bow/blossom_bow.png")));
        event.registerEntityRenderer(EntityRegistry.EARTH_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/earth_bow/earth_bow.png")));
        event.registerEntityRenderer(EntityRegistry.EARTH_SPIKE_VISUAL.get(), EarthSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ICE_CHAOS_WALL_VISUAL.get(), IceChaosWallVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VINE_FLOWER_VISUAL.get(), VineFlowerVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_HIVE_VISUAL.get(), BeeHiveVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_GRACE_VISUAL.get(), BeeGraceVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_BOUNTY_VISUAL.get(), BubbleBountyVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_GRACE_VISUAL.get(), BubbleGraceVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_CHAOS_WAVE_VISUAL.get(), BubbleChaosWaveVisualEntityRenderer::new);
            event.registerEntityRenderer(EntityRegistry.RUNE_EFFECT.get(), net.sweenus.simplybows.client.renderer.RuneEffectEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.KOI_FISH_VISUAL.get(), KoiFishVisualEntityRenderer::new);
    }
}
