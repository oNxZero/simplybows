package net.sweenus.simplybows.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SimplyBowsCommands {

    private static final List<EntityType<? extends MobEntity>> HOSTILE_MOBS = List.of(
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.HUSK
    );
    private static final float RUNE_CHANCE = 0.5F;
    private static final RuneEtching[] RANDOM_RUNES = {
            RuneEtching.PAIN, RuneEtching.GRACE, RuneEtching.BOUNTY, RuneEtching.CHAOS
    };
    private static final DynamicCommandExceptionType UNKNOWN_BOW =
            new DynamicCommandExceptionType(id -> Text.literal("Unknown unique bow: " + id));

    private static List<Item> cachedUniqueBows;

    private static final SuggestionProvider<ServerCommandSource> BOW_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(
                    getUniqueBows().stream().map(item -> Registries.ITEM.getId(item).toString()),
                    builder);

    private static final SuggestionProvider<ServerCommandSource> RUNE_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(
                    List.of("pain", "grace", "bounty", "chaos", "none"),
                    builder);

    private SimplyBowsCommands() {
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("simplybows")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("spawn_hostile")
                        .executes(context -> spawnHostile(context.getSource(), 1, null, null))
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 50))
                                .executes(context -> spawnHostile(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "count"), null, null))
                                .then(CommandManager.argument("bow", IdentifierArgumentType.identifier())
                                        .suggests(BOW_SUGGESTIONS)
                                        .executes(context -> spawnHostile(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count"),
                                                resolveBow(context), null))
                                        .then(CommandManager.argument("rune", StringArgumentType.word())
                                                .suggests(RUNE_SUGGESTIONS)
                                                .executes(context -> spawnHostile(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "count"),
                                                        resolveBow(context),
                                                        RuneEtching.fromId(StringArgumentType.getString(context, "rune")))))))));
    }

    private static int spawnHostile(ServerCommandSource source, int count, @Nullable Item forcedBow, @Nullable RuneEtching forcedRune) {
        ServerWorld world = source.getWorld();
        Random random = world.getRandom();
        List<Item> bows = getUniqueBows();
        if (bows.isEmpty()) {
            source.sendError(Text.literal("No Simply Bows unique bows are registered."));
            return 0;
        }

        int spawned = 0;
        MobEntity lastMob = null;
        ItemStack lastStack = ItemStack.EMPTY;
        RuneEtching lastRune = RuneEtching.NONE;
        for (int i = 0; i < count; i++) {
            double offsetX = random.nextBetween(-3, 3);
            double offsetZ = random.nextBetween(-3, 3);
            BlockPos spawnPos = BlockPos.ofFloored(
                    source.getPosition().x + offsetX,
                    source.getPosition().y,
                    source.getPosition().z + offsetZ);

            EntityType<? extends MobEntity> mobType = HOSTILE_MOBS.get(random.nextInt(HOSTILE_MOBS.size()));
            MobEntity mob = mobType.spawn(world, spawnPos, SpawnReason.COMMAND);
            if (mob == null) {
                continue;
            }

            Item bowItem = forcedBow != null ? forcedBow : bows.get(random.nextInt(bows.size()));
            RuneEtching rune = forcedRune;
            if (rune == null && random.nextFloat() <= RUNE_CHANCE) {
                rune = RANDOM_RUNES[random.nextInt(RANDOM_RUNES.length)];
            }
            if (rune == null) {
                rune = RuneEtching.NONE;
            }

            ItemStack bowStack = new ItemStack(bowItem);
            BowUpgradeData upgrades = BowUpgradeData.from(bowStack).withRune(rune);
            for (int s = random.nextInt(3); s > 0; s--) {
                upgrades = upgrades.withIncreasedString();
            }
            for (int f = random.nextInt(3); f > 0; f--) {
                upgrades = upgrades.withIncreasedFrame();
            }
            upgrades.write(bowStack);
            // Mob shots damage the stack (unlike vanilla skeleton firing); keep test bows alive.
            bowStack.getOrCreateNbt().putBoolean("Unbreakable", true);

            mob.equipStack(EquipmentSlot.MAINHAND, bowStack);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
            mob.setPersistent();

            spawned++;
            lastMob = mob;
            lastStack = bowStack;
            lastRune = rune;
        }

        if (spawned == 0) {
            source.sendError(Text.literal("Failed to spawn any hostile."));
            return 0;
        }

        if (spawned == 1 && lastMob != null) {
            MobEntity mob = lastMob;
            ItemStack stack = lastStack;
            RuneEtching rune = lastRune;
            source.sendFeedback(() -> Text.literal("Spawned ")
                    .append(mob.getType().getName().copy().formatted(Formatting.RED))
                    .append(Text.literal(" wielding "))
                    .append(stack.getName().copy())
                    .append(rune == RuneEtching.NONE
                            ? Text.literal("")
                            : Text.literal(" [" + rune.id() + " rune]").formatted(Formatting.LIGHT_PURPLE)), true);
        } else {
            int total = spawned;
            source.sendFeedback(() -> Text.literal("Spawned " + total + " hostiles armed with unique bows."), true);
        }
        return spawned;
    }

    private static Item resolveBow(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "bow");
        Item item = lookupBow(id);
        if (item == null && !id.getPath().contains("/")) {
            // Convenience: "simplybows:ice_bow" -> "simplybows:ice_bow/ice_bow"
            item = lookupBow(new Identifier(SimplyBows.MOD_ID, id.getPath() + "/" + id.getPath()));
        }
        if (item == null) {
            throw UNKNOWN_BOW.create(id);
        }
        return item;
    }

    @Nullable
    private static Item lookupBow(Identifier id) {
        if (!Registries.ITEM.containsId(id)) {
            return null;
        }
        Item item = Registries.ITEM.get(id);
        return item instanceof SimplyBowItem ? item : null;
    }

    private static List<Item> getUniqueBows() {
        if (cachedUniqueBows == null) {
            List<Item> bows = new ArrayList<>();
            for (Identifier id : Registries.ITEM.getIds()) {
                if (!SimplyBows.MOD_ID.equals(id.getNamespace())) {
                    continue;
                }
                Item item = Registries.ITEM.get(id);
                if (item instanceof SimplyBowItem) {
                    bows.add(item);
                }
            }
            cachedUniqueBows = bows;
        }
        return cachedUniqueBows;
    }
}
