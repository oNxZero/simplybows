package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.sweenus.simplybows.client.tooltip.SimplyBowsTooltipProvider;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture tab presses even when an inventory screen handles the key event. */
@Mixin(value = Keyboard.class, priority = 1100)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void simplybows$tooltipPageKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && window == MinecraftClient.getInstance().getWindow().getHandle()) {
            SimplyBowsTooltipProvider.onKeyPress(key, scanCode);
        }
    }
}
