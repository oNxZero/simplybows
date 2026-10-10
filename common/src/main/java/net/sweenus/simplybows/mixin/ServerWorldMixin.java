package net.sweenus.simplybows.mixin;

import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.world.BlossomStormManager;
import net.sweenus.simplybows.world.BeeChaosHoneyStormManager;
import net.sweenus.simplybows.world.BlossomChaosKoiManager;
import net.sweenus.simplybows.world.BeeGraceShieldManager;
import net.sweenus.simplybows.world.BeeHiveSwarmManager;
import net.sweenus.simplybows.world.BubbleColumnFieldManager;
import net.sweenus.simplybows.world.BubbleChaosWaveManager;
import net.sweenus.simplybows.world.EarthChaosSunderManager;
import net.sweenus.simplybows.world.EarthSpikeFieldManager;
import net.sweenus.simplybows.world.IceChaosWallManager;
import net.sweenus.simplybows.world.IceFrostBloomManager;
import net.sweenus.simplybows.world.MobBowFireManager;
import net.sweenus.simplybows.world.VineFlowerFieldManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void simplybows$freezeEntity(net.minecraft.entity.Entity entity, CallbackInfo ci) {
        if (net.sweenus.simplybows.world.IcePrisonManager.isFrozen(entity)) {
            net.sweenus.simplybows.world.IcePrisonManager.hold(entity);
            ci.cancel();
            return;
        }
        net.sweenus.simplybows.world.IceFrostSlowManager.beforeTick((ServerWorld) (Object) this, entity);
    }

    @Inject(method = "tickEntity", at = @At("TAIL"))
    private void simplybows$frostSlow(net.minecraft.entity.Entity entity, CallbackInfo ci) {
        net.sweenus.simplybows.world.IceFrostSlowManager.afterTick((ServerWorld) (Object) this, entity);
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void simplybows$tickVineFields(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;
        net.sweenus.simplybows.world.IcePrisonManager.tick(world);
        net.sweenus.simplybows.world.StoneRootManager.tick(world);
        net.sweenus.simplybows.world.IceFrostSlowManager.tick(world);
        if (VineFlowerFieldManager.hasActive(world)) {
            VineFlowerFieldManager.tick(world);
        }
        if (BubbleColumnFieldManager.hasActive(world)) {
            BubbleColumnFieldManager.tick(world);
        }
        if (BubbleChaosWaveManager.hasActive(world)) {
            BubbleChaosWaveManager.tick(world);
        }
        if (IceChaosWallManager.hasActive(world)) {
            IceChaosWallManager.tick(world);
        }
        if (IceFrostBloomManager.hasActive(world)) {
            IceFrostBloomManager.tick(world);
        }
        if (BlossomStormManager.hasActive(world)) {
            BlossomStormManager.tick(world);
        }
        if (EarthChaosSunderManager.hasActive(world)) {
            EarthChaosSunderManager.tick(world);
        }
        if (EarthSpikeFieldManager.hasActive(world)) {
            EarthSpikeFieldManager.tick(world);
        }
        if (BeeChaosHoneyStormManager.hasActive(world)) {
            BeeChaosHoneyStormManager.tick(world);
        }
        if (BlossomChaosKoiManager.hasActive(world)) {
            BlossomChaosKoiManager.tick(world);
        }
        if (BeeHiveSwarmManager.hasActive(world)) {
            BeeHiveSwarmManager.tick(world);
        }
        if (BeeGraceShieldManager.hasActive(world)) {
            BeeGraceShieldManager.tick(world);
        }
        MobBowFireManager.tick(world);
    }
}
