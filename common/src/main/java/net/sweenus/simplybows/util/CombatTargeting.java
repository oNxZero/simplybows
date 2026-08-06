package net.sweenus.simplybows.util;

import dev.architectury.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.compat.opac.OpacCompat;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class CombatTargeting {

    private static final Set<String> TARGET_WHITELIST = new HashSet<>();
    private static final Identifier RANGED_WEAPON_DAMAGE_ATTRIBUTE_ID = new Identifier("ranged_weapon", "damage");

    static {
        addTargetWhitelist("target_dummy");
        addTargetWhitelist("dummmmmmy:target_dummy");
    }

    private CombatTargeting() {
    }

    // Check if we should be able to hit the target
    public static boolean checkFriendlyFire(LivingEntity livingEntity, LivingEntity attackingEntity) {
        if (livingEntity == null || attackingEntity == null) {
            return false;
        }
        if (livingEntity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (!checkEntityBlacklist(livingEntity, attackingEntity)) {
            return false;
        }
        if (livingEntity == attackingEntity) {
            return false;
        }
        if (isTargetWhitelisted(livingEntity)) {
            return true;
        }
        if (livingEntity instanceof VillagerEntity && !(attackingEntity instanceof HostileEntity)) {
            return false;
        }
        if (isMonsterFaction(attackingEntity) && isMonsterFaction(livingEntity)) {
            return false;
        }

        AbstractTeam attackerTeam = attackingEntity.getScoreboardTeam();
        AbstractTeam targetTeam = livingEntity.getScoreboardTeam();
        if (isOpacLoaded() && livingEntity instanceof PlayerEntity && attackingEntity instanceof PlayerEntity playerEntity) {
            return OpacCompat.checkOpacFriendlyFire(livingEntity, playerEntity);
        }
        if (attackerTeam != null && targetTeam != null && livingEntity.isTeammate(attackingEntity)) {
            return false;
        }

        if (livingEntity instanceof PlayerEntity playerEntity && attackingEntity instanceof PlayerEntity player) {
            if (playerEntity == attackingEntity) {
                return false;
            }
            return playerEntity.shouldDamagePlayer(player);
        }
        if (attackingEntity instanceof Tameable attackingTameable) {
            UUID attackerOwnerUuid = attackingTameable.getOwnerUuid();
            if (attackerOwnerUuid != null) {
                if (attackerOwnerUuid.equals(livingEntity.getUuid())) {
                    return false;
                }
                if (livingEntity instanceof Tameable targetTameable) {
                    UUID targetOwnerUuid = targetTameable.getOwnerUuid();
                    if (targetOwnerUuid != null && targetOwnerUuid.equals(attackerOwnerUuid)) {
                        return false;
                    }
                }
            }
        }
        if (livingEntity instanceof Tameable tameable) {
            UUID ownerUuid = tameable.getOwnerUuid();
            if (ownerUuid != null) {
                if (ownerUuid.equals(attackingEntity.getUuid())) {
                    return false;
                }
                if (attackingEntity instanceof Tameable attackingTameable) {
                    UUID attackerOwnerUuid = attackingTameable.getOwnerUuid();
                    if (attackerOwnerUuid != null && attackerOwnerUuid.equals(ownerUuid)) {
                        return false;
                    }
                }
                LivingEntity owner = tameable.getOwner();
                if (owner != null && owner != attackingEntity
                        && owner instanceof PlayerEntity ownerPlayer
                        && attackingEntity instanceof PlayerEntity playerEntity) {
                    if (isOpacLoaded()) {
                        return OpacCompat.checkOpacFriendlyFire(ownerPlayer, playerEntity);
                    }
                    return playerEntity.shouldDamagePlayer(ownerPlayer);
                }
                return true;
            }
            return true;
        }
        return true;
    }

    /**
     * An entity belongs to the monster faction if it is a {@link Monster}; tamed
     * entities inherit their owner's faction (a player-tamed monster is not
     * monster-faction, a monster-owned minion is).
     */
    public static boolean isMonsterFaction(LivingEntity entity) {
        if (entity instanceof Monster) {
            if (entity instanceof Tameable tameable) {
                LivingEntity resolvedOwner = resolveTameableOwner(tameable, entity);
                return resolvedOwner != null && isMonsterFaction(resolvedOwner);
            }
            return true;
        }
        if (entity instanceof Tameable tameable) {
            LivingEntity resolvedOwner = resolveTameableOwner(tameable, entity);
            if (resolvedOwner != null) {
                return isMonsterFaction(resolvedOwner);
            }
        }
        return false;
    }

    @Nullable
    private static LivingEntity resolveTameableOwner(Tameable tameable, LivingEntity entity) {
        LivingEntity owner = tameable.getOwner();
        if (owner != null) {
            return owner;
        }
        UUID ownerUuid = tameable.getOwnerUuid();
        if (ownerUuid != null && entity.getWorld() instanceof ServerWorld world
                && world.getEntity(ownerUuid) instanceof LivingEntity ownerLiving) {
            return ownerLiving;
        }
        return null;
    }

    public static boolean isFriendlyTo(LivingEntity livingEntity, LivingEntity otherEntity) {
        if (livingEntity == null || otherEntity == null) {
            return false;
        }
        return !checkFriendlyFire(livingEntity, otherEntity);
    }

    public static boolean isOffensiveTargetCandidate(LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        return target instanceof HostileEntity
                || target instanceof PlayerEntity
                || isTargetWhitelisted(target);
    }

    public static boolean isOffensiveTargetCandidate(LivingEntity target, @Nullable LivingEntity attacker) {
        if (attacker != null && isMonsterFaction(attacker)) {
            if (target == null || !target.isAlive() || target.isRemoved()) {
                return false;
            }
            return !isMonsterFaction(target) && checkFriendlyFire(target, attacker);
        }
        if (!isOffensiveTargetCandidate(target)) {
            return false;
        }
        return attacker == null || checkFriendlyFire(target, attacker);
    }

    public static boolean checkEntityBlacklist(LivingEntity livingEntity, LivingEntity attackingEntity) {
        if (livingEntity == null || attackingEntity == null) {
            return false;
        }
        return livingEntity.isAlive() && attackingEntity.isAlive() && !livingEntity.isRemoved() && !attackingEntity.isRemoved();
    }

    public static boolean applyDamage(ServerWorld world, @Nullable Entity attackingEntity, LivingEntity target, float amount, boolean ignoreIframe) {
        return applyDamage(world, attackingEntity, target, amount, ignoreIframe, true);
    }

    public static boolean applyDamage(ServerWorld world, @Nullable Entity attackingEntity, LivingEntity target, float amount, boolean ignoreIframe, boolean applyKnockback) {
        if (world == null || target == null || amount <= 0.0F || !target.isAlive()) {
            return false;
        }

        if (attackingEntity instanceof LivingEntity attackerLiving && !checkFriendlyFire(target, attackerLiving)) {
            return false;
        }

        if (ignoreIframe) {
            target.hurtTime = 0;
            target.timeUntilRegen = 0;
        }

        Vec3d velocityBeforeDamage = applyKnockback ? null : target.getVelocity();
        float adjustedAmount = amount + getRangedWeaponDamageBonus(attackingEntity, "ability");
        adjustedAmount = applyNonPlayerAbilityDamageModifiers(attackingEntity, target, adjustedAmount);
        boolean damaged;
        if (attackingEntity instanceof PlayerEntity playerEntity) {
            damaged = target.damage(world.getDamageSources().playerAttack(playerEntity), adjustedAmount);
        } else if (attackingEntity instanceof LivingEntity attackerLiving) {
            damaged = target.damage(world.getDamageSources().mobAttack(attackerLiving), adjustedAmount);
        } else {
            damaged = target.damage(world.getDamageSources().magic(), adjustedAmount);
        }

        if (ignoreIframe) {
            target.hurtTime = 0;
            target.timeUntilRegen = 0;
        }
        if (!applyKnockback && damaged && velocityBeforeDamage != null) {
            target.setVelocity(velocityBeforeDamage);
        }

        return damaged;
    }

    public static float applyNonPlayerAbilityDamageModifiers(@Nullable Entity attackingEntity, LivingEntity target, float amount) {
        if (!(attackingEntity instanceof LivingEntity) || attackingEntity instanceof PlayerEntity) {
            return amount;
        }
        double modifier = SimplyBowsConfig.INSTANCE.general.nonPlayerBowAbilityDamageModifier.get();
        if (target instanceof PlayerEntity) {
            modifier *= SimplyBowsConfig.INSTANCE.general.nonPlayerBowDamageToPlayersModifier.get();
        }
        return (float) Math.max(0.0, amount * modifier);
    }

    public static float applyNonPlayerProjectileDamageModifier(@Nullable LivingEntity shooter, float damage) {
        if (shooter == null || shooter instanceof PlayerEntity) {
            return damage;
        }
        return (float) Math.max(0.0, damage * SimplyBowsConfig.INSTANCE.general.nonPlayerBowProjectileDamageModifier.get());
    }

    public static float getRangedWeaponDamageBonus(@Nullable Entity attackingEntity) {
        return getRangedWeaponDamageBonus(attackingEntity, "projectile");
    }

    public static float getRangedWeaponDamageBonus(@Nullable Entity attackingEntity, String sourceType) {
        if (!(attackingEntity instanceof LivingEntity attackerLiving)) {
            return 0.0F;
        }

        double configMultiplier = SimplyBowsConfig.INSTANCE.general.rangedWeaponApiDamageMultiplier.get();
        if (configMultiplier <= 0.0) {
            return 0.0F;
        }

        EntityAttribute attribute = Registries.ATTRIBUTE.get(RANGED_WEAPON_DAMAGE_ATTRIBUTE_ID);
        if (attribute == null) {
            return 0.0F;
        }

        if (attackerLiving.getAttributeInstance(attribute) == null) {
            return 0.0F;
        }

        double attributeValue = attackerLiving.getAttributeValue(attribute);
        float bonus = (float) (attributeValue * configMultiplier);
        if (bonus > 0.0F && SimplyBows.debugMode()) {
            SimplyBows.LOGGER.info(
                    "Applied ranged_weapon:damage bonus to bow {} damage: attacker={}, attributeValue={}, configMultiplier={}, bonus={}",
                    sourceType,
                    attackerLiving.getName().getString(),
                    attributeValue,
                    configMultiplier,
                    bonus
            );
        }
        return bonus;
    }

    public static float applyHealing(@Nullable LivingEntity sourceEntity, LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F || !target.isAlive()) {
            return 0.0F;
        }
        if (sourceEntity != null && !isFriendlyTo(target, sourceEntity)) {
            return 0.0F;
        }

        float before = target.getHealth();
        target.heal(amount);
        return Math.max(0.0F, target.getHealth() - before);
    }

    public static boolean isOpacLoaded() {
        return Platform.isModLoaded("openpartiesandclaims");
    }

    public static void addTargetWhitelist(String targetId) {
        String normalized = normalizeId(targetId);
        if (normalized != null) {
            TARGET_WHITELIST.add(normalized);
        }
    }

    public static boolean isTargetWhitelisted(LivingEntity target) {
        if (target == null) {
            return false;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(target.getType());
        if (id == null) {
            return false;
        }
        String fullId = normalizeId(id.toString());
        String path = normalizeId(id.getPath());
        return (fullId != null && TARGET_WHITELIST.contains(fullId))
                || (path != null && TARGET_WHITELIST.contains(path));
    }

    private static String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
