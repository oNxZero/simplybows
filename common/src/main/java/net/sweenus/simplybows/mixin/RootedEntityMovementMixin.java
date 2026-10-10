package net.sweenus.simplybows.mixin;
import net.minecraft.entity.*;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class RootedEntityMovementMixin {
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void simplybows$rootMovement(MovementType type,Vec3d movement,CallbackInfo ci) {
        if(net.sweenus.simplybows.world.StoneRootManager.isRooted((Entity)(Object)this)) ci.cancel();
    }
}
