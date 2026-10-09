package net.sweenus.simplybows.item.unique;
import dev.architectury.networking.NetworkManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.network.AbilityCooldownPayload;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.world.RuneUseCooldown;
import net.sweenus.simplybows.util.BowTooltipHelper;
import net.sweenus.simplybows.util.BowUser;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongSupplier;

public class SimplyBowItem extends BowItem {
    private static final ThreadLocal<Boolean> FORCE_VANILLA_ARROW = ThreadLocal.withInitial(() -> false);
    public static Function<String, long[]> CLIENT_COOLDOWN_READER = null;
    public static LongSupplier CLIENT_COOLDOWN_TICK_READER = null;

    public SimplyBowItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(1920).fireproof());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient()) {
            BowUpgradeData.migrateLegacy(stack);
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().styled(style -> style.withColor(switch (getTooltipBowKey()) {
            case "vine" -> 0x85C76B; case "ice" -> 0x8BD4F2; case "bubble" -> 0x55C8E8;
            case "bee" -> 0xEDC65B; case "blossom" -> 0xF19DBC; case "earth" -> 0xC29A69;
            default -> 0xFFFFFF;
        }));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        // Durability stays on the vanilla bar. Ability cooldown uses the white sweep overlay.
        return super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        // Keep vanilla item bar behavior (durability only).
        return super.getItemBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        // Keep vanilla item bar behavior (durability only).
        return super.getItemBarColor(stack);
    }

    public boolean simplybows$hasAbilityCooldown() {
        Function<String, long[]> reader = CLIENT_COOLDOWN_READER;
        LongSupplier tickReader = CLIENT_COOLDOWN_TICK_READER;
        if (reader == null || tickReader == null) {
            return false;
        }
        // Shared CD across every unique bow for this player.
        long[] data = reader.apply(RuneUseCooldown.GLOBAL_BOW_KEY);
        return data != null && tickReader.getAsLong() < data[0];
    }

    public float simplybows$getAbilityCooldownProgress(float tickDelta) {
        Function<String, long[]> reader = CLIENT_COOLDOWN_READER;
        LongSupplier tickReader = CLIENT_COOLDOWN_TICK_READER;
        if (reader == null || tickReader == null) {
            return 0.0F;
        }
        long[] data = reader.apply(RuneUseCooldown.GLOBAL_BOW_KEY);
        if (data == null) {
            return 0.0F;
        }

        long nowTick = tickReader.getAsLong();
        int total = (int) data[1];
        if (total <= 0 || nowTick >= data[0]) {
            return 0.0F;
        }

        float remaining = (data[0] - nowTick) - tickDelta;
        if (remaining <= 0.0F) {
            return 0.0F;
        }
        return Math.min(1.0F, remaining / total);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        BowTooltipHelper.appendBowTooltip(getTooltipBowKey(), stack, tooltip);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        ItemStack projectileStack = BowUser.getProjectile(user, stack);
        if (projectileStack.isEmpty()) {
            return;
        }
        boolean hasInfiniteAmmo = BowUser.hasInfiniteAmmo(user, stack, projectileStack);

        int i = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float f = getPullProgress(i);
        if (this instanceof IceBowItem && net.sweenus.simplybows.upgrade.BowUpgradeData.from(stack).runeEtching()
                == net.sweenus.simplybows.upgrade.RuneEtching.GRACE && f < 1.0F) return;
        if (!((double)f < 0.1)) {
            List<ItemStack> list;
            if (hasInfiniteAmmo) {
                ItemStack virtualProjectile = projectileStack.copy();
                virtualProjectile.setCount(1);
                list = List.of(virtualProjectile);
            } else {
                list = load(stack, projectileStack, user);
            }
            if (world instanceof ServerWorld serverWorld) {
                if (!list.isEmpty()) {
                    performStoppedUsing(stack, world, user, remainingUseTicks, f, user.getActiveHand(), serverWorld, list, null);
                }
            }

            SoundCategory soundCategory = user instanceof PlayerEntity ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, soundCategory, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
            if (user instanceof PlayerEntity playerEntity) {
                playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
            }
        }
    }

    /**
     * Full-draw shot fired by a mob at its current target: same pipeline as a player's
     * released shot (custom arrows + rune abilities), no stats/packets/ammo consumption.
     */
    public void performMobShot(ServerWorld world, MobEntity mob, ItemStack stack, @Nullable LivingEntity target) {
        ItemStack projectileStack = BowUser.getProjectile(mob, stack);
        if (projectileStack.isEmpty()) {
            return;
        }
        ItemStack virtualProjectile = projectileStack.copy();
        virtualProjectile.setCount(1);
        performStoppedUsing(stack, world, mob, 0, 1.0F, Hand.MAIN_HAND, world, List.of(virtualProjectile), target);
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
    }

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        if (target != null && !(shooter instanceof PlayerEntity)) {
            Vec3d targetPos = new Vec3d(target.getX(), target.getBodyY(1.0 / 3.0), target.getZ());
            Vec3d direction = simplybows$getCompensatedAimDirection(projectile.getPos(), targetPos, speed, 0.05F);
            if (direction.lengthSquared() > 1.0E-6) {
                Vec3d aimed = direction.rotateY((float) Math.toRadians(-yaw));
                projectile.setVelocity(aimed.x, aimed.y, aimed.z, speed, divergence);
                simplybows$applyNonPlayerProjectileDamageModifier(shooter, projectile);
                return;
            }
        }
        projectile.setVelocity(shooter, shooter.getPitch(), shooter.getYaw() + yaw, 0.0F, speed, divergence);
        simplybows$applyNonPlayerProjectileDamageModifier(shooter, projectile);
    }

    private static Vec3d simplybows$getCompensatedAimDirection(Vec3d origin, Vec3d targetPos, float speed, float gravityPerTick) {
        Vec3d delta = targetPos.subtract(origin);
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalDistance <= 1.0E-6) {
            return delta.lengthSquared() > 1.0E-6 ? delta.normalize() : Vec3d.ZERO;
        }

        double safeSpeed = Math.max(0.1, speed);
        double travelTicks = horizontalDistance / safeSpeed;
        double dropCompensation = 0.5 * gravityPerTick * travelTicks * travelTicks;
        Vec3d compensated = new Vec3d(delta.x, delta.y + dropCompensation, delta.z);
        return compensated.lengthSquared() > 1.0E-6 ? compensated.normalize() : delta.normalize();
    }

    private static void simplybows$applyNonPlayerProjectileDamageModifier(LivingEntity shooter, ProjectileEntity projectile) {
        if (!(shooter instanceof PlayerEntity) && projectile instanceof PersistentProjectileEntity persistentProjectile) {
            persistentProjectile.setDamage(
                    CombatTargeting.applyNonPlayerProjectileDamageModifier(shooter, (float) persistentProjectile.getDamage()));
        }
    }


    private void performStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, float f, Hand hand, ServerWorld serverWorld, List<ItemStack> list, @Nullable LivingEntity target) {
        Item item = stack.getItem();

        switch (item) {
            case IceBowItem iceBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, false, target);
                } else {
                    iceBowItem.performStoppedUsing(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, true, target);
                }
            }
            case VineBowItem vineBowItem -> {
                vineBowItem.performStoppedUsing(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, f == 1.0F, target);
            }
            case BubbleBowItem bubbleBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, false, target);
                } else {
                    bubbleBowItem.performStoppedUsing(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, true, target);
                }
            }
            case BeeBowItem beeBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, false, target);
                } else {
                    beeBowItem.performStoppedUsing(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, true, target);
                }
            }
            case BlossomBowItem blossomBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, false, target);
                } else {
                    blossomBowItem.performStoppedUsing(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, true, target);
                }
            }
            case EarthBowItem earthBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, false, target);
                } else {
                    earthBowItem.performStoppedUsing(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, true, target);
                }
            }
            default -> {
                this.shootAll(serverWorld, user, hand, stack, list, f * 3.0F, 1.0F, f == 1.0F, target);
            }
        }

    }


    protected void shootFan(SimplyBowItem bow, ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack,
                            List<ItemStack> projectiles, float speed, float divergence, boolean critical,
                            @Nullable LivingEntity target, int quantity) {
        float f = EnchantmentHelper.getProjectileSpread(world, stack, shooter, 0.0F);
        float g = projectiles.size() == 1 ? 0.0F : 2.0F * f / (float) (projectiles.size() - 1);
        float h = (float)((projectiles.size() - 1) % 2) * g / 2.0F;
        float i = 1.0F;

        int additionalArrowsNeeded = Math.max(0, quantity - 1) * projectiles.size();
        BowUser.ExtraArrowSupply extraArrows = BowUser.extraArrows(shooter, stack, additionalArrowsNeeded);

        // Iterate through the projectiles
        for (int j = 0; j < projectiles.size(); ++j) {
            for (int p = 0; p < quantity; ++p) {
                ItemStack arrowForProjectile;
                if (p == 0) {
                    // First shot for this projectile uses the arrow already consumed by Minecraft
                    arrowForProjectile = projectiles.get(j);
                } else {
                    arrowForProjectile = extraArrows.next(projectiles.get(j));
                    if (arrowForProjectile == null || arrowForProjectile.isEmpty()) {
                        // insufficient arrows to continue
                        break;
                    }
                }

                // Calculate the spread
                float k = h + i * (float) ((j + 1) / 2) * g;
                i = -i;

                // Create and shoot the projectile
                ProjectileEntity projectileEntity = bow.createArrowEntity(world, shooter, stack, arrowForProjectile, critical);
                bow.simplybows$applyRangedWeaponProjectileBonus(shooter, projectileEntity);
                bow.shoot(shooter, projectileEntity, j, speed, divergence, k + (p - ((float) quantity / 2)) * quantity, target);
                world.spawnEntity(projectileEntity);

                // Damage the bow after firing
                stack.damage(bow.getWeaponStackDamage(arrowForProjectile), shooter, LivingEntity.getSlotForHand(hand));

                // Stop processing if the bow breaks
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }

    protected static boolean simplybows$isForcingVanillaArrow() {
        return FORCE_VANILLA_ARROW.get();
    }

    /**
     * Sends a cooldown sync packet to {@code player} so the client-side bar appears
     * without writing anything to the ItemStack (which would cause the equip/bob animation).
     */
    protected void simplybows$startAbilityItemCooldown(ServerPlayerEntity player, int cooldownTicks) {
        if (player == null || cooldownTicks <= 0) {
            return;
        }
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            RuneUseCooldown.start(serverWorld, player.getUuid(), "item-" + getTooltipBowKey(), getTooltipBowKey(), cooldownTicks);
            return;
        }
        long endMs = System.currentTimeMillis() + Math.max(1, cooldownTicks) * 50L;
        simplybows$sendCooldownPacket(player, getTooltipBowKey(), endMs, cooldownTicks);
    }

    /**
     * Cooldown-bar sync for any wielder: sends the packet when the wielder is a player,
     * no-ops for mobs (whose shot cadence is governed by MobBowFireManager and the
     * managers' own server-side cooldown storage).
     */
    protected void simplybows$startAbilityItemCooldown(LivingEntity user, int cooldownTicks) {
        if (user instanceof ServerPlayerEntity player) {
            simplybows$startAbilityItemCooldown(player, cooldownTicks);
        }
    }

    /**
     * Sends a raw cooldown packet to the given player.
     * Call this from managers (e.g. VineFlowerFieldManager) that need to extend the bar
     * beyond its initial value without going through an ItemStack.
     */
    public static void simplybows$sendCooldownPacket(ServerPlayerEntity player, String bowKey, long endMs, int totalTicks) {
        if (player == null || endMs <= 0 || totalTicks <= 0) {
            return;
        }
        // Always sync as the shared player CD so every Simply Bow shows the same overlay.
        NetworkManager.sendToPlayer(player, new AbilityCooldownPayload(endMs, totalTicks, RuneUseCooldown.GLOBAL_BOW_KEY));
    }

    protected boolean simplybows$hasInfiniteAmmo(PlayerEntity player, ItemStack bowStack) {
        return simplybows$hasInfiniteAmmo(player, bowStack, player.getProjectileType(bowStack));
    }

    protected boolean simplybows$hasInfiniteAmmo(PlayerEntity player, ItemStack bowStack, ItemStack projectileStack) {
        if (player.getAbilities().creativeMode) {
            return true;
        }
        if (projectileStack == null || projectileStack.isEmpty() || !projectileStack.isOf(Items.ARROW)) {
            return false;
        }
        Registry<net.minecraft.enchantment.Enchantment> enchantmentRegistry =
                player.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        return enchantmentRegistry
                .getEntry(Enchantments.INFINITY)
                .map(infinity -> EnchantmentHelper.getLevel(infinity, bowStack) > 0)
                .orElse(false);
    }

    private void simplybows$shootVanillaArrow(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack,
                                              List<ItemStack> projectiles, float speed, float divergence, boolean critical,
                                              @Nullable LivingEntity target) {
        FORCE_VANILLA_ARROW.set(true);
        try {
            this.shootAll(world, shooter, hand, stack, projectiles, speed, divergence, critical, target);
        } finally {
            FORCE_VANILLA_ARROW.set(false);
        }
    }

    protected String getTooltipBowKey() {
        return "generic";
    }

    protected void simplybows$applyRangedWeaponProjectileBonus(@Nullable LivingEntity shooter, ProjectileEntity projectileEntity) {
        if ((projectileEntity instanceof net.sweenus.simplybows.entity.HomingArrowEntity arrow && arrow.isGraceSupportProjectile())
                || (projectileEntity instanceof net.sweenus.simplybows.entity.HomingSpectralArrowEntity spectral && spectral.isGraceSupportProjectile())) return;
        if (projectileEntity instanceof PersistentProjectileEntity persistentProjectile) {
            persistentProjectile.setDamage(persistentProjectile.getDamage() + CombatTargeting.getRangedWeaponDamageBonus(shooter));
        }
    }


}
