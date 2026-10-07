package net.sweenus.simplybows.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.config.SimplyBowsConfig.VineBowSection;
import net.sweenus.simplybows.entity.VineFlowerVisualEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VineFlowerFieldManager {

    private static int fieldDurationTicks() { return SimplyBowsConfig.INSTANCE.everbloom.fieldDurationTicks.get(); }
    private static double fieldRadius() { return SimplyBowsConfig.INSTANCE.everbloom.fieldRadius.get(); }
    private static final double ATTRACTION_RADIUS = 14.0;
    private static final double PATCH_VISUAL_RADIUS = 2.35;
    private static final int PATCH_VISUAL_POINTS = 26;
    private static final double STRING_FIELD_RADIUS_BONUS_PER_LEVEL = 1.0;
    private static final double STRING_ATTRACTION_RADIUS_BONUS_PER_LEVEL = 2.2;
    private static final double STRING_VISUAL_RADIUS_BONUS_PER_LEVEL = 0.85;
    private static final int GROWTH_POINTS_PER_TICK = 6;
    private static final int RETRACT_POINTS_PER_TICK = 6;
    private static final int SPRING_ANIM_TICKS = 8;
    private static final int RETRACT_ANIM_TICKS = 10;
    private static final double SPRING_START_OFFSET_Y = -0.62;
    private static float friendlyHealBase() { return SimplyBowsConfig.INSTANCE.everbloom.friendlyHeal.get(); }
    private static float hostileDamageBase() { return SimplyBowsConfig.INSTANCE.everbloom.hostileDamage.get(); }
    private static float undeadBonusDamageBase() { return SimplyBowsConfig.INSTANCE.everbloom.undeadBonusDamage.get(); }
    private static final int GROUND_SCAN_UP = 5;
    private static final int GROUND_SCAN_DOWN = 18;
    private static final int FLOWER_TYPE_SHORT_GRASS = 0;
    private static final int FLOWER_TYPE_FERN = 1;
    private static final int FLOWER_TYPE_DANDELION = 2;
    private static final int FLOWER_TYPE_POPPY = 3;
    private static final int FLOWER_TYPE_CHERRY_LOG = 4;
    private static final int FLOWER_TYPE_CHERRY_LEAVES = 5;
    private static final int FLOWER_TYPE_SPORE_BLOSSOM = 6;
    private static final int FLOWER_TYPE_GLOW_LICHEN = 7;
    private static final int FLOWER_TYPE_GLOW_LICHEN_ACTIVE = 8;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST = 9;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST = 10;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH = 11;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH = 12;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST_ACTIVE = 13;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST_ACTIVE = 14;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH_ACTIVE = 15;
    private static final int FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH_ACTIVE = 16;
    private static final int MAX_VISUAL_POINTS = 180;
    private static final int FIELD_PULSE_INTERVAL_TICKS = 24;
    private static final int FIELD_PULSE_CUTOFF_TICKS = 80;
    private static final String FIELD_VISUAL_TAG = "simplybows_vine_field_visual";
    private static final int CHAOS_ROOT_SLOWNESS_AMPLIFIER = 10;
    private static final int CHAOS_LARGE_TARGET_SLOWNESS_AMPLIFIER = 1; // Slowness II
    private static final float CHAOS_ROOT_MAX_TARGET_WIDTH = 0.65F;
    private static final float CHAOS_ROOT_MAX_TARGET_HEIGHT = 2.0F;
    private static final Map<ServerWorld, List<ActiveFlowerField>> ACTIVE_FIELDS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> CHAOS_FIELD_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();
    private static final Map<MinecraftServer, Map<UUID, Long>> GRACE_FIELD_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private VineFlowerFieldManager() {
    }

    public static double flowerPatchRadius(int stringLevel) {
        double perString = SimplyBowsConfig.INSTANCE.upgrades.sizeMultiplierPerString.get();
        double sizeMultiplier = 1.0 + Math.max(0, stringLevel) * perString;
        return flowerPatchRadius(stringLevel, sizeMultiplier);
    }

    public static double flowerPatchRadiusPerString() {
        double perString = SimplyBowsConfig.INSTANCE.upgrades.sizeMultiplierPerString.get();
        return PATCH_VISUAL_RADIUS * perString + STRING_VISUAL_RADIUS_BONUS_PER_LEVEL;
    }

    private static double flowerPatchRadius(int stringLevel, double sizeMultiplier) {
        return PATCH_VISUAL_RADIUS * sizeMultiplier + Math.max(0, stringLevel) * STRING_VISUAL_RADIUS_BONUS_PER_LEVEL;
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveFlowerField> fields = ACTIVE_FIELDS.get(world);
        return (fields != null && !fields.isEmpty()) || !getCooldowns(world).isEmpty() || (world.getTime() % 20L == 0L);
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner) {
        createOrReplaceField(world, center, owner, BowUpgradeData.none());
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner, BowUpgradeData upgrades) {
        List<ActiveFlowerField> fields = ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        FieldTuning tuning = buildTuning(upgrades);

        if (tuning.chaosMode() && ownerId != null && !isChaosFieldReady(world, ownerId, fields)) {
            return;
        }
        if (tuning.cherryTreeVisual() && ownerId != null && !isGraceFieldReady(world, ownerId)) {
            return;
        }

        if (ownerId != null) {
            for (ActiveFlowerField field : fields) {
                if (ownerId.equals(field.ownerId()) && !field.retracting) {
                    beginRetract(world, field, false);
                }
            }
        }
        long baseDuration = tuning.chaosMode() ? tuning.chaosBaseDurationTicks() : fieldDurationTicks();
        long expiryTick = world.getTime() + baseDuration;

        if (tuning.chaosMode()) {
            ActiveFlowerField chaosField = new ActiveFlowerField(center, expiryTick, ownerId, tuning, (int) baseDuration);
            initializeChaosField(world, chaosField, upgrades);
            fields.add(chaosField);
            playChaosFieldCreationSound(world, center);
            spawnChaosCreationParticles(world, center, tuning);
            if (owner instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.everbloom.chaosCooldownTicks.get());
                int overlayTicks = (int) baseDuration + cooldownTicks;
                SimplyBowItem.simplybows$sendCooldownPacket(serverPlayer, "vine",
                        System.currentTimeMillis() + (long) overlayTicks * 50L, overlayTicks);
            }
            return;
        }

        List<FlowerPoint> pendingPoints = new ArrayList<>();
        if (tuning.cherryTreeVisual()) {
            pendingPoints.addAll(buildCherryTreeVisualPoints(world, center));
        }
        pendingPoints.addAll(buildPatchPoints(world, center, tuning.visualPoints(), tuning.visualRadius()));
        ActiveFlowerField field = new ActiveFlowerField(center, expiryTick, ownerId, tuning, (int) baseDuration);
        field.pendingPoints.addAll(pendingPoints);
        fields.add(field);
        playFieldCreationSound(world, center);
        spawnBurstParticles(world, center, tuning);
        if (tuning.cherryTreeVisual() && owner instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            int durationTicks = (int) baseDuration;
            startGraceCooldown(world, ownerId, durationTicks);
            SimplyBowItem.simplybows$sendCooldownPacket(serverPlayer, "vine",
                    System.currentTimeMillis() + (long) durationTicks * 50L, durationTicks);
        }
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center) {
        createOrReplaceField(world, center, null, BowUpgradeData.none());
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveFlowerField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanFieldVisuals(world);
            }
            return;
        }

        fields.removeIf(field -> {
            if (!field.retracting && world.getTime() >= field.effectiveExpiryTick()) {
                beginRetract(world, field, true);
            }
            return field.retracting && tickFieldRetract(world, field);
        });
        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
            return;
        }

        List<ActiveFlowerField> healingFields = new ArrayList<>();
        Set<UUID> regenerationRings = new HashSet<>();
        for (ActiveFlowerField field : fields) {
            if (field.retracting) {
                continue;
            }
            if (field.tuning().chaosMode()) {
                tickChaosField(world, field);
                continue;
            }

            growFieldVisuals(world, field);
            animateFieldVisuals(world, field);
            spawnAmbientParticles(world, field.center(), field.tuning());
            playFieldPulseSound(world, field);

            if (world.getTime() % 10L == 0L) {
                attractPassiveMobs(world, field.center(), field.tuning());
            }
            if (field.tuning().healFriendlies() && field.tuning().friendlyHeal() > 0.0F) {
                spawnRegenerationRings(world, field, regenerationRings);
                healingFields.add(field);
            }

            if (world.getTime() % field.tuning().auraIntervalTicks() == 0L) {
                applyAuraEffects(world, field);
            }
        }
        if (!healingFields.isEmpty()) {
            applyUnstackedHeals(world, healingFields);
        }
    }

    private static void initializeChaosField(ServerWorld world, ActiveFlowerField field, BowUpgradeData upgrades) {
        Vec3d center = field.center();
        double centerX = alignToBlockCenter(center.x);
        double centerZ = alignToBlockCenter(center.z);
        double groundY = findGroundTopY(world, centerX, centerZ, center.y) + 0.03;
        UUID coreId = spawnFlowerVisual(world, field, centerX, groundY, centerZ, FLOWER_TYPE_SPORE_BLOSSOM, true, 0.9F);
        field.chaosCoreVisualId = coreId;

        int tendrilCount = Math.max(1, field.tuning().chaosTendrilCount());
        int minNodes = Math.max(1, field.tuning().chaosNodesPerTendrilMin());
        int maxNodes = Math.max(minNodes, field.tuning().chaosNodesPerTendrilMax());
        double radius = field.tuning().fieldRadius();

        for (int tendril = 0; tendril < tendrilCount; tendril++) {
            double baseAngle = (Math.PI * 2.0 / tendrilCount) * tendril + (world.random.nextDouble() - 0.5) * 0.28;
            double curveStrength = 0.12 + world.random.nextDouble() * 0.16;
            double curveFrequency = 1.1 + world.random.nextDouble() * 0.5;
            double curveSign = world.random.nextBoolean() ? 1.0 : -1.0;
            int nodeCount = minNodes + world.random.nextInt(maxNodes - minNodes + 1);
            double maxReach = radius * (0.88 + world.random.nextDouble() * 0.08);
            double radialStep = maxReach / (nodeCount + 1.0);
            Vec3d previousPos = new Vec3d(centerX, groundY, centerZ);
            int segmentIndex = 0;

            for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
                double segment = nodeIndex + 1.0;
                double progress = segment / (nodeCount + 1.0);
                double angle = baseAngle + (Math.sin(progress * Math.PI * curveFrequency) * curveStrength * curveSign);
                double stepRadius = radialStep * segment;
                double x = alignToBlockCenter(centerX + Math.cos(angle) * stepRadius);
                double z = alignToBlockCenter(centerZ + Math.sin(angle) * stepRadius);
                double y = findGroundTopY(world, x, z, center.y) + 0.03;
                Vec3d nodePos = new Vec3d(x, y, z);

                segmentIndex = appendChaosConnectorNodes(world, field, tendril, previousPos, nodePos, segmentIndex);
                UUID nodeVisualId = spawnFlowerVisual(world, field, x, y, z, FLOWER_TYPE_GLOW_LICHEN, false, 0.75F);
                field.chaosNodes.add(new ChaosNode(nodePos, tendril, segmentIndex, nodeVisualId));
                segmentIndex++;
                previousPos = nodePos;
            }
        }
    }

    private static int appendChaosConnectorNodes(
            ServerWorld world,
            ActiveFlowerField field,
            int tendrilId,
            Vec3d from,
            Vec3d to,
            int segmentIndexStart
    ) {
        BlockPos fromBlock = BlockPos.ofFloored(from.x, from.y - 0.03, from.z);
        BlockPos toBlock = BlockPos.ofFloored(to.x, to.y - 0.03, to.z);
        int dx = toBlock.getX() - fromBlock.getX();
        int dy = toBlock.getY() - fromBlock.getY();
        int dz = toBlock.getZ() - fromBlock.getZ();
        int connectorCount = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
        if (connectorCount <= 1) {
            return segmentIndexStart;
        }

        Direction preferredFace = null;
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
            preferredFace = dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (dz != 0) {
            preferredFace = dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        int segmentIndex = segmentIndexStart;
        Direction lastSupportFace = null;
        BlockPos lastPlacedPos = null;
        BlockPos current = fromBlock;

        while (true) {
            Direction stepDirection = nextConnectorStep(current, toBlock);
            if (stepDirection == null) {
                break;
            }

            current = current.offset(stepDirection);
            if (current.equals(toBlock)) {
                break;
            }

            Direction preferred = lastSupportFace != null ? lastSupportFace : preferredFace;
            LichenPlacement placement = resolveLichenPlacement(
                    world,
                    current,
                    stepDirection,
                    fromBlock,
                    toBlock,
                    lastPlacedPos,
                    preferred
            );
            if (placement == null) {
                continue;
            }

            BlockPos lichenPos = placement.pos();
            Direction supportFace = placement.face();
            lastSupportFace = supportFace;
            lastPlacedPos = lichenPos;

            int flowerType = switch (supportFace) {
                case EAST -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST;
                case WEST -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST;
                case NORTH -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH;
                case SOUTH -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH;
                default -> FLOWER_TYPE_GLOW_LICHEN;
            };

            double x = lichenPos.getX() + 0.5;
            double y = lichenPos.getY() + 0.03;
            double z = lichenPos.getZ() + 0.5;
            UUID connectorId = spawnFlowerVisual(world, field, x, y, z, flowerType, false, 0.75F);
            field.chaosNodes.add(new ChaosNode(new Vec3d(x, y, z), tendrilId, segmentIndex, connectorId));
            segmentIndex++;
        }

        return segmentIndex;
    }

    private static LichenPlacement resolveLichenPlacement(
            ServerWorld world,
            BlockPos current,
            Direction stepDirection,
            BlockPos fromBlock,
            BlockPos toBlock,
            BlockPos lastPlacedPos,
            Direction preferredFace
    ) {
        List<BlockPos> candidates = new ArrayList<>();
        if (lastPlacedPos != null && (stepDirection == Direction.UP || stepDirection == Direction.DOWN)) {
            candidates.add(new BlockPos(lastPlacedPos.getX(), current.getY(), lastPlacedPos.getZ()));
        }
        candidates.add(current);
        if (stepDirection == Direction.UP || stepDirection == Direction.DOWN) {
            candidates.add(new BlockPos(fromBlock.getX(), current.getY(), fromBlock.getZ()));
            candidates.add(new BlockPos(toBlock.getX(), current.getY(), toBlock.getZ()));
        }

        for (BlockPos candidate : candidates) {
            Direction supportFace = resolveLichenSupportFace(world, candidate, preferredFace);
            if (supportFace != null) {
                return new LichenPlacement(candidate, supportFace);
            }
        }
        return null;
    }

    private static Direction nextConnectorStep(BlockPos current, BlockPos target) {
        int remY = target.getY() - current.getY();
        if (remY != 0) {
            return remY > 0 ? Direction.UP : Direction.DOWN;
        }

        int remX = target.getX() - current.getX();
        int remZ = target.getZ() - current.getZ();
        if (remX == 0 && remZ == 0) {
            return null;
        }
        if (Math.abs(remX) >= Math.abs(remZ) && remX != 0) {
            return remX > 0 ? Direction.EAST : Direction.WEST;
        }
        if (remZ != 0) {
            return remZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }

    private static Direction resolveLichenSupportFace(ServerWorld world, BlockPos lichenPos, Direction preferredFace) {
        if (preferredFace != null && hasLichenSupport(world, lichenPos, preferredFace)) {
            return preferredFace;
        }

        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (hasLichenSupport(world, lichenPos, dir)) {
                return dir;
            }
        }
        return null;
    }

    private static boolean hasLichenSupport(ServerWorld world, BlockPos lichenPos, Direction face) {
        BlockPos supportPos = lichenPos.offset(face);
        BlockState supportState = world.getBlockState(supportPos);
        return supportState.isSideSolidFullSquare(world, supportPos, face.getOpposite());
    }

    private static double alignToBlockCenter(double value) {
        return Math.floor(value) + 0.5;
    }

    private static void tickChaosField(ServerWorld world, ActiveFlowerField field) {
        Vec3d center = field.center();
        FieldTuning tuning = field.tuning();
        LivingEntity owner = getOwnerEntity(world, field.ownerId());
        long now = world.getTime();

        applyChaosRootMaintenance(world, field, now);
        updateChaosCoreVisual(world, field);
        updateChaosLichenGlowVisuals(world, field, now);
        spawnChaosAmbientParticles(world, center, tuning);

        Box box = Box.of(center, tuning.fieldRadius() * 2.0, 4.0, tuning.fieldRadius() * 2.0);
        boolean anyDrained = false;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!CombatTargeting.isOffensiveTargetCandidate(entity)) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(entity, owner)) {
                continue;
            }

            ChaosNode nearestNode = findNearestTriggeredNode(field, entity.getPos(), tuning.chaosNodeTriggerRadius());
            if (nearestNode == null) {
                continue;
            }

            Long nextTick = field.victimNextDrainTick.get(entity.getUuid());
            if (nextTick != null && now < nextTick) {
                continue;
            }

            float damage = Math.min(
                    tuning.chaosMaxDrainDamage(),
                    tuning.chaosBaseDrainDamage() + field.energyStacks * tuning.chaosDrainDamagePerEnergy()
            );
            CombatTargeting.applyDamage(world, owner, entity, damage, true, false);
            if (isLargeTendrilTarget(entity)) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, tuning.chaosRootDurationTicks(), CHAOS_LARGE_TARGET_SLOWNESS_AMPLIFIER), owner);
                field.rootedUntilTick.remove(entity.getUuid());
            } else {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, tuning.chaosRootDurationTicks(), CHAOS_ROOT_SLOWNESS_AMPLIFIER), owner);
                field.rootedUntilTick.put(entity.getUuid(), now + tuning.chaosRootDurationTicks());
            }
            field.victimNextDrainTick.put(entity.getUuid(), now + tuning.chaosDrainIntervalTicks());

            applyChaosEnergyGain(field);
            anyDrained = true;
            animateChaosEnergyPull(world, field, nearestNode, entity.getPos());
            spawnChaosDrainEffects(world, entity.getPos());
        }
        if (anyDrained) {
            syncChaosCooldownOverlay(world, field, owner);
        }
    }

    private static void syncChaosCooldownOverlay(ServerWorld world, ActiveFlowerField field, LivingEntity owner) {
        if (!(owner instanceof ServerPlayerEntity player)) {
            return;
        }

        int remainingFieldTicks = (int) Math.max(0L, field.effectiveExpiryTick() - world.getTime());
        int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.everbloom.chaosCooldownTicks.get());
        int remainingOverlayTicks = Math.max(1, remainingFieldTicks + cooldownTicks);

        long endMs = System.currentTimeMillis() + (long) remainingOverlayTicks * 50L;
        if (endMs <= field.lastSentCooldownEndMs) {
            return;
        }
        field.lastSentCooldownEndMs = endMs;
        SimplyBowItem.simplybows$sendCooldownPacket(player, "vine", endMs, remainingOverlayTicks);
    }

    private static void applyChaosRootMaintenance(ServerWorld world, ActiveFlowerField field, long now) {
        field.rootedUntilTick.entrySet().removeIf(entry -> {
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                return true;
            }
            if (now >= entry.getValue()) {
                return true;
            }
            living.setVelocity(0.0, Math.min(0.0, living.getVelocity().y), 0.0);
            living.velocityModified = true;
            return false;
        });
        field.victimNextDrainTick.entrySet().removeIf(entry -> {
            Entity entity = world.getEntity(entry.getKey());
            return !(entity instanceof LivingEntity living) || !living.isAlive();
        });
    }

    private static boolean isLargeTendrilTarget(LivingEntity entity) {
        return entity.getWidth() > CHAOS_ROOT_MAX_TARGET_WIDTH || entity.getHeight() > CHAOS_ROOT_MAX_TARGET_HEIGHT;
    }

    private static void updateChaosCoreVisual(ServerWorld world, ActiveFlowerField field) {
        if (field.chaosCoreVisualId == null) {
            return;
        }
        Entity entity = world.getEntity(field.chaosCoreVisualId);
        if (!(entity instanceof VineFlowerVisualEntity visual)) {
            return;
        }
        float scale = 1.0F + Math.min(field.tuning().chaosCoreMaxScaleBonus(), field.energyStacks * field.tuning().chaosCoreScalePerEnergy());
        visual.setHeightScale(scale);
        if (world.getTime() % 12L == 0L) {
            Vec3d center = field.center();
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.95 + scale * 0.4, center.z, 2, 0.18, 0.18, 0.18, 0.0);
        }
    }

    private static void spawnChaosAmbientParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.6, center.z, 2, 0.4, 0.22, 0.4, 0.0);
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, center.x, center.y + 0.55, center.z, 2, 0.35, 0.16, 0.35, 0.0);
        if (world.getTime() % 4L == 0L) {
            world.spawnParticles(ParticleTypes.GLOW, center.x, center.y + 0.2, center.z, 2, tuning.fieldRadius() * 0.36, 0.08, tuning.fieldRadius() * 0.36, 0.0);
        }
    }

    private static void spawnChaosDrainEffects(ServerWorld world, Vec3d targetPos) {
        world.spawnParticles(ParticleTypes.ENCHANT, targetPos.x, targetPos.y + 0.4, targetPos.z, 8, 0.25, 0.25, 0.25, 0.05);
        world.spawnParticles(ParticleTypes.GLOW, targetPos.x, targetPos.y + 0.2, targetPos.z, 6, 0.18, 0.12, 0.18, 0.01);
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.BLOCK_AZALEA_LEAVES_HIT, SoundCategory.PLAYERS, 0.8F, 1.05F + world.random.nextFloat() * 0.18F);
    }

    private static void animateChaosEnergyPull(ServerWorld world, ActiveFlowerField field, ChaosNode startNode, Vec3d targetPos) {
        Vec3d center = field.center();
        Vec3d from = targetPos.add(0.0, 0.35, 0.0);
        for (int i = field.chaosNodes.size() - 1; i >= 0; i--) {
            ChaosNode node = field.chaosNodes.get(i);
            if (node.tendrilId() != startNode.tendrilId() || node.segmentIndex() > startNode.segmentIndex()) {
                continue;
            }
            Vec3d to = node.pos().add(0.0, 0.08, 0.0);
            spawnLineParticles(world, from, to, 4, ParticleTypes.GLOW);
            field.activeLichenUntilTick.put(node.visualId(), world.getTime() + 40L);
            from = to;
        }
        spawnLineParticles(world, from, center.add(0.0, 0.8, 0.0), 8, ParticleTypes.ENCHANT);
    }

    private static void updateChaosLichenGlowVisuals(ServerWorld world, ActiveFlowerField field, long now) {
        field.activeLichenUntilTick.entrySet().removeIf(entry -> now >= entry.getValue());
        for (ChaosNode node : field.chaosNodes) {
            if (node.visualId() == null) {
                continue;
            }
            Entity entity = world.getEntity(node.visualId());
            if (!(entity instanceof VineFlowerVisualEntity visual)) {
                continue;
            }
            boolean active = field.activeLichenUntilTick.containsKey(node.visualId());
            int expectedType = active ? toActiveLichenType(visual.getFlowerType()) : toPassiveLichenType(visual.getFlowerType());
            if (visual.getFlowerType() != expectedType) {
                visual.setFlowerType(expectedType);
                if (active) {
                    Vec3d p = node.pos();
                    world.playSound(null, p.x, p.y, p.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.BLOCKS, 0.5F, 1.15F + world.random.nextFloat() * 0.18F);
                }
            }
        }
    }

    private static int toActiveLichenType(int type) {
        return switch (type) {
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST_ACTIVE;
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST_ACTIVE;
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH_ACTIVE;
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH_ACTIVE;
            default -> FLOWER_TYPE_GLOW_LICHEN_ACTIVE;
        };
    }

    private static int toPassiveLichenType(int type) {
        return switch (type) {
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST;
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST;
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH;
            case FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH_ACTIVE -> FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH;
            default -> FLOWER_TYPE_GLOW_LICHEN;
        };
    }

    private static boolean isLichenType(int type) {
        return type == FLOWER_TYPE_GLOW_LICHEN
                || type == FLOWER_TYPE_GLOW_LICHEN_ACTIVE
                || (type >= FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST && type <= FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH_ACTIVE);
    }

    private static void spawnLineParticles(ServerWorld world, Vec3d from, Vec3d to, int steps, net.minecraft.particle.ParticleEffect effect) {
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double x = MathHelper.lerp(t, from.x, to.x);
            double y = MathHelper.lerp(t, from.y, to.y);
            double z = MathHelper.lerp(t, from.z, to.z);
            world.spawnParticles(effect, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static ChaosNode findNearestTriggeredNode(ActiveFlowerField field, Vec3d pos, double triggerRadius) {
        double bestSq = triggerRadius * triggerRadius;
        ChaosNode best = null;
        for (ChaosNode node : field.chaosNodes) {
            double sq = node.pos().squaredDistanceTo(pos.x, node.pos().y, pos.z);
            if (sq <= bestSq) {
                bestSq = sq;
                best = node;
            }
        }
        return best;
    }

    private static void applyChaosEnergyGain(ActiveFlowerField field) {
        field.energyStacks++;
        long maxExtra = Math.max(0, field.tuning().chaosMaxDurationTicks() - field.baseDurationTicks);
        field.bonusDurationTicks = Math.min(maxExtra, field.bonusDurationTicks + field.tuning().chaosEnergyDurationExtendTicks());
    }

    private static void playChaosFieldCreationSound(ServerWorld world, Vec3d center) {
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_MOSS_PLACE, SoundCategory.BLOCKS, 1.0F, 0.9F + world.random.nextFloat() * 0.15F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AZALEA_LEAVES_HIT, SoundCategory.BLOCKS, 0.8F, 1.2F + world.random.nextFloat() * 0.15F);
    }

    private static void spawnChaosCreationParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.4, center.z, 20, 0.65, 0.35, 0.65, 0.0);
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 0.55, center.z, 16, 0.7, 0.28, 0.7, 0.0);
        world.spawnParticles(ParticleTypes.GLOW, center.x, center.y + 0.2, center.z, 12, tuning.fieldRadius() * 0.22, 0.12, tuning.fieldRadius() * 0.22, 0.0);
    }

    private static void attractPassiveMobs(ServerWorld world, Vec3d center, FieldTuning tuning) {
        Box box = Box.of(center, tuning.attractionRadius() * 2.0, 6.0, tuning.attractionRadius() * 2.0);
        for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class, box, LivingEntity::isAlive)) {
            if (animal.squaredDistanceTo(center) > tuning.attractionRadius() * tuning.attractionRadius()) {
                continue;
            }
            animal.getNavigation().startMovingTo(center.x, center.y, center.z, 1.2);
        }
    }

    private static void applyAuraEffects(ServerWorld world, ActiveFlowerField field) {
        Vec3d center = field.center();
        FieldTuning tuning = field.tuning();
        LivingEntity owner = getOwnerEntity(world, field.ownerId());
        Box box = Box.of(center, tuning.fieldRadius() * 2.0, 4.0, tuning.fieldRadius() * 2.0);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.squaredDistanceTo(center) > tuning.fieldRadius() * tuning.fieldRadius()) {
                continue;
            }

            if (isFlowerFieldMonster(entity)) {
                if (tuning.damageHostiles() && tuning.hostileDamage() > 0.0F) {
                    float damage = tuning.hostileDamage();
                    if (entity.getType().isIn(EntityTypeTags.UNDEAD)) {
                        damage += tuning.undeadBonusDamage();
                    }
                    boolean died = dealAuraDamage(world, owner, entity, damage);
                    if (died && tuning.bountyLootChance() > 0.0) {
                        trySpawnBountyLoot(world, entity, owner, tuning.bountyLootChance());
                    }
                }
                continue;
            }

            boolean directHeal = isFlowerFieldHealTarget(entity);
            boolean friendlyHeal = !directHeal && (owner == null || CombatTargeting.isFriendlyTo(entity, owner));
            if (!directHeal && !friendlyHeal) {
                continue;
            }

            if (tuning.cleanseNegativeEffects()) {
                if (cleanseNegativeEffects(entity)) {
                    world.playSound(
                            null,
                            entity.getX(),
                            entity.getBodyY(0.5),
                            entity.getZ(),
                            SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT,
                            SoundCategory.PLAYERS,
                            0.75F,
                            1.35F + world.getRandom().nextFloat() * 0.15F
                    );
                }
            }
        }
    }

    private static void applyUnstackedHeals(ServerWorld world, List<ActiveFlowerField> fields) {
        Map<UUID, FieldHeal> best = new HashMap<>();
        for (ActiveFlowerField field : fields) {
            FieldTuning tuning = field.tuning();
            LivingEntity owner = getOwnerEntity(world, field.ownerId());
            Vec3d center = field.center();
            for (LivingEntity entity : flowerFieldHealTargets(world, field, owner)) {
                boolean directHeal = isFlowerFieldHealTarget(entity);
                FieldHeal existing = best.get(entity.getUuid());
                if (existing == null || tuning.friendlyHeal() > existing.amount()) {
                    best.put(entity.getUuid(), new FieldHeal(entity, owner, tuning.friendlyHeal() / 20.0F, directHeal));
                }
            }
        }
        boolean showIntake = world.getTime() % 10L == 0L;
        for (FieldHeal heal : best.values()) {
            float before = heal.entity().getHealth();
            if (heal.direct()) {
                heal.entity().heal(heal.amount());
            } else {
                CombatTargeting.applyHealing(heal.owner(), heal.entity(), heal.amount());
            }
            if (showIntake && heal.entity().getHealth() > before) {
                spawnHealIntakeParticles(world, heal.entity());
            }
        }
    }

    private static void spawnRegenerationRings(ServerWorld world, ActiveFlowerField field, Set<UUID> alreadyShown) {
        LivingEntity owner = getOwnerEntity(world, field.ownerId());
        for (LivingEntity entity : flowerFieldHealTargets(world, field, owner)) {
            if (!alreadyShown.add(entity.getUuid())) {
                continue;
            }
            spawnRegenerationRing(world, entity);
        }
    }

    private static List<LivingEntity> flowerFieldHealTargets(ServerWorld world, ActiveFlowerField field, LivingEntity owner) {
        FieldTuning tuning = field.tuning();
        Vec3d center = field.center();
        double healRadius = tuning.visualRadius();
        Box box = Box.of(center, healRadius * 2.0, 6.0, healRadius * 2.0);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            if (dx * dx + dz * dz > healRadius * healRadius) {
                continue;
            }
            if (isFlowerFieldMonster(entity) || entity.getHealth() >= entity.getMaxHealth()) {
                continue;
            }
            boolean directHeal = isFlowerFieldHealTarget(entity);
            boolean friendlyHeal = !directHeal && (owner == null || CombatTargeting.isFriendlyTo(entity, owner));
            if (!directHeal && !friendlyHeal) {
                continue;
            }
            found.add(entity);
        }
        if (tuning.cherryTreeVisual() || found.isEmpty()) {
            return found;
        }
        LivingEntity lowest = null;
        for (LivingEntity entity : found) {
            if (lowest == null || entity.getHealth() < lowest.getHealth()) {
                lowest = entity;
            }
        }
        return lowest == null ? List.of() : List.of(lowest);
    }

    private static void spawnRegenerationRing(ServerWorld world, LivingEntity entity) {
        if ((world.getTime() & 1L) != 0L) {
            return;
        }
        long now = world.getTime();
        int points = 4;
        double spin = now * 0.22;
        double radius = Math.max(0.55, entity.getWidth() * 0.85);
        double baseY = entity.getBodyY(0.42);
        for (int i = 0; i < points; i++) {
            double angle = spin + (Math.PI * 2.0 / points) * i;
            double x = entity.getX() + Math.cos(angle) * radius;
            double z = entity.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.COMPOSTER, x, baseY, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void spawnHealIntakeParticles(ServerWorld world, LivingEntity entity) {
        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                entity.getX(),
                entity.getBodyY(0.75),
                entity.getZ(),
                3,
                0.2,
                0.25,
                0.2,
                0.01
        );
    }

    private static boolean isFlowerFieldMonster(LivingEntity entity) {
        return CombatTargeting.isMonsterFaction(entity);
    }

    private static boolean isFlowerFieldHealTarget(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            return !player.isSpectator();
        }
        return entity instanceof AnimalEntity || entity instanceof IronGolemEntity;
    }

    private static void spawnAmbientParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, center.x, center.y + 0.35, center.z, 2, 1.6, 0.2, 1.6, 0.0);
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.2, center.z, 2, 1.3, 0.1, 1.3, 0.0);
        spawnGlitterParticles(world, center, tuning);
        if (tuning.cherryTreeVisual() && world.getTime() % 2L == 0L) {
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 2.5, center.z, 2, 0.45, 0.6, 0.45, 0.0);
            if (world.getTime() % 6L == 0L) {
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 2.2, center.z, 1, 0.28, 0.4, 0.28, 0.0);
            }
        }
    }

    private static void spawnGlitterParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        if (world.getTime() % 3L != 0L) {
            return;
        }
        int count = world.random.nextInt(2) + 1;
        double radius = tuning.fieldRadius() * 0.92;
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(world.random.nextDouble()) * radius;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y + 0.45 + world.random.nextDouble() * 0.55;

            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.01);
            if (world.random.nextFloat() < 0.35F) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.04, z, 1, 0.0, 0.05, 0.0, 0.01);
            }
        }
    }

    private static void playFieldPulseSound(ServerWorld world, ActiveFlowerField field) {
        if (world.getTime() >= field.expiryTick() - FIELD_PULSE_CUTOFF_TICKS) {
            return;
        }
        if (world.getTime() % FIELD_PULSE_INTERVAL_TICKS != 0L) {
            return;
        }
        Vec3d center = field.center();
        float phase = (float) Math.sin((double) world.getTime() * 0.08);
        float pitch = 0.6F + phase * 0.15F + world.getRandom().nextFloat() * 0.06F;
        world.playSound(
                null,
                center.x,
                center.y + 0.25,
                center.z,
                SoundEvents.BLOCK_BEACON_AMBIENT,
                SoundCategory.PLAYERS,
                0.90F,
                pitch
        );
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.2, center.z, 10, 0.9, 0.15, 0.9, 0.0);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SHORT_GRASS.getDefaultState()), center.x, center.y + 0.1, center.z, 10, 0.9, 0.1, 0.9, 0.015);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DANDELION.getDefaultState()), center.x, center.y + 0.15, center.z, 4, 0.8, 0.12, 0.8, 0.015);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.POPPY.getDefaultState()), center.x, center.y + 0.15, center.z, 4, 0.8, 0.12, 0.8, 0.015);
        if (tuning.cherryTreeVisual()) {
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 1.5, center.z, 36, 0.9, 1.0, 0.9, 0.01);
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 1.2, center.z, 20, 0.8, 0.8, 0.8, 0.0);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.CHERRY_LEAVES.getDefaultState()), center.x, center.y + 1.0, center.z, 16, 0.75, 0.6, 0.75, 0.01);
        }
    }

    private static void playFieldCreationSound(ServerWorld world, Vec3d center) {
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_GRASS_PLACE,
                SoundCategory.BLOCKS,
                0.9F,
                0.95F + world.getRandom().nextFloat() * 0.2F
        );
    }

    private static List<FlowerPoint> buildPatchPoints(ServerWorld world, Vec3d center, int pointCount, double visualRadius) {
        List<FlowerPoint> points = new ArrayList<>();
        if (pointCount <= 0 || visualRadius <= 0.0) {
            return points;
        }
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < pointCount; i++) {
            double coverage = pointCount == 1 ? 0.0 : (double) i / (double) (pointCount - 1);
            double radius = visualRadius * Math.sqrt(coverage);
            double angle = i * goldenAngle;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, center.y) + 0.03;
            int flowerType;
            if (i % 4 == 0) {
                flowerType = FLOWER_TYPE_DANDELION;
            } else if (i % 5 == 0) {
                flowerType = FLOWER_TYPE_POPPY;
            } else if (i % 3 == 0) {
                flowerType = FLOWER_TYPE_FERN;
            } else {
                flowerType = FLOWER_TYPE_SHORT_GRASS;
            }
            points.add(new FlowerPoint(x, y, z, flowerType));
        }
        return points;
    }

    private static FieldTuning buildTuning(BowUpgradeData upgrades) {
        double sizeMultiplier = upgrades.sizeMultiplier();
        float frameMultiplier = (float) upgrades.damageMultiplier();
        int stringLevel = upgrades.stringLevel();
        RuneEtching rune = upgrades.runeEtching();

        float friendlyHeal = friendlyHealBase() * (1.0F + upgrades.frameLevel() * 0.6F) * 0.5F;
        float hostileDamage = hostileDamageBase() * frameMultiplier;
        float undeadBonusDamage = undeadBonusDamageBase() * frameMultiplier;
        boolean healFriendlies = true;
        boolean damageHostiles = true;
        boolean cleanseNegative = false;
        boolean cherryTreeVisual = false;
        double bountyLootChance = 0.0;
        int auraInterval = SimplyBowsConfig.INSTANCE.everbloom.auraIntervalTicks.get();

        if (rune == RuneEtching.PAIN) {
            healFriendlies = false;
            damageHostiles = true;
            auraInterval = SimplyBowsConfig.INSTANCE.everbloom.painAuraInterval.get();
        } else if (rune == RuneEtching.GRACE) {
            healFriendlies = true;
            damageHostiles = false;
            hostileDamage = 0.0F;
            undeadBonusDamage = 0.0F;
            cleanseNegative = true;
            cherryTreeVisual = true;
        } else if (rune == RuneEtching.BOUNTY) {
            bountyLootChance = SimplyBowsConfig.INSTANCE.everbloom.bountyLootChance.get();
        } else if (rune == RuneEtching.CHAOS) {
            healFriendlies = false;
            damageHostiles = false;
            friendlyHeal = 0.0F;
            hostileDamage = 0.0F;
            undeadBonusDamage = 0.0F;
        }

        double fieldRadius = fieldRadius() * sizeMultiplier + stringLevel * STRING_FIELD_RADIUS_BONUS_PER_LEVEL;
        int chaosBaseDurationTicks = 0;
        int chaosDurationPerFrameTicks = 0;
        int chaosDrainIntervalTicks = 0;
        int chaosRootDurationTicks = 0;
        double chaosNodeTriggerRadius = 0.0;
        float chaosBaseDrainDamage = 0.0F;
        float chaosDrainDamagePerEnergy = 0.0F;
        float chaosMaxDrainDamage = 0.0F;
        int chaosEnergyDurationExtendTicks = 0;
        int chaosMaxDurationTicks = 0;
        float chaosCoreScalePerEnergy = 0.0F;
        float chaosCoreMaxScaleBonus = 0.0F;
        int chaosTendrilCount = 0;
        int chaosNodesPerTendrilMin = 0;
        int chaosNodesPerTendrilMax = 0;
        double chaosBurstRadius = 0.0;
        int chaosBurstBaseBuffDuration = 0;
        int chaosBurstBuffDurationPerEnergy = 0;
        int chaosBurstEnergyPerAmplifier = 1;
        int chaosBurstMaxAmplifier = 0;

        if (rune == RuneEtching.CHAOS) {
            fieldRadius = SimplyBowsConfig.INSTANCE.everbloom.chaosBaseRadius.get() + stringLevel * SimplyBowsConfig.INSTANCE.everbloom.chaosRadiusPerString.get();
            chaosBaseDurationTicks = SimplyBowsConfig.INSTANCE.everbloom.chaosBaseDurationTicks.get();
            chaosDurationPerFrameTicks = SimplyBowsConfig.INSTANCE.everbloom.chaosDurationPerFrameTicks.get();
            chaosDrainIntervalTicks = SimplyBowsConfig.INSTANCE.everbloom.chaosDrainIntervalTicks.get();
            chaosRootDurationTicks = SimplyBowsConfig.INSTANCE.everbloom.chaosRootDurationTicks.get();
            chaosNodeTriggerRadius = SimplyBowsConfig.INSTANCE.everbloom.chaosNodeTriggerRadius.get();
            chaosBaseDrainDamage = SimplyBowsConfig.INSTANCE.everbloom.chaosBaseDrainDamage.get();
            chaosDrainDamagePerEnergy = SimplyBowsConfig.INSTANCE.everbloom.chaosDrainDamagePerEnergy.get();
            chaosMaxDrainDamage = SimplyBowsConfig.INSTANCE.everbloom.chaosMaxDrainDamage.get();
            chaosEnergyDurationExtendTicks = SimplyBowsConfig.INSTANCE.everbloom.chaosEnergyDurationExtendTicks.get();
            chaosMaxDurationTicks = SimplyBowsConfig.INSTANCE.everbloom.chaosMaxDurationTicks.get();
            chaosCoreScalePerEnergy = SimplyBowsConfig.INSTANCE.everbloom.chaosCoreScalePerEnergy.get();
            chaosCoreMaxScaleBonus = SimplyBowsConfig.INSTANCE.everbloom.chaosCoreMaxScaleBonus.get();
            chaosTendrilCount = SimplyBowsConfig.INSTANCE.everbloom.chaosTendrilCount.get();
            chaosNodesPerTendrilMin = SimplyBowsConfig.INSTANCE.everbloom.chaosNodesPerTendrilMin.get();
            chaosNodesPerTendrilMax = SimplyBowsConfig.INSTANCE.everbloom.chaosNodesPerTendrilMax.get();
            chaosBurstRadius = SimplyBowsConfig.INSTANCE.everbloom.chaosBurstRadius.get();
            chaosBurstBaseBuffDuration = SimplyBowsConfig.INSTANCE.everbloom.chaosBurstBaseBuffDuration.get();
            chaosBurstBuffDurationPerEnergy = SimplyBowsConfig.INSTANCE.everbloom.chaosBurstBuffDurationPerEnergy.get();
            chaosBurstEnergyPerAmplifier = SimplyBowsConfig.INSTANCE.everbloom.chaosBurstEnergyPerAmplifier.get();
            chaosBurstMaxAmplifier = SimplyBowsConfig.INSTANCE.everbloom.chaosBurstMaxAmplifier.get();
        }

        double attractionRadius = ATTRACTION_RADIUS * sizeMultiplier + stringLevel * STRING_ATTRACTION_RADIUS_BONUS_PER_LEVEL;
        double visualRadius = flowerPatchRadius(stringLevel, sizeMultiplier);
        double radiusRatio = visualRadius / PATCH_VISUAL_RADIUS;
        int visualPoints = Math.min(
                MAX_VISUAL_POINTS,
                Math.max(
                        PATCH_VISUAL_POINTS,
                        (int) Math.round(PATCH_VISUAL_POINTS * radiusRatio * radiusRatio) + upgrades.stringLevel() * 10
                )
        );
        return new FieldTuning(
                fieldRadius,
                attractionRadius,
                visualRadius,
                friendlyHeal,
                hostileDamage,
                undeadBonusDamage,
                healFriendlies,
                damageHostiles,
                cleanseNegative,
                cherryTreeVisual,
                bountyLootChance,
                Math.max(5, auraInterval),
                visualPoints,
                rune == RuneEtching.CHAOS,
                chaosBaseDurationTicks + upgrades.frameLevel() * chaosDurationPerFrameTicks,
                chaosDrainIntervalTicks,
                chaosRootDurationTicks,
                chaosNodeTriggerRadius,
                chaosBaseDrainDamage,
                chaosDrainDamagePerEnergy,
                chaosMaxDrainDamage,
                chaosEnergyDurationExtendTicks,
                chaosMaxDurationTicks,
                chaosCoreScalePerEnergy,
                chaosCoreMaxScaleBonus,
                chaosTendrilCount,
                chaosNodesPerTendrilMin,
                chaosNodesPerTendrilMax,
                chaosBurstRadius,
                chaosBurstBaseBuffDuration,
                chaosBurstBuffDurationPerEnergy,
                chaosBurstEnergyPerAmplifier,
                chaosBurstMaxAmplifier
        );
    }

    private static List<FlowerPoint> buildCherryTreeVisualPoints(ServerWorld world, Vec3d center) {
        List<FlowerPoint> points = new ArrayList<>();
        double baseY = findGroundTopY(world, center.x, center.z, center.y) + 0.03;

        // Trunk
        points.add(new FlowerPoint(center.x, baseY, center.z, FLOWER_TYPE_CHERRY_LOG));
        points.add(new FlowerPoint(center.x, baseY + 1.0, center.z, FLOWER_TYPE_CHERRY_LOG));
        points.add(new FlowerPoint(center.x, baseY + 2.0, center.z, FLOWER_TYPE_CHERRY_LOG));
        // Add leaf coverage at top trunk space so the trunk cap blends into canopy.
        points.add(new FlowerPoint(center.x, baseY + 2.0, center.z, FLOWER_TYPE_CHERRY_LEAVES));

        // Canopy ring
        for (int i = 0; i < 8; i++) {
            double angle = ((Math.PI * 2.0) / 8.0) * i;
            double x = center.x + Math.cos(angle) * 1.1;
            double z = center.z + Math.sin(angle) * 1.1;
            points.add(new FlowerPoint(x, baseY + 2.2, z, FLOWER_TYPE_CHERRY_LEAVES));
        }

        // Canopy top
        points.add(new FlowerPoint(center.x, baseY + 3.0, center.z, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x + 0.55, baseY + 2.9, center.z, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x - 0.55, baseY + 2.9, center.z, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x, baseY + 2.9, center.z + 0.55, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x, baseY + 2.9, center.z - 0.55, FLOWER_TYPE_CHERRY_LEAVES));
        return points;
    }

    private static boolean dealAuraDamage(ServerWorld world, LivingEntity owner, LivingEntity entity, float damage) {
        float before = entity.getHealth();
        boolean damaged = CombatTargeting.applyDamage(world, owner, entity, damage, true, false);
        return damaged && before > 0.0F && !entity.isAlive();
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean cleanseNegativeEffects(LivingEntity entity) {
        List<StatusEffectInstance> toRemove = new ArrayList<>();
        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            if (!instance.getEffectType().value().isBeneficial()) {
                toRemove.add(instance);
            }
        }
        if (toRemove.isEmpty()) {
            return false;
        }
        for (StatusEffectInstance instance : toRemove) {
            entity.removeStatusEffect(instance.getEffectType());
        }
        return true;
    }

    private static void trySpawnBountyLoot(ServerWorld world, LivingEntity entity, LivingEntity owner, double chance) {
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            return;
        }
        if (entity.getRandom().nextDouble() > chance) {
            return;
        }

        RegistryKey<LootTable> lootTableKey = entity.getLootTable();
        LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);
        DamageSource damageSource = createBountyDamageSource(world, owner);

        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.THIS_ENTITY, entity)
                .add(LootContextParameters.ORIGIN, entity.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, damageSource)
                .addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker())
                .addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());

        if (owner instanceof ServerPlayerEntity playerOwner) {
            builder = builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, playerOwner).luck(playerOwner.getLuck());
        }

        LootContextParameterSet lootContext = builder.build(LootContextTypes.ENTITY);
        List<ItemStack> generatedLoot = lootTable.generateLoot(lootContext, entity.getLootTableSeed() ^ world.getRandom().nextLong());
        if (generatedLoot.isEmpty()) {
            return;
        }

        List<ItemStack> nonEmptyLoot = generatedLoot.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .toList();
        if (nonEmptyLoot.isEmpty()) {
            return;
        }

        ItemStack extraDrop = nonEmptyLoot.get(world.getRandom().nextInt(nonEmptyLoot.size())).copy();
        entity.dropStack(extraDrop);
    }

    private static DamageSource createBountyDamageSource(ServerWorld world, LivingEntity owner) {
        if (owner instanceof ServerPlayerEntity playerOwner) {
            return world.getDamageSources().playerAttack(playerOwner);
        }
        if (owner != null) {
            return world.getDamageSources().mobAttack(owner);
        }
        return world.getDamageSources().magic();
    }

    private static void growFieldVisuals(ServerWorld world, ActiveFlowerField field) {
        if (field.spawnCursor >= field.pendingPoints.size()) {
            return;
        }

        int spawnCount = Math.min(GROWTH_POINTS_PER_TICK, field.pendingPoints.size() - field.spawnCursor);
        for (int i = 0; i < spawnCount; i++) {
            FlowerPoint point = field.pendingPoints.get(field.spawnCursor++);
            BlockState state = flowerTypeToBlockState(point.flowerType);
            UUID id = spawnFlowerVisual(world, field, point.x, point.y, point.z, point.flowerType);
            if (id != null) {
                field.springVisuals.add(new SpringVisual(id, point.x, point.y, point.z, world.getTime()));
            }
            // Brief upward burst so the patch feels like it's emerging from the ground.
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), point.x, point.y - 0.08, point.z, 3, 0.08, 0.02, 0.08, 0.03);
            world.spawnParticles(ParticleTypes.COMPOSTER, point.x, point.y + 0.04, point.z, 2, 0.06, 0.01, 0.06, 0.0);
        }
    }

    private static void animateFieldVisuals(ServerWorld world, ActiveFlowerField field) {
        if (field.springVisuals.isEmpty()) {
            return;
        }

        field.springVisuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof VineFlowerVisualEntity display)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            if (age >= SPRING_ANIM_TICKS) {
                display.setPos(visual.targetX(), visual.targetY(), visual.targetZ());
                display.setHeightScale(1.0F);
                return true;
            }

            float t = (float) age / (float) SPRING_ANIM_TICKS;
            double riseOffset = SPRING_START_OFFSET_Y * (1.0 - t) * (1.0 - t);
            double bounceOffset = Math.sin(t * Math.PI * 2.3) * 0.10 * (1.0 - t);
            double y = visual.targetY() + riseOffset + bounceOffset;
            display.setPos(visual.targetX(), y, visual.targetZ());
            display.setHeightScale(MathHelper.clamp(t * 1.05F, 0.0F, 1.0F));
            return false;
        });
    }

    private static UUID spawnFlowerVisual(ServerWorld world, ActiveFlowerField field, double x, double y, double z, int flowerType) {
        return spawnFlowerVisual(world, field, x, y, z, flowerType, false, 0.0F);
    }

    private static UUID spawnFlowerVisual(ServerWorld world, ActiveFlowerField field, double x, double y, double z, int flowerType, boolean upFacing, float initialHeightScale) {
        double spawnY = initialHeightScale <= 0.01F ? y + SPRING_START_OFFSET_Y : y;
        VineFlowerVisualEntity visual = new VineFlowerVisualEntity(world, x, spawnY, z, flowerType);
        if (upFacing || flowerType == FLOWER_TYPE_CHERRY_LOG || flowerType == FLOWER_TYPE_SPORE_BLOSSOM || isLichenType(flowerType)) {
            visual.setYaw(0.0F);
        } else {
            visual.setYaw(world.random.nextFloat() * 360.0F);
        }
        visual.setHeightScale(Math.max(0.0F, initialHeightScale));
        visual.addCommandTag(FIELD_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return null;
        }
        field.displayIds.add(visual.getUuid());
        field.placedVisuals.add(new PlacedVisual(visual.getUuid(), x, y, z));
        return visual.getUuid();
    }

    private static BlockState flowerTypeToBlockState(int flowerType) {
        return switch (flowerType) {
            case FLOWER_TYPE_DANDELION -> Blocks.DANDELION.getDefaultState();
            case FLOWER_TYPE_POPPY -> Blocks.POPPY.getDefaultState();
            case FLOWER_TYPE_FERN -> Blocks.FERN.getDefaultState();
            case FLOWER_TYPE_CHERRY_LOG -> Blocks.CHERRY_LOG.getDefaultState();
            case FLOWER_TYPE_CHERRY_LEAVES -> Blocks.CHERRY_LEAVES.getDefaultState();
            case FLOWER_TYPE_SPORE_BLOSSOM -> Blocks.SPORE_BLOSSOM.getDefaultState();
            case FLOWER_TYPE_GLOW_LICHEN, FLOWER_TYPE_GLOW_LICHEN_ACTIVE,
                 FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST,
                 FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH,
                 FLOWER_TYPE_GLOW_LICHEN_VERTICAL_EAST_ACTIVE, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_WEST_ACTIVE,
                 FLOWER_TYPE_GLOW_LICHEN_VERTICAL_NORTH_ACTIVE, FLOWER_TYPE_GLOW_LICHEN_VERTICAL_SOUTH_ACTIVE -> Blocks.GLOW_LICHEN.getDefaultState();
            default -> Blocks.SHORT_GRASS.getDefaultState();
        };
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }

        return centerY;
    }

    private static void removeField(ServerWorld world, ActiveFlowerField field) {
        if (field == null) {
            return;
        }

        for (UUID id : field.displayIds) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void beginRetract(ServerWorld world, ActiveFlowerField field, boolean expired) {
        if (field == null || field.retracting) {
            return;
        }
        field.retracting = true;
        field.retractCursor = field.placedVisuals.size();
        field.spawnCursor = field.pendingPoints.size();
        field.springVisuals.clear();
        if (expired && field.tuning().chaosMode()) {
            triggerChaosExpiryBurst(world, field);
            startChaosCooldown(world, field.ownerId());
        }
    }

    private static boolean tickFieldRetract(ServerWorld world, ActiveFlowerField field) {
        int startCount = Math.min(RETRACT_POINTS_PER_TICK, field.retractCursor);
        for (int i = 0; i < startCount; i++) {
            field.retractCursor--;
            PlacedVisual placed = field.placedVisuals.get(field.retractCursor);
            Entity entity = world.getEntity(placed.id());
            float startScale = 1.0F;
            double startY = placed.y();
            if (entity instanceof VineFlowerVisualEntity visual) {
                startScale = Math.max(0.05F, visual.getHeightScale());
                startY = visual.getY();
            }
            field.retractVisuals.add(new RetractVisual(
                    placed.id(),
                    placed.x(),
                    startY,
                    placed.y(),
                    placed.z(),
                    startScale,
                    world.getTime()
            ));
        }

        field.retractVisuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof VineFlowerVisualEntity display)) {
                return true;
            }
            long age = world.getTime() - visual.startTick();
            if (age >= RETRACT_ANIM_TICKS) {
                display.discard();
                return true;
            }
            float t = (float) age / (float) RETRACT_ANIM_TICKS;
            float ease = t * t;
            double endY = visual.groundY() + SPRING_START_OFFSET_Y;
            double y = visual.startY() + (endY - visual.startY()) * ease;
            display.setPos(visual.x(), y, visual.z());
            display.setHeightScale(MathHelper.clamp(visual.startScale() * (1.0F - ease), 0.0F, 1.0F));
            return false;
        });

        if (field.retractCursor <= 0 && field.retractVisuals.isEmpty()) {
            removeField(world, field);
            return true;
        }
        return false;
    }

    private static boolean isChaosFieldReady(ServerWorld world, UUID ownerId, List<ActiveFlowerField> fields) {
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        if (cooldownEnd != null && cooldownEnd > now) {
            return false;
        }

        for (ActiveFlowerField field : fields) {
            if (ownerId.equals(field.ownerId()) && field.tuning().chaosMode() && now < field.effectiveExpiryTick()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isChaosFieldReady(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return false;
        }
        List<ActiveFlowerField> fields = ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        return isChaosFieldReady(world, ownerId, fields);
    }

    private static boolean isGraceFieldReady(ServerWorld world, UUID ownerId) {
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = graceCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startGraceCooldown(ServerWorld world, UUID ownerId, int durationTicks) {
        if (ownerId == null) {
            return;
        }
        graceCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + Math.max(20, durationTicks));
    }

    private static Map<UUID, Long> graceCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(GRACE_FIELD_COOLDOWNS_BY_SERVER, world);
    }

    private static void startChaosCooldown(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        long now = CooldownStorage.currentTick(world);
        int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.everbloom.chaosCooldownTicks.get());
        getCooldowns(world).put(ownerId, now + cooldownTicks);
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(CHAOS_FIELD_COOLDOWNS_BY_SERVER, world);
    }

    private static void triggerChaosExpiryBurst(ServerWorld world, ActiveFlowerField field) {
        FieldTuning tuning = field.tuning();
        Vec3d center = field.center();
        LivingEntity owner = getOwnerEntity(world, field.ownerId());
        int amplifier = Math.min(tuning.chaosBurstMaxAmplifier(), field.energyStacks / Math.max(1, tuning.chaosBurstEnergyPerAmplifier()));
        int duration = tuning.chaosBurstBaseBuffDuration() + field.energyStacks * tuning.chaosBurstBuffDurationPerEnergy();

        Box box = Box.of(center, tuning.chaosBurstRadius() * 2.0, 5.0, tuning.chaosBurstRadius() * 2.0);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.squaredDistanceTo(center) > tuning.chaosBurstRadius() * tuning.chaosBurstRadius()) {
                continue;
            }
            if (owner == null || CombatTargeting.isFriendlyTo(entity, owner)) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, amplifier), owner);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, amplifier), owner);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier), owner);
            }
        }

        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.7, center.z, 46, tuning.chaosBurstRadius() * 0.35, 0.45, tuning.chaosBurstRadius() * 0.35, 0.0);
        world.spawnParticles(ParticleTypes.GLOW, center.x, center.y + 0.8, center.z, 36, tuning.chaosBurstRadius() * 0.32, 0.4, tuning.chaosBurstRadius() * 0.32, 0.0);
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 0.9, center.z, 40, tuning.chaosBurstRadius() * 0.36, 0.45, tuning.chaosBurstRadius() * 0.36, 0.0);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.0F, 0.85F + world.random.nextFloat() * 0.15F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_MOSS_BREAK, SoundCategory.PLAYERS, 0.9F, 1.1F + world.random.nextFloat() * 0.15F);
    }

    private static void purgeOrphanFieldVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof VineFlowerVisualEntity && entity.getCommandTags().contains(FIELD_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveFlowerField {
        private final Vec3d center;
        private final long expiryTick;
        private final UUID ownerId;
        private final FieldTuning tuning;
        private final List<FlowerPoint> pendingPoints = new ArrayList<>();
        private final List<UUID> displayIds = new ArrayList<>();
        private final List<SpringVisual> springVisuals = new ArrayList<>();
        private final List<PlacedVisual> placedVisuals = new ArrayList<>();
        private final List<RetractVisual> retractVisuals = new ArrayList<>();
        private boolean retracting;
        private int retractCursor;
        private final List<ChaosNode> chaosNodes = new ArrayList<>();
        private final Map<UUID, Long> victimNextDrainTick = new HashMap<>();
        private final Map<UUID, Long> rootedUntilTick = new HashMap<>();
        private final Map<UUID, Long> activeLichenUntilTick = new HashMap<>();
        private final int baseDurationTicks;
        private UUID chaosCoreVisualId;
        private int energyStacks;
        private long bonusDurationTicks;
        private long lastSentCooldownEndMs;
        private int spawnCursor;

        private ActiveFlowerField(Vec3d center, long expiryTick, UUID ownerId, FieldTuning tuning, int baseDurationTicks) {
            this.center = center;
            this.expiryTick = expiryTick;
            this.ownerId = ownerId;
            this.tuning = tuning;
            this.baseDurationTicks = baseDurationTicks;
        }

        private Vec3d center() {
            return this.center;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private long effectiveExpiryTick() {
            return this.expiryTick + this.bonusDurationTicks;
        }

        private UUID ownerId() {
            return this.ownerId;
        }

        private FieldTuning tuning() {
            return this.tuning;
        }
    }

    private record ChaosNode(Vec3d pos, int tendrilId, int segmentIndex, UUID visualId) {
    }

    private record LichenPlacement(BlockPos pos, Direction face) {
    }

    private record FlowerPoint(double x, double y, double z, int flowerType) {
    }

    private record SpringVisual(UUID id, double targetX, double targetY, double targetZ, long spawnTick) {
    }

    private record PlacedVisual(UUID id, double x, double y, double z) {
    }

    private record RetractVisual(UUID id, double x, double startY, double groundY, double z, float startScale, long startTick) {
    }

    private record FieldHeal(LivingEntity entity, LivingEntity owner, float amount, boolean direct) {
    }

    private record FieldTuning(
            double fieldRadius,
            double attractionRadius,
            double visualRadius,
            float friendlyHeal,
            float hostileDamage,
            float undeadBonusDamage,
            boolean healFriendlies,
            boolean damageHostiles,
            boolean cleanseNegativeEffects,
            boolean cherryTreeVisual,
            double bountyLootChance,
            int auraIntervalTicks,
            int visualPoints,
            boolean chaosMode,
            int chaosBaseDurationTicks,
            int chaosDrainIntervalTicks,
            int chaosRootDurationTicks,
            double chaosNodeTriggerRadius,
            float chaosBaseDrainDamage,
            float chaosDrainDamagePerEnergy,
            float chaosMaxDrainDamage,
            int chaosEnergyDurationExtendTicks,
            int chaosMaxDurationTicks,
            float chaosCoreScalePerEnergy,
            float chaosCoreMaxScaleBonus,
            int chaosTendrilCount,
            int chaosNodesPerTendrilMin,
            int chaosNodesPerTendrilMax,
            double chaosBurstRadius,
            int chaosBurstBaseBuffDuration,
            int chaosBurstBuffDurationPerEnergy,
            int chaosBurstEnergyPerAmplifier,
            int chaosBurstMaxAmplifier
    ) {
    }
}
