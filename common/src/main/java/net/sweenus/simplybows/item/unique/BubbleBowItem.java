package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.BubbleArrowEntity;
import net.sweenus.simplybows.entity.BubblePainArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.util.HelperMethods;
import net.sweenus.simplybows.world.BubbleChaosWaveManager;
import net.sweenus.simplybows.world.RuneUseCooldown;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BubbleBowItem extends SimplyBowItem {

    private static final ThreadLocal<Boolean> FORCE_DEFAULT_BUBBLE_ARROW = ThreadLocal.withInitial(() -> false);
    private static final double BUBBLE_PAIN_LINE_SPACING = 0.42;
    private static final double BUBBLE_PAIN_LINE_FORWARD_OFFSET = 0.45;

    public BubbleBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "bubble";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        if (upgrades.runeEtching() == RuneEtching.CHAOS && critical
                && RuneUseCooldown.isReady(serverWorld, ownerId, "bubble-chaos")) {
            int steps = BubbleChaosWaveManager.waveStepCount(upgrades);
            int stepInterval = Math.max(1, SimplyBowsConfig.INSTANCE.bubbleveil.chaosWaveStepIntervalTicks.get());
            BubbleChaosWaveManager.cast(serverWorld, shooter, upgrades);
            int effectTicks = Math.max(1, steps) * stepInterval;
            int cooldownTicks = Math.max(1, (int) Math.round(RuneUseCooldown.fromEffectDuration(effectTicks) * 2.5));
            RuneUseCooldown.start(serverWorld, ownerId, "bubble-chaos", "bubble", cooldownTicks);
            ItemStack ammoReference = projectiles.isEmpty() ? ItemStack.EMPTY : projectiles.getFirst();
            stack.damage(this.getWeaponStackDamage(ammoReference), shooter, LivingEntity.getSlotForHand(hand));
            return;
        }

        float speed = f * SimplyBowsConfig.INSTANCE.bubbleveil.arrowSpeedMultiplier.get();
        if (upgrades.runeEtching() == RuneEtching.PAIN && critical
                && RuneUseCooldown.isReady(serverWorld, ownerId, "bubble-pain")) {
            float painShotSpeed = shooter.isTouchingWater()
                    ? SimplyBowsConfig.INSTANCE.bubbleveil.painShotSpeedWater.get()
                    : SimplyBowsConfig.INSTANCE.bubbleveil.painShotSpeedLand.get();
            this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * painShotSpeed, SimplyBowsConfig.INSTANCE.bubbleveil.painDivergence.get(), critical, target);
            RuneUseCooldown.start(serverWorld, ownerId, "bubble-pain", "bubble", net.sweenus.simplybows.util.RuneEffectRules.cooldown(3, upgrades.stringLevel()));
            return;
        } else if (upgrades.runeEtching() == RuneEtching.PAIN) {
            FORCE_DEFAULT_BUBBLE_ARROW.set(true);
            try {
                this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.bubbleveil.arrowDivergence.get(), critical, target);
            } finally {
                FORCE_DEFAULT_BUBBLE_ARROW.set(false);
            }
            return;
        }
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.bubbleveil.arrowDivergence.get(), critical, target);
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
        ProjectileEntity arrowEntity;
        if (upgrades.runeEtching() == RuneEtching.PAIN && !FORCE_DEFAULT_BUBBLE_ARROW.get()) {
            arrowEntity = new BubblePainArrowEntity(world, shooter, firedArrowStack, weaponStack);
        } else {
            arrowEntity = new BubbleArrowEntity(world, shooter, firedArrowStack, weaponStack);
        }
        if (arrowEntity instanceof net.minecraft.entity.projectile.PersistentProjectileEntity persistent) {
            double damage = SimplyBowsConfig.INSTANCE.bubbleveil.baseDamage.get()
                    * (1.0 + upgrades.frameLevel() * SimplyBowsConfig.INSTANCE.upgrades.damageMultiplierPerFrame.get() * 0.5);
            persistent.setDamage(damage);
            persistent.setCritical(critical);
        }
        return arrowEntity;
    }

}
