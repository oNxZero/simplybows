package net.sweenus.simplybows.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Simply Tooltips 0.1.5 splits Simply Bows upgrades across tabs and only draws
 * rune effectLines on the Forge tab. We keep the base LORE view complete
 * (effects under Rune) and let Forge show the full upgrade panel as the
 * "more details" tab instead of a rune-only panel.
 */
@Pseudo
@Mixin(targets = "net.sweenus.simplytooltips.client.render.TooltipRenderer", remap = false)
public abstract class TooltipRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Lnet/sweenus/simplytooltips/api/TooltipProvider;Ljava/util/List;IIII)V",
            at = @At("STORE"),
            name = "drawUpgradeFull",
            remap = false
    )
    private static boolean simplybows$showRuneEffectsOnLore(
            boolean drawUpgradeFull,
            @Local(name = "drawUpgradeSummary") boolean drawUpgradeSummary,
            @Local(name = "hasUpgrade") boolean hasUpgrade,
            @Local(name = "drawForge") boolean drawForge
    ) {
        // LORE summary gets effect lines; Forge tab gets the full upgrade panel.
        return drawUpgradeFull || drawUpgradeSummary || (hasUpgrade && drawForge);
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Lnet/sweenus/simplytooltips/api/TooltipProvider;Ljava/util/List;IIII)V",
            at = @At("STORE"),
            name = "drawUpgradeRuneDetails",
            remap = false
    )
    private static boolean simplybows$disableRuneOnlyForgePanel(boolean drawUpgradeRuneDetails) {
        // Avoid drawing a second rune-only block once Forge uses the full panel.
        return false;
    }
}
