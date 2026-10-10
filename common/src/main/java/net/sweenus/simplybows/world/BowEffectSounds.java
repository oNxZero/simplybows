package net.sweenus.simplybows.world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Finite, positional cues. Nearby overlapping effects share a short sound budget. */
public final class BowEffectSounds {
    public enum Theme { GARDEN, SUPPORT_GARDEN, WITHER, SPORE, TREE, FROST, SUPPORT_FROST,
        WATER, SUPPORT_WATER, BEE, SUPPORT_BEE, BLOSSOM, SUPPORT_BLOSSOM, EARTH }
    private enum Stage { START, AMBIENT, HIT, END }
    private record Key(SoundEvent sound, Stage stage, int x, int y, int z) {}
    private static final Map<ServerWorld, Map<Key, Long>> PLAYED = new WeakHashMap<>();
    private BowEffectSounds() {}
    public static void start(ServerWorld world, Vec3d pos, Theme theme) {
        SoundEvent sound = switch (theme) {
            case FROST -> SoundEvents.BLOCK_GLASS_PLACE;
            case SUPPORT_FROST, SUPPORT_GARDEN -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case WATER -> SoundEvents.ENTITY_DOLPHIN_SPLASH;
            case SUPPORT_WATER -> SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP;
            case BEE -> SoundEvents.ENTITY_BEE_POLLINATE;
            case SUPPORT_BEE -> SoundEvents.ENTITY_BEE_POLLINATE;
            case TREE, BLOSSOM -> SoundEvents.BLOCK_CHERRY_LEAVES_PLACE;
            case SUPPORT_BLOSSOM -> SoundEvents.BLOCK_SPORE_BLOSSOM_PLACE;
            case EARTH -> SoundEvents.BLOCK_DRIPSTONE_BLOCK_PLACE;
            default -> SoundEvents.BLOCK_MOSS_PLACE;
        };
        play(world,pos,sound,Stage.START,.4F,theme == Theme.EARTH ? .7F : 1.05F,8);
    }
    public static void ambient(ServerWorld world, Vec3d pos, Theme theme) {
        SoundEvent sound = switch (theme) {
            case FROST -> SoundEvents.BLOCK_GLASS_HIT;
            case SUPPORT_FROST, SUPPORT_GARDEN, SUPPORT_BLOSSOM -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case WATER, SUPPORT_WATER -> SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT;
            case BEE, SUPPORT_BEE -> SoundEvents.ENTITY_BEE_POLLINATE;
            case TREE, BLOSSOM -> SoundEvents.BLOCK_CHERRY_LEAVES_HIT;
            case EARTH -> SoundEvents.BLOCK_STONE_HIT;
            default -> SoundEvents.BLOCK_AZALEA_LEAVES_HIT;
        };
        play(world,pos,sound,Stage.AMBIENT,.12F,theme == Theme.WITHER || theme == Theme.EARTH ? .65F : 1.15F,40);
    }
    public static void hit(ServerWorld world, Vec3d pos, Theme theme) {
        SoundEvent sound = switch (theme) {
            case FROST -> SoundEvents.BLOCK_GLASS_HIT;
            case SUPPORT_FROST, SUPPORT_BLOSSOM, SUPPORT_GARDEN -> SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT;
            case WATER, SUPPORT_WATER -> SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP;
            case BEE -> SoundEvents.ENTITY_BEE_STING;
            case SUPPORT_BEE -> SoundEvents.ENTITY_BEE_POLLINATE;
            case EARTH -> SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND;
            case BLOSSOM, TREE -> SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP;
            default -> SoundEvents.BLOCK_AZALEA_LEAVES_HIT;
        };
        play(world,pos,sound,Stage.HIT,.25F,theme == Theme.EARTH ? .8F : 1.1F,10);
    }
    public static void end(ServerWorld world, Vec3d pos, Theme theme) {
        SoundEvent sound = switch (theme) {
            case FROST -> SoundEvents.BLOCK_GLASS_BREAK;
            case SUPPORT_FROST -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case WATER, SUPPORT_WATER -> SoundEvents.ENTITY_PLAYER_SPLASH;
            case BEE, SUPPORT_BEE -> SoundEvents.ENTITY_BEE_POLLINATE;
            case TREE, BLOSSOM, SUPPORT_BLOSSOM -> SoundEvents.BLOCK_CHERRY_LEAVES_BREAK;
            case EARTH -> SoundEvents.BLOCK_DRIPSTONE_BLOCK_BREAK;
            default -> SoundEvents.BLOCK_MOSS_BREAK;
        };
        play(world,pos,sound,Stage.END,.25F,theme == Theme.EARTH ? .75F : .95F,8);
    }
    public static void eruption(ServerWorld world, Vec3d pos, boolean retract) {
        play(world,pos,retract ? SoundEvents.BLOCK_DRIPSTONE_BLOCK_BREAK : SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND,
                retract ? Stage.END : Stage.START,1.0F,retract ? .65F : .8F,8);
    }
    public static void stoneLayer(ServerWorld world, Vec3d pos, boolean retract, int layer) {
        play(world,pos,retract ? SoundEvents.BLOCK_DRIPSTONE_BLOCK_BREAK : SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND,
                retract ? Stage.END : Stage.START,.75F,(retract ? .65F : .85F)+layer*.06F,2);
    }
    public static void crush(ServerWorld world, Vec3d pos) {
        play(world,pos,SoundEvents.BLOCK_DEEPSLATE_BRICKS_BREAK,Stage.HIT,.95F,.65F,3);
    }
    public static void splash(ServerWorld world, Vec3d pos) {
        play(world,pos,SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED,Stage.HIT,.5F,1F,10);
    }
    private static void play(ServerWorld world, Vec3d pos, SoundEvent sound, Stage stage, float volume, float pitch, int interval) {
        Map<Key,Long> sounds=PLAYED.computeIfAbsent(world,w -> new HashMap<>());
        long now=world.getTime();
        if (now%80 == 0 || sounds.size()>=512) sounds.entrySet().removeIf(entry -> now-entry.getValue()>80);
        Key key=new Key(sound,stage,(int)Math.floor(pos.x/4),(int)Math.floor(pos.y/4),(int)Math.floor(pos.z/4));
        Long last=sounds.get(key);
        if (last != null && now-last<interval) return;
        if (last == null && sounds.size()>=512) return;
        sounds.put(key,now);
        world.playSound(null,pos.x,pos.y,pos.z,sound,SoundCategory.PLAYERS,volume,pitch+(world.random.nextFloat()-.5F)*.08F);
    }
}
