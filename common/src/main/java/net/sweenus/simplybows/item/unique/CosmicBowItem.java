package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;

public class CosmicBowItem extends SimplyBowItem {

    public CosmicBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "cosmic";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, java.util.List<ItemStack> projectiles, float f, float g, boolean critical, LivingEntity target) {
        playFireSound(serverWorld, shooter);
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        float runeSpeedMultiplier = upgrades.runeEtching() == RuneEtching.BOUNTY
                ? SimplyBowsConfig.INSTANCE.cosmicBow.bountyArrowSpeedMultiplier.get()
                : 1.0F;
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * SimplyBowsConfig.INSTANCE.cosmicBow.arrowSpeedMultiplier.get() * runeSpeedMultiplier, SimplyBowsConfig.INSTANCE.cosmicBow.arrowDivergence.get(), critical, target);
    }

    private static void playFireSound(ServerWorld world, LivingEntity shooter) {
        float shimmerPitch = 1.35F + world.random.nextFloat() * 0.18F;
        float castPitch = 1.55F + world.random.nextFloat() * 0.16F;
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.75F, shimmerPitch);
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.35F, castPitch);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        CosmicArrowEntity arrowEntity = new CosmicArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.cosmicBow.baseDamage.get());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
