package net.sweenus.simplybows.item.unique;

import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.HelperMethods;
import net.sweenus.simplybows.world.IceChaosWallManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IceBowItem extends SimplyBowItem {
    private static int baseQuantity() { return SimplyBowsConfig.INSTANCE.iceBow.baseQuantity.get(); }
    private static double painTargetHorizontalRange() { return SimplyBowsConfig.INSTANCE.iceBow.painTargetHorizontalRange.get(); }
    private static double painTargetVerticalRange() { return SimplyBowsConfig.INSTANCE.iceBow.painTargetVerticalRange.get(); }
    private static final String NBT_DAMAGE_MULTIPLIER = "simplybows_ice_damage_multiplier";
    private static final String NBT_LOCK_TARGET = "simplybows_ice_lock_target";
    private static final String NBT_SLOW_STACK = "simplybows_ice_stacking_slow";
    private static final String NBT_TARGET_UUID = "simplybows_ice_target_uuid";
    private static final String NBT_CHAOS_WALL_ON_IMPACT = "simplybows_ice_chaos_wall_on_impact";
    private static final String NBT_CHAOS_WALL_STRING_LEVEL = "simplybows_ice_chaos_wall_string_level";
    private static final String NBT_CHAOS_WALL_FRAME_LEVEL = "simplybows_ice_chaos_wall_frame_level";

    public IceBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "ice";
    }


    public static void passiveParticles(LivingEntity user, ServerWorld world) {
        int random = (int) (Math.random() * 30);
        Item item = ItemRegistry.ICE_BOW.get();
        if (HelperMethods.isHoldingItem(item, user) && user.age % (5 + random) == 0) {
            HelperMethods.spawnParticlesAtItem(world, user, item, ParticleTypes.SNOWFLAKE, 1);
            HelperMethods.spawnParticlesAtItem(world, user, item, ParticleTypes.WHITE_ASH, 3);
        }
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> list, float f, float g, boolean bl, @Nullable LivingEntity livingEntity) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        RuneEtching rune = upgrades.runeEtching();
        boolean chaosWallReady = rune == RuneEtching.CHAOS && IceChaosWallManager.isWallReady(serverWorld, shooter.getUuid());

        if (chaosWallReady) {
            int durationTicks = Math.max(20,
                    SimplyBowsConfig.INSTANCE.iceBow.chaosWallDurationTicks.get()
                            + Math.max(0, upgrades.frameLevel()) * SimplyBowsConfig.INSTANCE.iceBow.chaosWallDurationPerFrameTicks.get());
            int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.iceBow.chaosWallCooldownTicks.get());
            simplybows$startAbilityItemCooldown(shooter, durationTicks + cooldownTicks);
        }

        int quantity = getArrowQuantity(upgrades);
        double damageMultiplier = upgrades.damageMultiplier();
        if (rune == RuneEtching.PAIN) {
            damageMultiplier *= SimplyBowsConfig.INSTANCE.iceBow.painDamageMultiplier.get();
        } else if (rune == RuneEtching.BOUNTY) {
            damageMultiplier *= SimplyBowsConfig.INSTANCE.iceBow.bountyDamageMultiplier.get();
        }

        LivingEntity painTarget = null;
        if (rune == RuneEtching.PAIN) {
            painTarget = livingEntity != null && CombatTargeting.isOffensiveTargetCandidate(livingEntity, shooter)
                    ? livingEntity
                    : findNearestHostile(serverWorld, shooter);
        }

        NbtCompound customData = stack.getOrCreateNbt();
        customData.putDouble(NBT_DAMAGE_MULTIPLIER, damageMultiplier);
        // Only hard-lock when pain mode found a concrete target.
        // If no target is found, keep normal homing fallback behavior.
        customData.putBoolean(NBT_LOCK_TARGET, rune == RuneEtching.PAIN && painTarget != null);
        customData.putBoolean(NBT_SLOW_STACK, rune == RuneEtching.GRACE);
        customData.putBoolean(NBT_CHAOS_WALL_ON_IMPACT, chaosWallReady);
        if (chaosWallReady) {
            customData.putInt(NBT_CHAOS_WALL_STRING_LEVEL, upgrades.stringLevel());
            customData.putInt(NBT_CHAOS_WALL_FRAME_LEVEL, upgrades.frameLevel());
        } else {
            customData.remove(NBT_CHAOS_WALL_STRING_LEVEL);
            customData.remove(NBT_CHAOS_WALL_FRAME_LEVEL);
        }
        if (painTarget != null) {
            customData.putUuid(NBT_TARGET_UUID, painTarget.getUuid());
        } else {
            customData.remove(NBT_TARGET_UUID);
        }

        if (chaosWallReady) {
            this.shootAll(serverWorld, shooter, hand, stack, list, f * SimplyBowsConfig.INSTANCE.iceBow.arrowSpeed.get(), SimplyBowsConfig.INSTANCE.iceBow.chaosWallArrowDivergence.get() * 0.01F, f == 1.0F, livingEntity);
        } else {
            this.shootFan(this, serverWorld, shooter, hand, stack, list, f * SimplyBowsConfig.INSTANCE.iceBow.arrowSpeed.get(), SimplyBowsConfig.INSTANCE.iceBow.arrowDivergence.get(), f == 1.0F, livingEntity, quantity);
        }
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, shooter, ParticleTypes.SNOWFLAKE, 6);
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, shooter, ParticleTypes.WHITE_ASH, 8);

    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (simplybows$hasInfiniteAmmo(user, itemStack, user.getProjectileType(itemStack))) {
            //return super.use(world, user, hand);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(itemStack);
        int quantity = getArrowQuantity(upgrades);
        Map<ItemStack, Integer> arrowStacks = HelperMethods.findArrowStacks(user);

        if (upgrades.runeEtching() == RuneEtching.CHAOS) {
            for (Map.Entry<ItemStack, Integer> entry : arrowStacks.entrySet()) {
                if (entry.getValue() > 0) {
                    return super.use(world, user, hand);
                }
            }
            return TypedActionResult.fail(itemStack);
        }
        for (Map.Entry<ItemStack, Integer> entry : arrowStacks.entrySet()) {
            ItemStack arrowStack = entry.getKey();
            int arrowCount = entry.getValue();
            if (arrowCount > quantity - 1)
                return super.use(world, user, hand);
        }
        return TypedActionResult.fail(itemStack);
    }

    @Override
    protected ProjectileEntity createArrow(World world, LivingEntity shooter, ItemStack arrowStack) {
        if (simplybows$isForcingVanillaArrow()) {
            return super.createArrow(world, shooter, arrowStack);
        }

        double damageMultiplier = 1.0;
        boolean lockTarget = false;
        boolean stackSlow = false;
        boolean chaosWallOnImpact = false;
        int chaosWallStringLevel = 0;
        int chaosWallFrameLevel = 0;
        UUID targetUuid = null;
        // Read temp data written to bow stack in performStoppedUsing
        ItemStack weaponStack = shooter.getActiveItem();
        NbtCompound nbt = weaponStack.getNbt();
        if (nbt != null) {
            damageMultiplier = nbt.getDouble(NBT_DAMAGE_MULTIPLIER);
            if (damageMultiplier <= 0.0) {
                damageMultiplier = 1.0;
            }
            lockTarget = nbt.getBoolean(NBT_LOCK_TARGET);
            stackSlow = nbt.getBoolean(NBT_SLOW_STACK);
            chaosWallOnImpact = nbt.getBoolean(NBT_CHAOS_WALL_ON_IMPACT);
            chaosWallStringLevel = nbt.getInt(NBT_CHAOS_WALL_STRING_LEVEL);
            chaosWallFrameLevel = nbt.getInt(NBT_CHAOS_WALL_FRAME_LEVEL);
            if (nbt.containsUuid(NBT_TARGET_UUID)) {
                targetUuid = nbt.getUuid(NBT_TARGET_UUID);
            }
        }

        ProjectileEntity arrowEntity;
        if (arrowStack.isOf(Items.SPECTRAL_ARROW)) {
            HomingSpectralArrowEntity spectralArrow = new HomingSpectralArrowEntity(world, shooter, arrowStack, weaponStack);
            spectralArrow.setDamage(SimplyBowsConfig.INSTANCE.iceBow.baseDamage.get() * damageMultiplier);
            spectralArrow.setLockSingleTarget(lockTarget);
            spectralArrow.setStackingSlowness(stackSlow);
            if (targetUuid != null) {
                spectralArrow.setLockedTargetUuid(targetUuid);
            }
            spectralArrow.setChaosWallOnImpact(chaosWallOnImpact);
            if (chaosWallOnImpact) {
                spectralArrow.setHomingEnabled(false);
                spectralArrow.setChaosWallUpgradeLevels(chaosWallStringLevel, chaosWallFrameLevel);
            }
            arrowEntity = spectralArrow;
        } else {
            HomingArrowEntity homingArrow = new HomingArrowEntity(world, shooter, arrowStack, weaponStack);
            homingArrow.setDamage(SimplyBowsConfig.INSTANCE.iceBow.baseDamage.get() * damageMultiplier);
            homingArrow.setLockSingleTarget(lockTarget);
            homingArrow.setStackingSlowness(stackSlow);
            if (targetUuid != null) {
                homingArrow.setLockedTargetUuid(targetUuid);
            }
            homingArrow.setChaosWallOnImpact(chaosWallOnImpact);
            if (chaosWallOnImpact) {
                homingArrow.setHomingEnabled(false);
                homingArrow.setChaosWallUpgradeLevels(chaosWallStringLevel, chaosWallFrameLevel);
            }
            arrowEntity = homingArrow;
        }
        return arrowEntity;
    }

    private int getArrowQuantity(BowUpgradeData upgrades) {
        int quantity = baseQuantity() + upgrades.stringLevel();
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            quantity *= SimplyBowsConfig.INSTANCE.iceBow.bountyExtraArrowMultiplier.get();
        }
        return quantity;
    }

    private LivingEntity findNearestHostile(ServerWorld world, LivingEntity shooter) {
        List<LivingEntity> hostiles = world.getEntitiesByClass(
                LivingEntity.class,
                shooter.getBoundingBox().expand(painTargetHorizontalRange(), painTargetVerticalRange(), painTargetHorizontalRange()),
                candidate -> CombatTargeting.isOffensiveTargetCandidate(candidate, shooter)
        );
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity hostile : hostiles) {
            if (!CombatTargeting.checkFriendlyFire(hostile, shooter)) {
                continue;
            }
            double dist = hostile.squaredDistanceTo(shooter);
            if (dist < bestDist) {
                bestDist = dist;
                best = hostile;
            }
        }
        return best;
    }

}
