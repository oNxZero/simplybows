package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.entity.VineArrowEntity;
import net.sweenus.simplybows.world.BeeGraceShieldManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void simplybows$consumeBeeGraceShield(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source == null || amount <= 0.0F) {
            return;
        }
        LivingEntity living = (LivingEntity) (Object) this;
        if ((living instanceof PlayerEntity || living instanceof IronGolemEntity || living instanceof AnimalEntity) && source.getSource() instanceof VineArrowEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (!(living.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (BeeGraceShieldManager.consumeShield(serverWorld, living)) {
            cir.setReturnValue(false);
        }
    }
}
