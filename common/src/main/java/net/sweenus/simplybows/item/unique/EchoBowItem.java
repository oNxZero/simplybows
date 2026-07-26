package net.sweenus.simplybows.item.unique;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.EchoArrowEntity;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import net.sweenus.simplybows.world.EchoChaosBlackHoleManager;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EchoBowItem extends SimplyBowItem {

    private static final ThreadLocal<Boolean> CHAOS_BLACK_HOLE_ON_IMPACT = ThreadLocal.withInitial(() -> false);

    public EchoBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "echo";
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            ItemStack offhandStack = user.getOffHandStack();
            if (isValidDrinkablePotion(offhandStack)) {
                user.setCurrentHand(Hand.OFF_HAND);
                return TypedActionResult.consume(user.getStackInHand(Hand.MAIN_HAND));
            }
        }
        return super.use(world, user, hand);
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        boolean chaosBlackHoleReady = upgrades.runeEtching() == RuneEtching.CHAOS
                && ownerId != null
                && EchoChaosBlackHoleManager.isBlackHoleReady(serverWorld, ownerId);

        if (chaosBlackHoleReady) {
            int durationTicks = Math.max(20,
                    SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleDurationTicks.get()
                            + Math.max(0, upgrades.stringLevel()) * SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleDurationPerStringTicks.get());
            int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleCooldownTicks.get());
            simplybows$startAbilityItemCooldown(shooter, durationTicks + cooldownTicks);
        }

        CHAOS_BLACK_HOLE_ON_IMPACT.set(chaosBlackHoleReady);
        try {
            this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * SimplyBowsConfig.INSTANCE.echoBow.arrowSpeedMultiplier.get(), SimplyBowsConfig.INSTANCE.echoBow.arrowDivergence.get(), critical, target);
        } finally {
            CHAOS_BLACK_HOLE_ON_IMPACT.set(false);
        }

        EchoShoulderBowManager.onOwnerFired(shooter);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        EchoArrowEntity arrowEntity = new EchoArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.echoBow.baseDamage.get());
        arrowEntity.setChaosBlackHoleOnImpact(CHAOS_BLACK_HOLE_ON_IMPACT.get());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }

    private static boolean isValidDrinkablePotion(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) {
            return false;
        }
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {
            return false;
        }
        final boolean[] hasEffects = {false};
        potionContents.forEachEffect(effect -> hasEffects[0] = true);
        return hasEffects[0];
    }
}
