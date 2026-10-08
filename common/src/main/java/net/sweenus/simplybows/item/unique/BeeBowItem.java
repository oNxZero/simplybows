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
import net.sweenus.simplybows.world.RuneUseCooldown;
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
        float speed = f * SimplyBowsConfig.INSTANCE.buzzkill.arrowSpeedMultiplier.get();
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        boolean chaosStormReady = upgrades.runeEtching() == RuneEtching.CHAOS
                && ownerId != null
                && BeeChaosHoneyStormManager.isStormReady(serverWorld, ownerId);

        if (chaosStormReady) {
            int durationTicks = Math.max(20, Math.min(280,
                    SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseDurationTicks.get()
                            + Math.max(0, upgrades.stringLevel()) * Math.min(10, SimplyBowsConfig.INSTANCE.buzzkill.chaosDurationPerStringTicks.get())));
            simplybows$startAbilityItemCooldown(shooter, RuneUseCooldown.fromEffectDuration(durationTicks));
        }

        CHAOS_HONEY_STORM_ON_IMPACT.set(chaosStormReady);
        try {
            if (upgrades.runeEtching() == RuneEtching.PAIN
                    && RuneUseCooldown.isReady(serverWorld, ownerId, "bee-pain")) {
                int quantity = Math.max(1, upgrades.stringLevel() + 1);
                // Total fan damage ≈ ~35% of one normal Buzzkill shot when every bee lands.
                float painDamageScale = 0.35F / quantity;
                float painSpeed = Math.min(speed, 0.8F);
                BeeArrowEntity.setPainHoming(true, painDamageScale);
                try {
                    this.shootFan(this, serverWorld, shooter, hand, stack, projectiles, painSpeed, SimplyBowsConfig.INSTANCE.buzzkill.arrowDivergence.get(), false, target, quantity);
                } finally {
                    BeeArrowEntity.setPainHoming(false);
                }
                RuneUseCooldown.startForEffect(serverWorld, ownerId, "bee-pain", "bee", RuneUseCooldown.BURST_EFFECT_TICKS);
                return;
            }
            this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.buzzkill.arrowDivergence.get(), critical, target);
        } finally {
            CHAOS_HONEY_STORM_ON_IMPACT.set(false);
        }
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        BeeArrowEntity arrowEntity = new BeeArrowEntity(world, shooter, firedArrowStack, weaponStack);
        double damage = SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get() * upgrades.damageMultiplier();
        damage *= BeeArrowEntity.getPainDamageScale();
        arrowEntity.setDamage(damage);
        arrowEntity.setChaosHoneyStormOnImpact(CHAOS_HONEY_STORM_ON_IMPACT.get());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
