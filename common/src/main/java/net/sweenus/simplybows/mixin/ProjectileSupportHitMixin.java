package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.util.GraceProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class ProjectileSupportHitMixin {
    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void simplybows$protectSupportHit(EntityHitResult hit, CallbackInfo ci) {
        PersistentProjectileEntity arrow = (PersistentProjectileEntity) (Object) this;
        var weapon = arrow.getWeaponStack();
        if (weapon == null || !(weapon.getItem() instanceof SimplyBowItem)) return;
        if (arrow.getDamage() <= 0 || (hit.getEntity() instanceof LivingEntity living && GraceProjectile.shouldCancelDamage(arrow, living))) {
            // Stop before vanilla ignites a target, including when damage would be cancelled later.
            arrow.discard();
            ci.cancel();
        }
    }
}
