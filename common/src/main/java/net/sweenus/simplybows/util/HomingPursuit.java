package net.sweenus.simplybows.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/** Stop pursuit after a tracked target teleports, irrespective of its mod or entity class. */
public final class HomingPursuit {
    private UUID targetId;
    private Vec3d lastPosition;
    public boolean targetTeleported(LivingEntity target) {
        if (target == null) { targetId = null; lastPosition = null; return false; }
        Vec3d position = target.getPos();
        boolean teleported = target.getUuid().equals(targetId) && lastPosition != null
                && position.squaredDistanceTo(lastPosition) > 16;
        targetId = target.getUuid();
        lastPosition = position;
        return teleported;
    }
}
