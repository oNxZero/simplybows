package net.sweenus.simplybows.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelperMethods {

    public static void spawnParticlesAtItem(
            ServerWorld serverWorld,
            LivingEntity user,
            Item item,
            ParticleEffect particle,
            int particleCount) {
        ItemStack mainHandItem = user.getMainHandStack();
        ItemStack offHandItem = user.getOffHandStack();

        boolean isBowInMainHand = mainHandItem.getItem().equals(item);
        boolean isBowInOffHand = offHandItem.getItem().equals(item);

        Vec3d handPosition;
        if (isBowInMainHand) {
            handPosition = getHandPosition(user, Hand.OFF_HAND);
        } else if (isBowInOffHand) {
            handPosition = getHandPosition(user, Hand.MAIN_HAND);
        } else {
            return;
        }

        double bowLength = 1.2;
        double bowWidth = 0.1;

        Vec3d bowDirection = user.getRotationVec(1.0F).normalize();

        for (int i = 0; i < particleCount; i++) {
            double lengthOffset = (serverWorld.random.nextDouble() - 0.5) * bowLength;
            double widthOffsetX = (serverWorld.random.nextDouble() - 0.5) * bowWidth;
            double widthOffsetZ = (serverWorld.random.nextDouble() - 0.5) * bowWidth;

            Vec3d particlePosition = handPosition
                    .add(bowDirection.multiply(lengthOffset))
                    .add(new Vec3d(widthOffsetX, 0, widthOffsetZ));
            serverWorld.spawnParticles(
                    particle,
                    particlePosition.x,
                    particlePosition.y,
                    particlePosition.z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }

    public static void spawnParticlesInFrontOfPlayer(
            ServerWorld serverWorld,
            LivingEntity user,
            ParticleEffect particle,
            int particleCount) {

        Vec3d eyePosition = user.getEyePos();
        Vec3d lookDirection = user.getRotationVec(1.0F).normalize();
        Vec3d particleStartPosition = eyePosition.add(lookDirection.multiply(0.5));

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (serverWorld.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (serverWorld.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (serverWorld.random.nextDouble() - 0.5) * 0.2;

            Vec3d particlePosition = particleStartPosition.add(offsetX, offsetY, offsetZ);

            serverWorld.spawnParticles(
                    particle,
                    particlePosition.x,
                    particlePosition.y,
                    particlePosition.z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }

    public static Map<ItemStack, Integer> findArrowStacks(PlayerEntity player) {
        Map<ItemStack, Integer> arrowStacks = new HashMap<>();

        // Iterate through player inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);

            // Check  stack is not empty
            if (!stack.isEmpty() && stack.getItem() instanceof ArrowItem) {
                arrowStacks.put(stack, stack.getCount());
            }
        }

        return arrowStacks;
    }

    public static List<ItemStack> collectArrows(Map<ItemStack, Integer> arrowStacks, int arrowsNeeded) {
        List<ItemStack> consumedArrows = new ArrayList<>();
        int remaining = arrowsNeeded;

        for (Map.Entry<ItemStack, Integer> entry : arrowStacks.entrySet()) {
            ItemStack arrowStack = entry.getKey();
            int count = entry.getValue();

            if (remaining <= 0) {
                break;
            }

            ItemStack consumableStack = arrowStack.copy();
            consumableStack.setCount(Math.min(count, remaining));

            consumedArrows.add(consumableStack);

            arrowStack.decrement(consumableStack.getCount());
            remaining -= consumableStack.getCount();
        }

        return consumedArrows;
    }

    public static @Nullable ItemStack consumeNextArrow(List<ItemStack> consumedArrows) {
        if (!consumedArrows.isEmpty()) {
            ItemStack stack = consumedArrows.get(0);
            ItemStack oneArrow = stack.copy();
            oneArrow.setCount(1);

            // Reduce stack size by 1
            stack.decrement(1);

            // If stack becomes empty, remove from list
            if (stack.isEmpty()) {
                consumedArrows.remove(0);
            }

            return oneArrow;
        }

        return null;
    }





    public static Vec3d getHandPosition(LivingEntity user, Hand hand) {
        Vec3d eyePosition = user.getEyePos();

        boolean isRightHand = (hand == Hand.MAIN_HAND) == (user.getMainArm() == Arm.RIGHT);

        double horizontalOffset = isRightHand ? 0.35 : -0.35;
        Vec3d handOffset = user.getRotationVec(1.0F).rotateY((float) Math.PI / 2).multiply(horizontalOffset);

        double verticalOffset = user.isSneaking() ? -0.8 : -0.6;

        return eyePosition.add(handOffset).add(0, verticalOffset, 0);
    }



    public static boolean isHolding(ItemStack stack, LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.MAINHAND).equals(stack)
                || entity.getEquippedStack(EquipmentSlot.OFFHAND).equals(stack);
    }

    public static boolean isHoldingItem(Item item, LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.MAINHAND).getItem().equals(item)
                || entity.getEquippedStack(EquipmentSlot.OFFHAND).getItem().equals(item);
    }

    public static boolean hasItemInInventory(PlayerEntity player, Item item) {
        if (player == null || item == null)
            return false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }


}
