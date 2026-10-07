package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.world.BowPassiveParticleManager;
import net.sweenus.simplybows.world.MobBowFireManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    private static final double SIMPLYBOWS_MOB_SHOT_RANGE = 40.0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplybows$tickUniqueBowWielder(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        if (!SimplyBowsConfig.INSTANCE.general.enableNonPlayerBowUse.get()
                || !(mob.getWorld() instanceof ServerWorld world)
                || !mob.isAlive()) {
            return;
        }
        ItemStack stack = mob.getMainHandStack();
        if (!(stack.getItem() instanceof SimplyBowItem bow)) {
            return;
        }

        BowPassiveParticleManager.tick(mob, world);

        int interval = Math.max(1, SimplyBowsConfig.INSTANCE.general.nonPlayerBowCheckInterval.get());
        if ((mob.age + mob.getId()) % interval != 0) {
            return;
        }
        int chance = Math.clamp(SimplyBowsConfig.INSTANCE.general.nonPlayerBowChance.get(), 0, 100);
        if (chance <= 0 || mob.getRandom().nextInt(100) >= chance) {
            return;
        }

        // Skeleton-type mobs fire through their (patched) vanilla bow AI; don't double-fire.
        if (mob instanceof AbstractSkeletonEntity) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()
                || !CombatTargeting.checkFriendlyFire(target, mob)
                || mob.squaredDistanceTo(target) > SIMPLYBOWS_MOB_SHOT_RANGE * SIMPLYBOWS_MOB_SHOT_RANGE
                || !mob.getVisibilityCache().canSee(target)
                || !MobBowFireManager.isReady(world, mob, stack)) {
            return;
        }

        bow.performMobShot(world, mob, stack, target);
        MobBowFireManager.markFired(world, mob, stack, interval);
        mob.swingHand(Hand.MAIN_HAND);
    }
}
