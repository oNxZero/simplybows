package net.sweenus.simplybows.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.sweenus.simplytooltips.api.TooltipTheme;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.sweenus.simplybows.client.tooltip.SimplyBowsTooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;

/** Keep Simply Bows pages and Simply Tooltips' native tabs from competing. */
@Pseudo
@Mixin(targets = "net.sweenus.simplytooltips.client.render.TooltipRenderer", remap = false)
public abstract class TooltipRendererMixin {
    @ModifyVariable(method = "prepareAbilitySection(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)Lnet/sweenus/simplytooltips/client/render/TooltipRenderer$AbilitySectionData;",
            at = @At("STORE"), ordinal = 1, remap = false)
    private static List<String> simplybows$consumePageHeading(List<String> body,
            @Local(argsOnly = true) ItemStack stack) {
        if ((stack.getItem() instanceof net.sweenus.simplybows.item.unique.SimplyBowItem
                || stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem)
                && !body.isEmpty() && body.getFirst().startsWith(net.sweenus.simplytooltips.api.ModernTooltipModel.SECTION_MARKER)) {
            body.removeFirst();
        }
        return body;
    }
    @ModifyExpressionValue(method = "prepareAbilitySection(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)Lnet/sweenus/simplytooltips/client/render/TooltipRenderer$AbilitySectionData;",
            at = @At(value = "CONSTANT", args = "stringValue=Description"), remap = false)
    private static String simplybows$pageHeading(String original, @Local(argsOnly = true) ItemStack stack,
            @Local(argsOnly = true) List<String> lines) {
        if (!(stack.getItem() instanceof net.sweenus.simplybows.item.unique.SimplyBowItem)
                && !(stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem)) return original;
        return !lines.isEmpty() && lines.getFirst().startsWith(net.sweenus.simplytooltips.api.ModernTooltipModel.SECTION_MARKER)
                ? lines.getFirst().substring(net.sweenus.simplytooltips.api.ModernTooltipModel.SECTION_MARKER.length()) : original;
    }
    // The shorter render overload only delegates; native tab checks live in this overload.
    private static final String RENDER_WITH_COMPONENTS = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Lnet/sweenus/simplytooltips/api/TooltipProvider;Ljava/util/List;IIII)V";

    @WrapOperation(method = RENDER_WITH_COMPONENTS, at = @At(value = "INVOKE",
            target = "Lnet/sweenus/simplytooltips/client/render/TooltipPainter;drawFooterDots(Lnet/minecraft/client/gui/GuiGraphics;IILnet/sweenus/simplytooltips/api/TooltipTheme;)V"), remap = false)
    private static void simplybows$pageMarkers(DrawContext context, int x, int y, TooltipTheme theme,
            Operation<Void> original, @Local(argsOnly = true) ItemStack stack,
            @Local(argsOnly = true) TooltipProvider provider) {
        if (provider instanceof SimplyBowsTooltipProvider bows) bows.drawPageFooter(context, x, y, stack);
        else original.call(context, x, y, theme);
    }

    @WrapOperation(method = RENDER_WITH_COMPONENTS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"), remap = false)
    private static int simplybows$compactRuneRow(DrawContext context, TextRenderer font, Text text,
            int x, int y, int color, boolean shadow, Operation<Integer> original,
            @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) TooltipProvider provider,
            @Local(name = "lineHeight") int lineHeight) {
        if (provider instanceof SimplyBowsTooltipProvider bows && bows.isOverviewRuneRow(stack, text)) y -= lineHeight;
        return original.call(context, font, text, x, y, color, shadow);
    }

    @WrapOperation(method = RENDER_WITH_COMPONENTS, at = @At(value = "INVOKE",
            target = "Lnet/sweenus/simplytooltips/client/render/PipAnimator;drawAnimatedPip(Lnet/minecraft/client/gui/GuiGraphics;IIIZJI)V"), remap = false)
    private static void simplybows$squareLevelPips(DrawContext context, int x, int y, int color,
            boolean filled, long elapsed, int sequence, Operation<Void> original,
            @Local(argsOnly = true) TooltipProvider provider) {
        if (!(provider instanceof SimplyBowsTooltipProvider)) {
            original.call(context, x, y, color, filled, elapsed, sequence);
            return;
        }
        int opaque = color | 0xFF000000;
        context.fill(x, y, x + 5, y + 5, opaque);
        if (filled) context.fill(x, y, x + 5, y + 1, 0xFFBBDFFF);
    }

    @ModifyVariable(method = RENDER_WITH_COMPONENTS, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static List<Text> simplybows$pageCacheKey(List<Text> rawLines,
            @Local(argsOnly = true) ItemStack stack,
            @Local(argsOnly = true) TooltipProvider provider) {
        return provider instanceof SimplyBowsTooltipProvider bows ? bows.withPageCacheKey(stack, rawLines) : rawLines;
    }

    @ModifyExpressionValue(method = RENDER_WITH_COMPONENTS, at = @At(value = "INVOKE",
            target = "Lnet/sweenus/simplytooltips/client/TooltipNavigationConfig;tooltipTabs()Z"), remap = false)
    private static boolean simplybows$useBowPages(boolean nativeTabs,
            @Local(argsOnly = true) TooltipProvider provider) {
        return nativeTabs && !(provider instanceof SimplyBowsTooltipProvider);
    }
}
