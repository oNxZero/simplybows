package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ability cooldown sweep for unique bows.
 * Drawn immediately before the item icon so the bow sits on top of the wash
 * (Legendary Weapons-style), instead of a flat white slab covering the sprite.
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Inject(
            method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void simplybows$drawAbilityCooldownBehindItem(
            LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        simplybows$tryDrawAbilityCooldown(stack, x, y);
    }

    @Inject(
            method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)V",
            at = @At("HEAD")
    )
    private void simplybows$drawAbilityCooldownBehindItemZ(
            LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
        simplybows$tryDrawAbilityCooldown(stack, x, y);
    }

    @Unique
    private void simplybows$tryDrawAbilityCooldown(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SimplyBowItem bowItem)) {
            return;
        }
        if (!bowItem.simplybows$hasAbilityCooldown()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client == null ? 0.0F : client.getRenderTickCounter().getTickDelta(true);
        float progress = bowItem.simplybows$getAbilityCooldownProgress(tickDelta);
        if (progress <= 0.0F) {
            return;
        }
        int top = y + MathHelper.floor(16.0F * (1.0F - progress));
        int bottom = top + MathHelper.ceil(16.0F * progress);
        DrawContext context = (DrawContext) (Object) this;
        // Soft dark-gray wash behind the icon — readable without burying the sprite.
        context.fill(RenderLayer.getGui(), x, top, x + 16, bottom, 0x6A686868);
    }
}
