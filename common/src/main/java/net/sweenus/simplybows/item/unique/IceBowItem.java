package net.sweenus.simplybows.item.unique;

import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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
import net.sweenus.simplybows.world.IceFrostBloomManager;
import net.sweenus.simplybows.world.RuneUseCooldown;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IceBowItem extends SimplyBowItem {
    private static final ThreadLocal<net.sweenus.simplybows.world.IcePainVolley> PAIN_VOLLEY = new ThreadLocal<>();
    private static int baseQuantity() { return SimplyBowsConfig.INSTANCE.winterfang.baseQuantity.get(); }
    private static final String NBT_DAMAGE_MULTIPLIER = "simplybows_ice_damage_multiplier";
    private static final String NBT_SLOW_STACK = "simplybows_ice_stacking_slow";
    private static final String NBT_PAIN_FROST = "simplybows_ice_pain_frost";
    private static final String NBT_BOUNTY_FROST = "simplybows_ice_bounty_frost";
    private static final String NBT_STRING_LEVEL = "simplybows_ice_string_level";
    private static final String NBT_FRAME_LEVEL = "simplybows_ice_frame_level";
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
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        boolean chaosWallReady = rune == RuneEtching.CHAOS && IceChaosWallManager.isWallReady(serverWorld, shooter.getUuid());
        boolean painReady = rune == RuneEtching.PAIN && RuneUseCooldown.isReady(serverWorld, ownerId, "ice-pain");
        boolean graceReady = rune == RuneEtching.GRACE && RuneUseCooldown.isReady(serverWorld, ownerId, "ice-grace");
        boolean bountyReady = rune == RuneEtching.BOUNTY && RuneUseCooldown.isReady(serverWorld, ownerId, "ice-bounty");

        if (chaosWallReady) {
            int durationTicks = Math.max(20,
                    SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationTicks.get()
                            + Math.max(0, upgrades.frameLevel()) * SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationPerFrameTicks.get());
            simplybows$startAbilityItemCooldown(shooter, RuneUseCooldown.fromEffectDuration(durationTicks));
        }

        int quantity = baseQuantity() + upgrades.stringLevel();
        // One arrow for rune AOEs — String must not multiply frost blooms.
        if (rune == RuneEtching.BOUNTY || rune == RuneEtching.GRACE) {
            quantity = 1;
        }
        if (painReady) quantity = net.sweenus.simplybows.util.WinterfangAbilityRules.painArrowCount(upgrades.stringLevel());
        // Soft Frame curve — global 0.55/level made headshots nuclear vs abilities.
        double damageMultiplier = net.sweenus.simplybows.util.WinterfangAbilityRules.arrowDamageMultiplier(rune, upgrades.frameLevel());

        NbtCompound customData = getOrCreateCustomData(stack);
        customData.putDouble(NBT_DAMAGE_MULTIPLIER, damageMultiplier);
        customData.putBoolean(NBT_SLOW_STACK, rune == RuneEtching.GRACE);
        customData.putBoolean("simplybows_ice_grace_ready", graceReady);
        customData.putBoolean(NBT_PAIN_FROST, painReady);
        customData.putBoolean(NBT_BOUNTY_FROST, bountyReady);
        customData.putInt(NBT_STRING_LEVEL, upgrades.stringLevel());
        customData.putInt(NBT_FRAME_LEVEL, upgrades.frameLevel());
        customData.putBoolean(NBT_CHAOS_WALL_ON_IMPACT, chaosWallReady);
        if (chaosWallReady) {
            customData.putInt(NBT_CHAOS_WALL_STRING_LEVEL, upgrades.stringLevel());
            customData.putInt(NBT_CHAOS_WALL_FRAME_LEVEL, upgrades.frameLevel());
        } else {
            customData.remove(NBT_CHAOS_WALL_STRING_LEVEL);
            customData.remove(NBT_CHAOS_WALL_FRAME_LEVEL);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        if (painReady) {
            int painCd = RuneUseCooldown.fromEffectDuration(RuneUseCooldown.BURST_EFFECT_TICKS) * 2;
            RuneUseCooldown.start(serverWorld, ownerId, "ice-pain", "ice", painCd);
        } else if (graceReady) {
            int graceCd = RuneUseCooldown.fromEffectDuration(IceFrostBloomManager.graceZoneDurationTicks()) * 2;
            RuneUseCooldown.start(serverWorld, ownerId, "ice-grace", "ice", graceCd);
        } else if (bountyReady) {
            RuneUseCooldown.start(serverWorld, ownerId, "ice-bounty", "ice", net.sweenus.simplybows.util.WinterfangAbilityRules.bountyCooldownTicks(upgrades.frameLevel()));
        }

        if (painReady) PAIN_VOLLEY.set(new net.sweenus.simplybows.world.IcePainVolley(serverWorld, shooter));
        try {
        // Never pass vanilla crit — multi-arrow + crit was nuking targets ("headshot" spikes).
        if (chaosWallReady) {
            this.shootAll(serverWorld, shooter, hand, stack, list, f * SimplyBowsConfig.INSTANCE.winterfang.arrowSpeed.get(), SimplyBowsConfig.INSTANCE.winterfang.chaosWallArrowDivergence.get() * 0.01F, false, livingEntity);
        } else if (rune == RuneEtching.BOUNTY || rune == RuneEtching.GRACE) {
            this.shootAll(serverWorld, shooter, hand, stack, list, f * SimplyBowsConfig.INSTANCE.winterfang.arrowSpeed.get(), 0.0F, false, livingEntity);
        } else {
            this.shootFan(this, serverWorld, shooter, hand, stack, list, f * SimplyBowsConfig.INSTANCE.winterfang.arrowSpeed.get(), SimplyBowsConfig.INSTANCE.winterfang.arrowDivergence.get(), false, livingEntity, quantity);
        }
        } finally { PAIN_VOLLEY.remove(); }
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, shooter, ParticleTypes.SNOWFLAKE, 6);
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, shooter, ParticleTypes.WHITE_ASH, 8);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (simplybows$hasInfiniteAmmo(user, itemStack, user.getProjectileType(itemStack))) {
            return super.use(world, user, hand);
        }
        BowUpgradeData upgrades = BowUpgradeData.from(itemStack);
        int quantity = getArrowQuantity(upgrades, user);
        int arrows = HelperMethods.findArrowStacks(user).values().stream().mapToInt(Integer::intValue).sum();
        if (arrows >= quantity) return super.use(world, user, hand);
        return TypedActionResult.fail(itemStack);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        if (simplybows$isForcingVanillaArrow()) {
            return super.createArrowEntity(world, shooter, weaponStack, arrowStack, critical);
        }

        double damageMultiplier = 1.0;
        boolean stackSlow = false;
        boolean painFrost = false;
        boolean bountyFrost = false;
        int stringLevel = 0;
        int frameLevel = 0;
        boolean chaosWallOnImpact = false;
        int chaosWallStringLevel = 0;
        int chaosWallFrameLevel = 0;
        NbtComponent customData = weaponStack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            damageMultiplier = nbt.getDouble(NBT_DAMAGE_MULTIPLIER);
            if (damageMultiplier <= 0.0) {
                damageMultiplier = 1.0;
            }
            stackSlow = nbt.getBoolean(NBT_SLOW_STACK);
            painFrost = nbt.getBoolean(NBT_PAIN_FROST);
            bountyFrost = nbt.getBoolean(NBT_BOUNTY_FROST);
            stringLevel = nbt.getInt(NBT_STRING_LEVEL);
            frameLevel = nbt.getInt(NBT_FRAME_LEVEL);
            chaosWallOnImpact = nbt.getBoolean(NBT_CHAOS_WALL_ON_IMPACT);
            chaosWallStringLevel = nbt.getInt(NBT_CHAOS_WALL_STRING_LEVEL);
            chaosWallFrameLevel = nbt.getInt(NBT_CHAOS_WALL_FRAME_LEVEL);
        }

        ProjectileEntity arrowEntity;
        if (arrowStack.isOf(Items.SPECTRAL_ARROW)) {
            HomingSpectralArrowEntity spectralArrow = new HomingSpectralArrowEntity(world, shooter, arrowStack, weaponStack);
            double damage = SimplyBowsConfig.INSTANCE.winterfang.baseDamage.get() * damageMultiplier;
            if (stackSlow) {
                damage = 0;
            }
            spectralArrow.setDamage(damage);
            spectralArrow.setStackingSlowness(stackSlow);
            spectralArrow.setFrostStringLevel(stringLevel);
            spectralArrow.setPainFrostBloom(painFrost, stringLevel);
            spectralArrow.setBountyFrostBloom(bountyFrost, stringLevel, frameLevel);
            spectralArrow.setChaosWallOnImpact(chaosWallOnImpact);
            if (chaosWallOnImpact) {
                spectralArrow.setChaosWallUpgradeLevels(chaosWallStringLevel, chaosWallFrameLevel);
            }
            if (BowUpgradeData.from(weaponStack).runeEtching() == RuneEtching.BOUNTY) spectralArrow.setHomingEnabled(false);
            if (stackSlow) spectralArrow.setGraceSanctuaryEnabled(customData != null && customData.copyNbt().getBoolean("simplybows_ice_grace_ready"), frameLevel);
            var volley = PAIN_VOLLEY.get();
            if (painFrost && volley != null) {
                spectralArrow.setLockSingleTarget(true);
                spectralArrow.setLockedTargetUuid(volley.nextTarget());
                spectralArrow.setPainVolleyId(volley.id);
            }
            // No vanilla crit multiplier — multi-arrow + crit was nuking targets.
            spectralArrow.setCritical(false);
            arrowEntity = spectralArrow;
        } else {
            HomingArrowEntity homingArrow = new HomingArrowEntity(world, shooter, arrowStack, weaponStack);
            double damage = SimplyBowsConfig.INSTANCE.winterfang.baseDamage.get() * damageMultiplier;
            if (stackSlow) {
                damage = 0;
            }
            homingArrow.setDamage(damage);
            homingArrow.setStackingSlowness(stackSlow);
            homingArrow.setFrostStringLevel(stringLevel);
            homingArrow.setPainFrostBloom(painFrost, stringLevel);
            homingArrow.setBountyFrostBloom(bountyFrost, stringLevel, frameLevel);
            homingArrow.setChaosWallOnImpact(chaosWallOnImpact);
            if (chaosWallOnImpact) {
                homingArrow.setChaosWallUpgradeLevels(chaosWallStringLevel, chaosWallFrameLevel);
            }
            if (BowUpgradeData.from(weaponStack).runeEtching() == RuneEtching.BOUNTY) homingArrow.setHomingEnabled(false);
            if (stackSlow) homingArrow.setGraceSanctuaryEnabled(customData != null && customData.copyNbt().getBoolean("simplybows_ice_grace_ready"), frameLevel);
            var volley = PAIN_VOLLEY.get();
            if (painFrost && volley != null) {
                homingArrow.setLockSingleTarget(true);
                homingArrow.setLockedTargetUuid(volley.nextTarget());
                homingArrow.setPainVolleyId(volley.id);
            }
            homingArrow.setCritical(false);
            arrowEntity = homingArrow;
        }
        return arrowEntity;
    }

    private int getArrowQuantity(BowUpgradeData upgrades, PlayerEntity player) {
        RuneEtching rune = upgrades.runeEtching();
        // Client input must allow the server to decide cooldown readiness.
        // On cooldown the actual shot falls back to the normal fan.
        if (rune == RuneEtching.GRACE || rune == RuneEtching.BOUNTY) return 1;
        if (player.getWorld().isClient() && rune == RuneEtching.PAIN) return net.sweenus.simplybows.util.WinterfangAbilityRules.painArrowCount(upgrades.stringLevel());
        if (player.getWorld().isClient() && rune != RuneEtching.NONE) return 1;
        if (player.getWorld() instanceof ServerWorld world) {
            if (rune == RuneEtching.CHAOS && IceChaosWallManager.isWallReady(world, player.getUuid())) return 1;
            String key = switch (rune) {
                case PAIN -> "ice-pain"; case GRACE -> "ice-grace"; case BOUNTY -> "ice-bounty";
                default -> null;
            };
            if (key != null && RuneUseCooldown.isReady(world, player.getUuid(), key)) return rune == RuneEtching.PAIN ? net.sweenus.simplybows.util.WinterfangAbilityRules.painArrowCount(upgrades.stringLevel()) : 1;
        }
        return baseQuantity() + upgrades.stringLevel();
    }

    private static NbtCompound getOrCreateCustomData(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData == null ? new NbtCompound() : customData.copyNbt();
    }

}
