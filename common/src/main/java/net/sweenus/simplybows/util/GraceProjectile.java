package net.sweenus.simplybows.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.entity.BlossomArrowEntity;
import net.sweenus.simplybows.entity.BubbleArrowEntity;
import net.sweenus.simplybows.entity.EarthArrowEntity;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.entity.VineArrowEntity;
import net.sweenus.simplybows.upgrade.RuneEtching;

/**
 * Shared Grace-rune helpers: support shots must never hurt players (and similar friendlies).
 */
public final class GraceProjectile {

    private GraceProjectile() {
    }

    public static boolean isSupportTarget(LivingEntity living) {
        return living instanceof PlayerEntity
                || living instanceof AnimalEntity
                || living instanceof IronGolemEntity
                || living instanceof VillagerEntity;
    }

    public static boolean isGraceSupportSource(Entity source) {
        if (source instanceof VineArrowEntity) {
            return true;
        }
        if (source instanceof BeeArrowEntity bee) {
            return bee.isGraceSupportProjectile();
        }
        if (source instanceof BlossomArrowEntity blossom) {
            return blossom.isGraceSupportProjectile();
        }
        if (source instanceof EarthArrowEntity earth) {
            return earth.isGraceSupportProjectile();
        }
        if (source instanceof BubbleArrowEntity bubble) {
            return bubble.isGraceSupportProjectile();
        }
        if (source instanceof HomingArrowEntity homing) {
            return homing.isGraceSupportProjectile();
        }
        if (source instanceof HomingSpectralArrowEntity spectral) {
            return spectral.isGraceSupportProjectile();
        }
        return false;
    }

    public static boolean shouldCancelDamage(Entity source, LivingEntity victim) {
        return isGraceSupportSource(source) && isSupportTarget(victim);
    }

    public static boolean isGraceRune(RuneEtching rune) {
        return rune == RuneEtching.GRACE;
    }
}
