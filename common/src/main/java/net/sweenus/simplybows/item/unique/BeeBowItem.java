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
            int durationTicks = Math.max(100, Math.min(160,
                    SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseDurationTicks.get()
                            + Math.max(0, upgrades.stringLevel()) * Math.min(8, SimplyBowsConfig.INSTANCE.buzzkill.chaosDurationPerStringTicks.get())));
            simplybows$startAbilityItemCooldown(shooter, RuneUseCooldown.fromEffectDuration(durationTicks));
        }

        CHAOS_HONEY_STORM_ON_IMPACT.set(chaosStormReady);
        try {
            if (upgrades.runeEtching() == RuneEtching.PAIN
                    && RuneUseCooldown.isReady(serverWorld, ownerId, "bee-pain")) {
                BeeArrowEntity.setPainHoming(true, 1.0F);
                try {
                    this.shootAll(serverWorld, shooter, hand, stack, projectiles, Math.min(speed, 1.2F), SimplyBowsConfig.INSTANCE.buzzkill.arrowDivergence.get(), critical, target);
                } finally { BeeArrowEntity.setPainHoming(false); }
                // Buzzkill's shared cooldown applies its usual 3x multiplier.
                RuneUseCooldown.start(serverWorld, ownerId, "bee-pain", "bee", 120);
                return;
            }
            this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.buzzkill.arrowDivergence.get(), critical, target);
        } finally {
            CHAOS_HONEY_STORM_ON_IMPACT.set(false);
        }
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        if (simplybows$isForcingVanillaArrow()) {
            return super.createArrowEntity(world, shooter, weaponStack, arrowStack, critical);
        }
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        BeeArrowEntity arrowEntity = new BeeArrowEntity(world, shooter, firedArrowStack, weaponStack);
        double damage = SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get() * upgrades.damageMultiplier();
        damage *= BeeArrowEntity.getPainDamageScale();
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            damage *= 0.5;
        }
        arrowEntity.setDamage(damage);
        arrowEntity.setChaosHoneyStormOnImpact(CHAOS_HONEY_STORM_ON_IMPACT.get());
        // Full draw still enables poison; Bounty does not get vanilla crit damage.
        arrowEntity.setFullyDrawnShot(critical);
        arrowEntity.setCritical(critical && upgrades.runeEtching() != RuneEtching.BOUNTY);
        return arrowEntity;
    }
}
