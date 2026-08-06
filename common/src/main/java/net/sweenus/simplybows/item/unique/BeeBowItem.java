package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.BeeChaosHoneyStormManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class BeeBowItem extends SimplyBowItem {

    private static final ThreadLocal<Boolean> CHAOS_HONEY_STORM_ON_IMPACT = ThreadLocal.withInitial(() -> false);

    public BeeBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "bee";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        float speed = f * SimplyBowsConfig.INSTANCE.beeBow.arrowSpeedMultiplier.get();
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        boolean chaosStormReady = upgrades.runeEtching() == RuneEtching.CHAOS
                && ownerId != null
                && BeeChaosHoneyStormManager.isStormReady(serverWorld, ownerId);

        if (chaosStormReady) {
            int durationTicks = Math.max(20,
                    SimplyBowsConfig.INSTANCE.beeBow.chaosBaseDurationTicks.get()
                            + Math.max(0, upgrades.stringLevel()) * SimplyBowsConfig.INSTANCE.beeBow.chaosDurationPerStringTicks.get());
            int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.beeBow.chaosCooldownTicks.get());
            simplybows$startAbilityItemCooldown(shooter, durationTicks + cooldownTicks);
        }

        CHAOS_HONEY_STORM_ON_IMPACT.set(chaosStormReady);
        try {
            if (upgrades.runeEtching() == RuneEtching.PAIN) {
                int quantity = Math.max(1, upgrades.stringLevel() + 1);
                this.shootFan(this, serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.beeBow.arrowDivergence.get(), critical, target, quantity);
                return;
            }
            this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.beeBow.arrowDivergence.get(), critical, target);
        } finally {
            CHAOS_HONEY_STORM_ON_IMPACT.set(false);
        }
    }

    @Override
    protected ProjectileEntity createArrow(World world, LivingEntity shooter, ItemStack arrowStack) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        ItemStack weaponStack = shooter.getActiveItem();
        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        BeeArrowEntity arrowEntity = new BeeArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.beeBow.baseDamage.get() * upgrades.damageMultiplier());
        arrowEntity.setChaosHoneyStormOnImpact(CHAOS_HONEY_STORM_ON_IMPACT.get());
        return arrowEntity;
    }
}
