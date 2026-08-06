package net.sweenus.simplybows.util;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Ammo handling for any LivingEntity bow wielder. Players keep their existing
 * inventory/creative/Infinity behavior; non-players shoot like vanilla skeletons
 * (a default arrow, never consumed).
 */
public final class BowUser {

    private BowUser() {
    }

    public static ItemStack getProjectile(LivingEntity shooter, ItemStack bowStack) {
        ItemStack projectileStack = shooter.getProjectileType(bowStack);
        if (projectileStack.isEmpty()) {
            if (shooter instanceof PlayerEntity player) {
                if (player.getAbilities().creativeMode) {
                    projectileStack = new ItemStack(Items.ARROW);
                }
            } else {
                projectileStack = new ItemStack(Items.ARROW);
            }
        }
        return projectileStack;
    }

    public static boolean hasInfiniteAmmo(LivingEntity shooter, ItemStack bowStack, ItemStack projectileStack) {
        if (!(shooter instanceof PlayerEntity player)) {
            return true;
        }
        if (player.getAbilities().creativeMode) {
            return true;
        }
        if (projectileStack == null || projectileStack.isEmpty() || !projectileStack.isOf(Items.ARROW)) {
            return false;
        }
        // 1.20.1 enchantments are plain objects, not registry entries.
        return EnchantmentHelper.getLevel(Enchantments.INFINITY, bowStack) > 0;
    }

    public static ExtraArrowSupply extraArrows(LivingEntity shooter, ItemStack bowStack, int additionalArrowsNeeded) {
        if (shooter instanceof ServerPlayerEntity player
                && !hasInfiniteAmmo(player, bowStack, player.getProjectileType(bowStack))) {
            Map<ItemStack, Integer> arrowStacks = HelperMethods.findArrowStacks(player);
            List<ItemStack> usableArrows = HelperMethods.collectArrows(arrowStacks, additionalArrowsNeeded);
            return new ExtraArrowSupply(usableArrows, additionalArrowsNeeded);
        }
        return new ExtraArrowSupply(null, additionalArrowsNeeded);
    }

    /**
     * Supplies the arrows for the extra shots of fan/line volleys beyond the first,
     * pre-consumed one: infinite copies for creative players, Infinity bows, and all
     * non-players; real inventory arrows otherwise.
     */
    public static final class ExtraArrowSupply {
        @Nullable
        private final List<ItemStack> usableArrows;
        private final int maxAdditional;
        private int consumed;

        private ExtraArrowSupply(@Nullable List<ItemStack> usableArrows, int maxAdditional) {
            this.usableArrows = usableArrows;
            this.maxAdditional = maxAdditional;
        }

        @Nullable
        public ItemStack next(ItemStack template) {
            if (usableArrows == null) {
                ItemStack copy = template.copy();
                copy.setCount(1);
                return copy;
            }
            if (consumed >= maxAdditional || usableArrows.isEmpty()) {
                return null;
            }
            ItemStack arrow = HelperMethods.consumeNextArrow(usableArrows);
            if (arrow == null || arrow.isEmpty()) {
                return null;
            }
            consumed++;
            return arrow;
        }
    }
}
