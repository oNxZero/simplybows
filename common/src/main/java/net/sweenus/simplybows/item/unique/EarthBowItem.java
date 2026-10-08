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
import net.sweenus.simplybows.entity.EarthArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.EarthChaosSunderManager;
import net.sweenus.simplybows.world.RuneUseCooldown;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EarthBowItem extends SimplyBowItem {

    private static final ThreadLocal<Boolean> CHAOS_SUNDER_ON_IMPACT = ThreadLocal.withInitial(() -> false);

    public EarthBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "earth";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        float speed = (float) (f * SimplyBowsConfig.INSTANCE.tremorstrike.arrowSpeedMultiplier.get() * (1.0 + upgrades.stringLevel() * 0.05));
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        boolean chaosSunderReady = upgrades.runeEtching() == RuneEtching.CHAOS
                && ownerId != null
                && EarthChaosSunderManager.isSunderReady(serverWorld, ownerId);

        if (upgrades.runeEtching() == RuneEtching.CHAOS && ownerId != null) {
            int durationTicks = Math.max(200, Math.min(360,
                    Math.max(200, SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationTicks.get())
                            + Math.max(0, upgrades.stringLevel()) * Math.max(20, SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationPerStringTicks.get())));
            if (chaosSunderReady) {
                simplybows$startAbilityItemCooldown(shooter, RuneUseCooldown.fromEffectDuration(durationTicks));
            }
        }

        CHAOS_SUNDER_ON_IMPACT.set(chaosSunderReady);
        try {
            this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.tremorstrike.arrowDivergence.get(), critical, target);
        } finally {
            CHAOS_SUNDER_ON_IMPACT.set(false);
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
        EarthArrowEntity arrowEntity = new EarthArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.tremorstrike.baseDamage.get() * upgrades.damageMultiplier());
        arrowEntity.setChaosSunderOnImpact(CHAOS_SUNDER_ON_IMPACT.get());
        //arrowEntity.setPunch(upgrades.bonusKnockback());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
