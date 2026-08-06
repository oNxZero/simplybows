package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.EquipmentSlot;
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
import net.sweenus.simplybows.util.BowUser;
import net.sweenus.simplybows.util.HelperMethods;
import net.sweenus.simplybows.world.BubbleChaosWaveManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
        if (upgrades.runeEtching() == RuneEtching.CHAOS && critical) {
            BubbleChaosWaveManager.cast(serverWorld, shooter, upgrades);
            EquipmentSlot bowSlot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.damage(1, shooter, e -> e.sendEquipmentBreakStatus(bowSlot));
            return;
        }

        float speed = f * SimplyBowsConfig.INSTANCE.bubbleBow.arrowSpeedMultiplier.get();
        if (upgrades.runeEtching() == RuneEtching.PAIN && critical) {
            float painSpeedMultiplier = shooter.isTouchingWater() ? SimplyBowsConfig.INSTANCE.bubbleBow.painSpeedMultiplierWater.get() : SimplyBowsConfig.INSTANCE.bubbleBow.painSpeedMultiplierLand.get();
            int quantity = Math.max(1, upgrades.stringLevel() + 1);
            this.shootLine(serverWorld, shooter, hand, stack, projectiles, f * painSpeedMultiplier, critical, target, quantity);
            return;
        } else if (upgrades.runeEtching() == RuneEtching.PAIN) {
            FORCE_DEFAULT_BUBBLE_ARROW.set(true);
            try {
                this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.bubbleBow.arrowDivergence.get(), critical, target);
            } finally {
                FORCE_DEFAULT_BUBBLE_ARROW.set(false);
            }
            return;
        }
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.bubbleBow.arrowDivergence.get(), critical, target);
    }

    @Override
    protected ProjectileEntity createArrow(World world, LivingEntity shooter, ItemStack arrowStack) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        ItemStack weaponStack = shooter.getActiveItem();
        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        ProjectileEntity arrowEntity;
        if (upgrades.runeEtching() == RuneEtching.PAIN && !FORCE_DEFAULT_BUBBLE_ARROW.get()) {
            arrowEntity = new BubblePainArrowEntity(world, shooter, firedArrowStack, weaponStack);
        } else {
            arrowEntity = new BubbleArrowEntity(world, shooter, firedArrowStack, weaponStack);
        }
        if (arrowEntity instanceof net.minecraft.entity.projectile.PersistentProjectileEntity persistent) {
            persistent.setDamage(SimplyBowsConfig.INSTANCE.bubbleBow.baseDamage.get());
        }
        return arrowEntity;
    }

    private void shootLine(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles,
                           float speed, boolean critical, @Nullable LivingEntity target, int quantity) {
        int additionalArrowsNeeded = Math.max(0, quantity - 1) * projectiles.size();
        BowUser.ExtraArrowSupply extraArrows = BowUser.extraArrows(shooter, stack, additionalArrowsNeeded);
        Vec3d forward = shooter.getRotationVec(1.0F).normalize();
        Vec3d horizontalForward = new Vec3d(forward.x, 0.0, forward.z);
        if (horizontalForward.lengthSquared() <= 1.0E-6) {
            horizontalForward = Vec3d.fromPolar(0.0F, shooter.getYaw());
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3d right = new Vec3d(-horizontalForward.z, 0.0, horizontalForward.x).normalize();

        for (int j = 0; j < projectiles.size(); ++j) {
            for (int p = 0; p < quantity; ++p) {
                ItemStack arrowForProjectile;
                if (p == 0) {
                    arrowForProjectile = projectiles.get(j);
                } else {
                    arrowForProjectile = extraArrows.next(projectiles.get(j));
                    if (arrowForProjectile == null || arrowForProjectile.isEmpty()) {
                        break;
                    }
                }

                ProjectileEntity projectileEntity = this.createArrow(world, shooter, arrowForProjectile);
                this.simplybows$applyRangedWeaponProjectileBonus(shooter, projectileEntity);
                this.shoot(shooter, projectileEntity, j, speed, SimplyBowsConfig.INSTANCE.bubbleBow.painDivergence.get(), 0.0F, target);
                double centerOffset = (quantity - 1) * 0.5;
                double lateralOffset = (p - centerOffset) * BUBBLE_PAIN_LINE_SPACING;
                Vec3d spawnOffset = horizontalForward.multiply(BUBBLE_PAIN_LINE_FORWARD_OFFSET).add(right.multiply(lateralOffset));
                projectileEntity.setPosition(projectileEntity.getX() + spawnOffset.x, projectileEntity.getY() + spawnOffset.y, projectileEntity.getZ() + spawnOffset.z);
                world.spawnEntity(projectileEntity);

                EquipmentSlot bowSlot2 = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.damage(1, shooter, e -> e.sendEquipmentBreakStatus(bowSlot2));
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }
}
