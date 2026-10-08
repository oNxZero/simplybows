package net.sweenus.simplybows.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    @Final
    private Property levelCost;

    @Shadow
    private int repairItemUsage;

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void simplybows$applyUpgradeRecipe(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack left = handler.getSlot(0).getStack();
        ItemStack right = handler.getSlot(1).getStack();
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        // Upgrade parts contain "bow" in their item id (enchanted_bow_string, etc.).
        // Never treat them as upgradeable bows, or Frame+String becomes a fake anvil craft.
        if (isUpgradeComponent(left) && isUpgradeComponent(right)) {
            handler.getSlot(2).setStack(ItemStack.EMPTY);
            this.levelCost.set(0);
            this.repairItemUsage = 0;
            ((ScreenHandler) (Object) this).sendContentUpdates();
            ci.cancel();
            return;
        }

        ItemStack bowStack;
        ItemStack componentStack;
        boolean componentOnRight;
        if (isUpgradeableBow(left) && isUpgradeComponent(right)) {
            bowStack = left;
            componentStack = right;
            componentOnRight = true;
        } else if (isUpgradeableBow(right) && isUpgradeComponent(left)) {
            bowStack = right;
            componentStack = left;
            componentOnRight = false;
        } else {
            return;
        }
        if (!(componentStack.getItem() instanceof BowUpgradeComponentItem upgradeComponent)) {
            return;
        }

        BowUpgradeData current = BowUpgradeData.from(bowStack);
        BowUpgradeData updated = upgradeComponent.applyTo(current);
        if (current.equals(updated)) {
            handler.getSlot(2).setStack(ItemStack.EMPTY);
            this.levelCost.set(0);
            this.repairItemUsage = 0;
            ((ScreenHandler) (Object) this).sendContentUpdates();
            ci.cancel();
            return;
        }

        ItemStack result = bowStack.copy();
        updated.write(result);
        handler.getSlot(2).setStack(result);
        this.levelCost.set(upgradeComponent.getAnvilCost(current, updated));
        this.repairItemUsage = componentOnRight ? 1 : 0;
        ((ScreenHandler) (Object) this).sendContentUpdates();
        ci.cancel();
    }

    private static boolean isUpgradeableBow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SimplyBowItem;
    }

    private static boolean isUpgradeComponent(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BowUpgradeComponentItem) {
            return true;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id != null && "simplybows".equals(id.getNamespace()) && id.getPath().startsWith("upgrades/");
    }
}
