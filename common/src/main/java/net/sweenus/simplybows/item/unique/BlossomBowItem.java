package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BlossomArrowEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlossomBowItem extends SimplyBowItem {

    public BlossomBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "blossom";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        serverWorld.playSound(
                null,
                shooter.getX(),
                shooter.getY(),
                shooter.getZ(),
                SoundEvents.BLOCK_CHERRY_LEAVES_PLACE,
                SoundCategory.PLAYERS,
                0.8F,
                1.05F + serverWorld.getRandom().nextFloat() * 0.15F
        );
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * SimplyBowsConfig.INSTANCE.petalwind.arrowSpeedMultiplier.get(), SimplyBowsConfig.INSTANCE.petalwind.arrowDivergence.get(), critical, target);
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

        BlossomArrowEntity arrowEntity = new BlossomArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.petalwind.baseDamage.get());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
