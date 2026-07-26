package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BowAttackGoal.class)
public abstract class BowAttackGoalMixin {

    /**
     * Vanilla's isHoldingBow checks the exact Items.BOW; let the goal run while the
     * actor holds a unique bow as well.
     */
    @Redirect(
            method = "isHoldingBow",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/HostileEntity;isHolding(Lnet/minecraft/item/Item;)Z")
    )
    private boolean simplybows$acceptUniqueBows(HostileEntity actor, Item item) {
        return actor.isHolding(item)
                || actor.isHolding(stack -> stack.getItem() instanceof SimplyBowItem);
    }

    /**
     * tick() starts drawing via setCurrentHand(getHandPossiblyHolding(actor, Items.BOW));
     * with a unique bow in the main hand that resolves to the empty OFF_HAND, so
     * setCurrentHand no-ops and the mob never draws or fires. Resolve MAIN_HAND for
     * unique bows too.
     */
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getHandPossiblyHolding(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/Item;)Lnet/minecraft/util/Hand;")
    )
    private Hand simplybows$resolveUniqueBowHand(LivingEntity entity, Item item) {
        ItemStack mainHand = entity.getMainHandStack();
        return mainHand.isOf(item) || mainHand.getItem() instanceof SimplyBowItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }
}
