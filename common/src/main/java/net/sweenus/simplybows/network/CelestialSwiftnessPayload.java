package net.sweenus.simplybows.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;

import java.util.UUID;

public final class CelestialSwiftnessPayload {

    public static final Identifier CHANNEL_ID =
            new Identifier(SimplyBows.MOD_ID, "celestial_swiftness_sync");

    public final UUID playerId;
    public final int stacks;
    public final int durationTicks;

    public CelestialSwiftnessPayload(UUID playerId, int stacks, int durationTicks) {
        this.playerId = playerId;
        this.stacks = stacks;
        this.durationTicks = durationTicks;
    }

    public static void encode(CelestialSwiftnessPayload payload, PacketByteBuf buf) {
        buf.writeUuid(payload.playerId);
        buf.writeInt(payload.stacks);
        buf.writeInt(payload.durationTicks);
    }

    public static CelestialSwiftnessPayload decode(PacketByteBuf buf) {
        return new CelestialSwiftnessPayload(buf.readUuid(), buf.readInt(), buf.readInt());
    }
}
