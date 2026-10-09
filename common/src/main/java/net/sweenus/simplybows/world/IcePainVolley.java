package net.sweenus.simplybows.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.util.CombatTargeting;
import java.util.Comparator;
import java.util.UUID;

/** One distinct visible enemy per arrow, reserved for the entire launch. */
public final class IcePainVolley {
    private final net.sweenus.simplybows.util.WinterfangAbilityRules.Targets targets;
    public final UUID id = UUID.randomUUID();
    public IcePainVolley(ServerWorld world, LivingEntity shooter) {
        double radius = SimplyBowsConfig.INSTANCE.winterfang.homingRadius.get();
        var look = shooter.getRotationVec(1.0F);
        targets = new net.sweenus.simplybows.util.WinterfangAbilityRules.Targets(world.getEntitiesByClass(LivingEntity.class,
                shooter.getBoundingBox().expand(radius), target ->
                        (CombatTargeting.isOffensiveTargetCandidate(target) || target instanceof Monster)
                        && target.isAlive() && CombatTargeting.checkFriendlyFire(target, shooter)
                        && shooter.squaredDistanceTo(target) <= radius * radius && shooter.canSee(target)
                        && target.getPos().subtract(shooter.getPos()).normalize().dotProduct(look) > 0.0)
                .stream().sorted(Comparator.comparingDouble(shooter::squaredDistanceTo))
                .map(LivingEntity::getUuid).toList());
    }
    public UUID nextTarget() { return targets.next(); }
}
