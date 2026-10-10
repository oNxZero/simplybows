package net.sweenus.simplybows.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.sweenus.simplybows.world.IcePrisonManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class FrozenPlayerNetworkMixin {
    @Shadow public ServerPlayerEntity player;
    // Off-thread packets must reach vanilla's main-thread scheduling first.
    private boolean frozen() { return player.getServer() != null && player.getServer().isOnThread() && IcePrisonManager.isFrozen(player); }

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void simplybows$denyMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (!frozen() && !(player.getServer()!=null && player.getServer().isOnThread() && net.sweenus.simplybows.world.StoneRootManager.isRooted(player))) return;
        player.setYaw(packet.getYaw(player.getYaw()));
        player.setPitch(packet.getPitch(player.getPitch()));
        if (Math.abs(packet.getX(player.getX()) - player.getX()) > 0.001
                || Math.abs(packet.getY(player.getY()) - player.getY()) > 0.001
                || Math.abs(packet.getZ(player.getZ()) - player.getZ()) > 0.001)
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        ci.cancel();
    }
    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void simplybows$denyBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (!frozen()) return;
        player.networkHandler.updateSequence(packet.getSequence());
        var pos = packet.getBlockHitResult().getBlockPos();
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getServerWorld(), pos));
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getServerWorld(), pos.offset(packet.getBlockHitResult().getSide())));
        player.currentScreenHandler.syncState();
        ci.cancel();
    }
    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    private void simplybows$denyItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        if (!frozen()) return;
        player.networkHandler.updateSequence(packet.getSequence());
        player.currentScreenHandler.syncState();
        ci.cancel();
    }
    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void simplybows$denyAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (!frozen()) return;
        player.networkHandler.updateSequence(packet.getSequence());
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getServerWorld(), packet.getPos()));
        player.currentScreenHandler.syncState();
        ci.cancel();
    }
    @Inject(method = {"onPlayerInteractEntity", "onHandSwing", "onPlayerInput", "onVehicleMove",
            "onBoatPaddleState", "onClientCommand", "onClickSlot", "onCraftRequest", "onButtonClick"},
            at = @At("HEAD"), cancellable = true)
    private void simplybows$denyOtherActions(CallbackInfo ci) {
        if (frozen()) { player.currentScreenHandler.syncState(); ci.cancel(); }
    }
}
