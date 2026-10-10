package net.sweenus.simplybows.world;

import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.sweenus.simplybows.entity.RuneEffectEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.*;
import java.util.*;

public final class RuneEffectManager {
    private RuneEffectManager() {}
    public static void cast(ServerWorld world, int kind, Vec3d position, Entity source, LivingEntity directTarget, BowUpgradeData upgrades, boolean reserveCooldown) {
        if (!(source instanceof LivingEntity owner)) return;
        if (reserveCooldown && !RuneUseCooldown.isPlayerReady(world, owner.getUuid())) return;
        if (reserveCooldown) RuneUseCooldown.start(world, owner.getUuid(), "formation", "earth", RuneEffectRules.cooldown(kind, upgrades.stringLevel()));
        if (kind == RuneEffectRules.SWARM) {
            if (directTarget != null && CombatTargeting.checkFriendlyFire(directTarget, owner)) spawn(world, kind, directTarget.getPos(), owner, directTarget, upgrades);
        } else {
            BlockPos origin = BlockPos.ofFloored(position.add(0, 1, 0));
            for (int down=0;down<=8;down++) {
                BlockPos pos = origin.down(down);
                var shape = world.getBlockState(pos).getCollisionShape(world, pos);
                if (!shape.isEmpty()) { position = new Vec3d(position.x, pos.getY()+shape.getMax(Direction.Axis.Y), position.z); break; }
            }
            spawn(world, kind, position, owner, null, upgrades);
        }
    }
    private static void spawn(ServerWorld world, int kind, Vec3d position, LivingEntity owner, LivingEntity target, BowUpgradeData upgrades) {
        world.spawnEntity(new RuneEffectEntity(world, kind, position, owner, target, upgrades.stringLevel(), upgrades.frameLevel()));
    }
}
