package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stripped DrawContextMixin — only the inventory cooldown bar overlay remains.
 * All tooltip rendering has been moved to Simply Tooltips' DrawContextMixin,
 * delegated via {@link net.sweenus.simplybows.client.tooltip.SimplyBowsTooltipProvider}.
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
    private void simplybows$drawAbilityCooldownOverlay(
            TextRenderer textRenderer, ItemStack stack, int x, int y,
            String countOverride, CallbackInfo ci) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SimplyBowItem bowItem)) return;
        if (!bowItem.simplybows$hasAbilityCooldown()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client == null ? 0.0F : client.getRenderTickCounter().getTickDelta(true);
        float progress = bowItem.simplybows$getAbilityCooldownProgress(tickDelta);
        if (progress <= 0.0F) return;
        int top = y + MathHelper.floor(16.0F * (1.0F - progress));
        int bottom = top + MathHelper.ceil(16.0F * progress);
        DrawContext context = (DrawContext) (Object) this;
        context.fill(RenderLayer.getGuiOverlay(), x, top, x + 16, bottom, 0x59FFFFFF);
    }
}
