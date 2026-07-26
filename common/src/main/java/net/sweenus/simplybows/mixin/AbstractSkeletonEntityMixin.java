package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.world.MobBowFireManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeletonEntity.class)
public abstract class AbstractSkeletonEntityMixin {

    /**
     * Vanilla only selects BowAttackGoal for the exact Items.BOW; accept unique bows too.
     */
    @Redirect(
            method = "updateAttackType",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z")
    )
    private boolean simplybows$acceptUniqueBows(ItemStack stack, Item item) {
        return stack.isOf(item) || stack.getItem() instanceof SimplyBowItem;
    }

    /**
     * getHandPossiblyHolding answers OFF_HAND unless the main hand holds the exact
     * Items.BOW, which would make updateAttackType inspect the (empty) offhand and
     * fall back to the melee goal. Resolve MAIN_HAND for unique bows too.
     */
    @Redirect(
            method = "updateAttackType",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getHandPossiblyHolding(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/Item;)Lnet/minecraft/util/Hand;")
    )
    private Hand simplybows$resolveUniqueBowHand(LivingEntity entity, Item item) {
        ItemStack mainHand = entity.getMainHandStack();
        return mainHand.isOf(item) || mainHand.getItem() instanceof SimplyBowItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }

    /**
     * Route skeleton bow-AI shots through the unique bow's full pipeline (custom arrows
     * + rune abilities) instead of the vanilla plain-arrow shot.
     */
    @Inject(method = "shootAt(Lnet/minecraft/entity/LivingEntity;F)V", at = @At("HEAD"), cancellable = true)
    private void simplybows$shootUniqueBow(LivingEntity target, float pullProgress, CallbackInfo ci) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        ItemStack stack = skeleton.getMainHandStack();
        if (!(stack.getItem() instanceof SimplyBowItem bow)
                || !(skeleton.getWorld() instanceof ServerWorld world)
                || !SimplyBowsConfig.INSTANCE.general.enableNonPlayerBowUse.get()) {
            // Config disabled: let vanilla fire a plain arrow from the unique bow.
            return;
        }
        ci.cancel();
        if (!MobBowFireManager.isReady(world, skeleton, stack)) {
            return;
        }
        bow.performMobShot(world, skeleton, stack, target);
        MobBowFireManager.markFired(world, skeleton, stack,
                SimplyBowsConfig.INSTANCE.general.nonPlayerBowCheckInterval.get());
        skeleton.swingHand(Hand.MAIN_HAND);
    }
}
