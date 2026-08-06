package net.sweenus.simplybows.mixin;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.world.CosmicChaosSunManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void simplybows$captureCosmicChaosSunProjectile(CallbackInfo ci) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        if (projectile.getWorld() instanceof ServerWorld serverWorld) {
            CosmicChaosSunManager.tryCaptureProjectile(serverWorld, projectile);
        }
    }
}
