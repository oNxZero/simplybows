package net.sweenus.simplybows.world;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.ShoulderBowEntity;
import net.sweenus.simplybows.item.unique.EchoBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EchoShoulderBowManager {

    private static final Map<UUID, CompanionPair> ACTIVE_BOWS = new HashMap<>();
    private static final Map<UUID, UUID> FOCUSED_TARGETS = new HashMap<>();
    private static final Map<UUID, GracePotionCharge> GRACE_POTION_CHARGES = new HashMap<>();
    private static double lookTargetDistance() { return SimplyBowsConfig.INSTANCE.echoBow.lookTargetDistance.get(); }
    private static int graceBaseShots() { return SimplyBowsConfig.INSTANCE.echoBow.graceBaseShots.get(); }
    private static int graceShotsPerString() { return SimplyBowsConfig.INSTANCE.echoBow.graceShotsPerString.get(); }
    private static float graceBaseSplashRadius() { return SimplyBowsConfig.INSTANCE.echoBow.graceBaseSplashRadius.get(); }
    private static float graceSplashRadiusPerFrame() { return SimplyBowsConfig.INSTANCE.echoBow.graceSplashRadiusPerFrame.get(); }

    private EchoShoulderBowManager() {
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        tickOwner(player);
    }

    public static void tickOwner(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        UUID ownerId = owner.getUuid();
        boolean holdingEchoBow = owner.getMainHandStack().getItem() instanceof EchoBowItem;
        if (!holdingEchoBow) {
            discardTracked(world, ownerId);
            ACTIVE_BOWS.remove(ownerId);
            FOCUSED_TARGETS.remove(ownerId);
            GRACE_POTION_CHARGES.remove(ownerId);
            return;
        }

        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        ShoulderBowEntity left = resolveTracked(world, pair != null ? pair.leftId() : null, ownerId, -1);
        ShoulderBowEntity right = resolveTracked(world, pair != null ? pair.rightId() : null, ownerId, 1);

        if (left == null) {
            left = new ShoulderBowEntity(world, owner, -1);
            world.spawnEntity(left);
        }
        if (right == null) {
            right = new ShoulderBowEntity(world, owner, 1);
            world.spawnEntity(right);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(owner.getMainHandStack());
        if (upgrades.runeEtching() != RuneEtching.GRACE) {
            GRACE_POTION_CHARGES.remove(ownerId);
        }
        boolean mirrorOffhandBow = upgrades.runeEtching() == RuneEtching.BOUNTY
                && owner.getOffHandStack().getItem() instanceof SimplyBowItem
                && !(owner.getOffHandStack().getItem() instanceof EchoBowItem);
        UUID focusedTargetId = null;
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            focusedTargetId = FOCUSED_TARGETS.get(ownerId);
            validateFocusedTarget(owner, focusedTargetId);
            focusedTargetId = FOCUSED_TARGETS.get(ownerId);
        } else {
            FOCUSED_TARGETS.remove(ownerId);
        }

        left.configureOffhandMirror(false, ItemStack.EMPTY);
        right.configureOffhandMirror(mirrorOffhandBow, mirrorOffhandBow ? owner.getOffHandStack() : ItemStack.EMPTY);
        left.setForcedTargetUuid(focusedTargetId);
        right.setForcedTargetUuid(focusedTargetId);

        ACTIVE_BOWS.put(ownerId, new CompanionPair(left.getUuid(), right.getUuid()));

        if ((world.getTime() + owner.getId()) % 40L == 0L) {
            cleanupOwnerOrphans(world, ownerId, left.getUuid(), right.getUuid());
        }
    }

    public static void onOwnerFired(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        UUID ownerId = owner.getUuid();
        tickOwner(owner);
        Vec3d lookDirection = owner.getRotationVec(1.0F);
        UUID focusedTarget = resolveLookFocusedTarget(owner);
        if (focusedTarget != null) {
            FOCUSED_TARGETS.put(ownerId, focusedTarget);
        } else {
            focusedTarget = FOCUSED_TARGETS.get(ownerId);
        }
        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        if (pair == null) {
            return;
        }

        ShoulderBowEntity left = resolveTracked(world, pair.leftId(), ownerId, -1);
        ShoulderBowEntity right = resolveTracked(world, pair.rightId(), ownerId, 1);
        if (left != null) {
            left.queueLookShot(lookDirection, focusedTarget);
        }
        if (right != null) {
            right.queueLookShot(lookDirection, focusedTarget);
        }
    }

    public static void setFocusedTarget(LivingEntity owner, @Nullable LivingEntity target) {
        if (owner == null) {
            return;
        }
        UUID ownerId = owner.getUuid();
        if (target == null || !target.isAlive() || !CombatTargeting.checkFriendlyFire(target, owner)) {
            FOCUSED_TARGETS.remove(ownerId);
            return;
        }
        FOCUSED_TARGETS.put(ownerId, target.getUuid());
    }

    public static void tickWorld(ServerWorld world) {
        if (world.getTime() % 20L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ShoulderBowEntity shoulderBow)) {
                continue;
            }
            UUID ownerId = shoulderBow.getOwnerUuid();
            if (ownerId == null) {
                shoulderBow.discard();
                continue;
            }
            LivingEntity owner = world.getEntity(ownerId) instanceof LivingEntity living ? living : null;
            if (owner == null || !owner.isAlive() || !(owner.getMainHandStack().getItem() instanceof EchoBowItem)) {
                shoulderBow.discard();
                ACTIVE_BOWS.remove(ownerId);
                FOCUSED_TARGETS.remove(ownerId);
                GRACE_POTION_CHARGES.remove(ownerId);
            }
        }
    }

    public static void registerGracePotionCharge(ServerPlayerEntity player, List<StatusEffectInstance> effects) {
        if (player == null || effects == null || effects.isEmpty()) {
            return;
        }
        ItemStack mainHand = player.getMainHandStack();
        if (!(mainHand.getItem() instanceof EchoBowItem)) {
            return;
        }

        BowUpgradeData upgrades = BowUpgradeData.from(mainHand);
        if (upgrades.runeEtching() != RuneEtching.GRACE) {
            return;
        }

        boolean allBeneficial = true;
        java.util.ArrayList<StatusEffectInstance> copied = new java.util.ArrayList<>();
        for (StatusEffectInstance effect : effects) {
            if (effect == null) {
                continue;
            }
            StatusEffectInstance instance = new StatusEffectInstance(effect);
            copied.add(instance);
            if (!instance.getEffectType().value().isBeneficial()) {
                allBeneficial = false;
            }
        }
        if (copied.isEmpty()) {
            return;
        }

        int shots = graceBaseShots() + upgrades.stringLevel() * graceShotsPerString();
        GRACE_POTION_CHARGES.put(player.getUuid(), new GracePotionCharge(copied, allBeneficial, shots));
    }

    @Nullable
    public static GracePotionPayload consumeGracePotionShot(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        GracePotionCharge charge = GRACE_POTION_CHARGES.get(player.getUuid());
        if (charge == null || charge.shotsRemaining <= 0 || charge.effects.isEmpty()) {
            GRACE_POTION_CHARGES.remove(player.getUuid());
            return null;
        }

        ItemStack mainHand = player.getMainHandStack();
        BowUpgradeData upgrades = BowUpgradeData.from(mainHand);
        if (!(mainHand.getItem() instanceof EchoBowItem) || upgrades.runeEtching() != RuneEtching.GRACE) {
            return null;
        }

        float splashRadius = graceBaseSplashRadius() + upgrades.frameLevel() * graceSplashRadiusPerFrame();
        GracePotionPayload payload = new GracePotionPayload(copyEffects(charge.effects), charge.supportMode, splashRadius);
        charge.shotsRemaining--;
        if (charge.shotsRemaining <= 0) {
            GRACE_POTION_CHARGES.remove(player.getUuid());
            moveNextValidPotionToOffhand(player);
        }
        return payload;
    }

    public static boolean hasGracePotionCharge(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        GracePotionCharge charge = GRACE_POTION_CHARGES.get(player.getUuid());
        return charge != null && charge.shotsRemaining > 0 && !charge.effects.isEmpty();
    }

    public static boolean isGraceSupportActive(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        GracePotionCharge charge = GRACE_POTION_CHARGES.get(player.getUuid());
        if (charge == null || charge.shotsRemaining <= 0 || !charge.supportMode) {
            return false;
        }
        ItemStack mainHand = player.getMainHandStack();
        return mainHand.getItem() instanceof EchoBowItem && BowUpgradeData.from(mainHand).runeEtching() == RuneEtching.GRACE;
    }

    private static List<StatusEffectInstance> copyEffects(List<StatusEffectInstance> effects) {
        java.util.ArrayList<StatusEffectInstance> copied = new java.util.ArrayList<>();
        for (StatusEffectInstance effect : effects) {
            if (effect != null) {
                copied.add(new StatusEffectInstance(effect));
            }
        }
        return copied;
    }

    private static void validateFocusedTarget(LivingEntity owner, @Nullable UUID focusedTargetId) {
        if (focusedTargetId == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Entity focusedEntity = world.getEntity(focusedTargetId);
        if (!(focusedEntity instanceof LivingEntity living) || !living.isAlive() || !CombatTargeting.checkFriendlyFire(living, owner)) {
            FOCUSED_TARGETS.remove(owner.getUuid());
        }
    }

    @Nullable
    private static UUID resolveLookFocusedTarget(LivingEntity owner) {
        // Mobs have no crosshair; their focused target is whatever their AI is attacking.
        if (owner instanceof MobEntity mob) {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive() && CombatTargeting.checkFriendlyFire(target, mob)) {
                return target.getUuid();
            }
            return null;
        }

        Vec3d start = owner.getCameraPosVec(1.0F);
        Vec3d look = owner.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(lookTargetDistance()));
        Box searchBox = owner.getBoundingBox().stretch(look.multiply(lookTargetDistance())).expand(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                owner.getWorld(),
                owner,
                start,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && CombatTargeting.checkFriendlyFire(living, owner)
        );
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }

        HitResult blockHit = owner.raycast(lookTargetDistance(), 1.0F, false);
        double entityDistanceSq = start.squaredDistanceTo(entityHit.getPos());
        double blockDistanceSq = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : start.squaredDistanceTo(blockHit.getPos());
        if (entityDistanceSq > blockDistanceSq) {
            return null;
        }
        return living.getUuid();
    }

    private static ShoulderBowEntity resolveTracked(ServerWorld world, UUID bowId, UUID ownerId, int side) {
        if (bowId == null) {
            return null;
        }

        Entity entity = world.getEntity(bowId);
        if (!(entity instanceof ShoulderBowEntity bow)) {
            return null;
        }
        if (!ownerId.equals(bow.getOwnerUuid()) || bow.getSide() != side || !bow.isAlive()) {
            bow.discard();
            return null;
        }
        return bow;
    }

    private static void discardTracked(ServerWorld world, UUID ownerId) {
        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        if (pair == null) {
            return;
        }
        discardByUuid(world, pair.leftId());
        discardByUuid(world, pair.rightId());
    }

    private static void discardByUuid(ServerWorld world, UUID entityId) {
        if (entityId == null) {
            return;
        }
        Entity entity = world.getEntity(entityId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void cleanupOwnerOrphans(ServerWorld world, UUID ownerId, UUID leftId, UUID rightId) {
        Set<UUID> keep = new HashSet<>();
        keep.add(leftId);
        keep.add(rightId);
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ShoulderBowEntity bow)) {
                continue;
            }
            if (ownerId.equals(bow.getOwnerUuid()) && !keep.contains(bow.getUuid())) {
                bow.discard();
            }
        }
    }

    private static void moveNextValidPotionToOffhand(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        ItemStack offhand = player.getOffHandStack();
        boolean offhandEmpty = offhand == null || offhand.isEmpty();
        boolean offhandIsBottle = !offhandEmpty && offhand.isOf(Items.GLASS_BOTTLE);
        if (!offhandEmpty && !offhandIsBottle) {
            return;
        }

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!isValidPotionStack(stack)) {
                continue;
            }

            if (offhandIsBottle) {
                // True swap: potion stack into offhand, bottle stack into source slot.
                player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, stack.copy());
                player.getInventory().setStack(i, offhand.copy());
            } else {
                player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, stack.copy());
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
            player.getInventory().markDirty();
            return;
        }
    }

    private static boolean isValidPotionStack(ItemStack stack) {
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

    private record CompanionPair(UUID leftId, UUID rightId) {
    }

    private static final class GracePotionCharge {
        private final List<StatusEffectInstance> effects;
        private final boolean supportMode;
        private int shotsRemaining;

        private GracePotionCharge(List<StatusEffectInstance> effects, boolean supportMode, int shotsRemaining) {
            this.effects = effects;
            this.supportMode = supportMode;
            this.shotsRemaining = shotsRemaining;
        }
    }

    public record GracePotionPayload(List<StatusEffectInstance> effects, boolean supportMode, float splashRadius) {
    }
}
