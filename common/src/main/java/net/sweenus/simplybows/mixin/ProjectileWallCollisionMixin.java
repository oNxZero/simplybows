package net.sweenus.simplybows.mixin;

import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.sweenus.simplybows.world.EarthSpikeFieldManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileWallCollisionMixin {
    @Inject(method = "hitOrDeflect", at = @At("HEAD"), cancellable = true)
    private void simplybows$blockBeforeImpact(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> ci) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        if (projectile.getWorld() instanceof ServerWorld world && EarthSpikeFieldManager.blockProjectileImpact(world, projectile, hit.getPos()))
            ci.setReturnValue(ProjectileDeflection.NONE);
    }
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void simplybows$blockCollision(HitResult hit, CallbackInfo ci) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        if (projectile.getWorld() instanceof ServerWorld world && EarthSpikeFieldManager.blockProjectileImpact(world, projectile, hit.getPos())) ci.cancel();
    }
}
