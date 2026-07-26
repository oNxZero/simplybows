package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplybows.world.BeeGraceShieldManager;
import net.sweenus.simplybows.world.CosmicBountyManager;
import net.sweenus.simplybows.world.CosmicGraceTrailManager;
import net.sweenus.simplybows.world.CosmicOrbitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"))
    private void simplybows$triggerCosmicBountyDetonation(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (living.getWorld() instanceof ServerWorld) {
            CosmicBountyManager.triggerAirborneDetonation(living);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void simplybows$consumeBeeGraceShield(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source == null || amount <= 0.0F) {
            return;
        }
        LivingEntity living = (LivingEntity) (Object) this;
        if (!(living.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (CosmicGraceTrailManager.consumeCocoon(serverWorld, living, amount)
                || BeeGraceShieldManager.consumeShield(serverWorld, living)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void simplybows$shareCosmicPainTetherDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || amount <= 0.0F) {
            return;
        }
        LivingEntity living = (LivingEntity) (Object) this;
        if (living.getWorld() instanceof ServerWorld serverWorld) {
            CosmicOrbitManager.sharePainTetherDamage(serverWorld, living, source, amount);
        }
    }
}
