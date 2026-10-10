package net.sweenus.simplybows.util;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.mixin.ProjectileWeaponAccessor;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;

/** Restore vanilla projectile enchantment handling without changing ability damage. */
public final class BowProjectileEnchantments {
    private BowProjectileEnchantments() {}

    public static void initialize(PersistentProjectileEntity arrow, ItemStack bow, ItemStack ammo) {
        if (bow == null || bow.isEmpty()) return;
        if (arrow.getOwner() instanceof net.minecraft.entity.LivingEntity shooter
                && BowUser.hasInfiniteAmmo(shooter, bow, ammo)) {
            arrow.pickupType = shooter instanceof net.minecraft.entity.player.PlayerEntity
                    ? PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY
                    : PersistentProjectileEntity.PickupPermission.DISALLOWED;
        }
        if (bow.getItem() instanceof IceBowItem && BowUpgradeData.from(bow).runeEtching() == RuneEtching.GRACE) return;
        ItemStack snapshot = bow.copy();
        ((ProjectileWeaponAccessor) arrow).simplybows$setWeapon(snapshot);
        if (arrow.getWorld() instanceof ServerWorld world)
            EnchantmentHelper.onProjectileSpawned(world, snapshot, arrow, ignored -> {});
        // Vanilla reads this saved weapon on impact for Power/Punch and saves it in arrow NBT.
    }
}
