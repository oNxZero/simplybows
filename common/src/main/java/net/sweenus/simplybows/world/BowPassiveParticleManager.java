package net.sweenus.simplybows.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.util.HelperMethods;

public final class BowPassiveParticleManager {

    private BowPassiveParticleManager() {
    }

    public static void tick(LivingEntity user, ServerWorld world) {
        if (user == null || world == null) {
            return;
        }

        emit(user, world, ItemRegistry.ICE_BOW.get(), 8, ParticleTypes.SNOWFLAKE, 1, ParticleTypes.WHITE_ASH, 2);
        emit(user, world, ItemRegistry.VINE_BOW.get(), 9, ParticleTypes.COMPOSTER, 2, ParticleTypes.FALLING_SPORE_BLOSSOM, 1);
        emit(user, world, ItemRegistry.BUBBLE_BOW.get(), 8, ParticleTypes.DRIPPING_WATER, 2, ParticleTypes.SPLASH, 1);
        emit(user, world, ItemRegistry.BEE_BOW.get(), 9, ParticleTypes.FALLING_HONEY, 1, ParticleTypes.WAX_ON, 1);
        emit(user, world, ItemRegistry.BLOSSOM_BOW.get(), 8, ParticleTypes.CHERRY_LEAVES, 1, ParticleTypes.SPORE_BLOSSOM_AIR, 1);
        emit(
                user,
                world,
                ItemRegistry.EARTH_BOW.get(),
                9,
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()),
                1,
                ParticleTypes.DUST_PLUME,
                1
        );
    }

    private static void emit(
            LivingEntity user,
            ServerWorld world,
            Item bowItem,
            int baseInterval,
            ParticleEffect primary,
            int primaryCount,
            ParticleEffect secondary,
            int secondaryCount
    ) {
        if (!HelperMethods.isHoldingItem(bowItem, user)) {
            return;
        }
        int interval = baseInterval + user.getRandom().nextInt(5);
        if (user.age % interval != 0) {
            return;
        }
        HelperMethods.spawnParticlesAtItem(world, user, bowItem, primary, primaryCount);
        HelperMethods.spawnParticlesAtItem(world, user, bowItem, secondary, secondaryCount);
    }
}
