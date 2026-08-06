package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.CosmicStrikeVisualEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CosmicStrikeVisualEntityRenderer extends EntityRenderer<CosmicStrikeVisualEntity> {

    private static final Identifier NODE_FILL_SPRITE = new Identifier("minecraft", "block/white_concrete");
    private static final int MIN_POINTS = 6;
    private static final int MAX_POINTS = 48;
    private static final float JITTER_RADIUS = 0.62F;
    private static final float PASSIVE_JITTER_RADIUS = 0.24F;
    private static final int PASSIVE_REVEAL_INTERVAL_TICKS = 8;
    private static final int PASSIVE_FADE_IN_TICKS = 18;
    private static final int PASSIVE_LINE_FADE_IN_TICKS = 10;
    private static final int PASSIVE_LINE_FADE_OUT_TICKS = 20;
    private static final int PASSIVE_LINE_EARLY_EXPIRE_TICKS = 18;
    private static final int PASSIVE_FADE_OUT_TICKS = 28;
    private static final float PASSIVE_NODE_RADIUS = 0.03825F;
    private static final float PASSIVE_NODE_OPACITY = 0.40F;
    private static final double PASSIVE_NODE_DRIFT_RADIUS = 0.08;
    private static final double PASSIVE_NODE_DRIFT_SPEED = 0.045;
    private static final double PASSIVE_PLAYER_HORIZONTAL_RADIUS = 0.72;
    private static final double PASSIVE_EXTRA_CONNECTION_DISTANCE_SQ = 1.35 * 1.35;
    private static final float PASSIVE_EXTRA_CONNECTION_CHANCE = 0.45F;

    private final Map<CosmicStrikeVisualEntity, List<Vec3d>> passivePoints = new WeakHashMap<>();

    public CosmicStrikeVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CosmicStrikeVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CosmicStrikeVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float alpha = entity.isPassiveMode()
                ? passiveAlpha(age, entity.getLifetimeTicks())
                : Math.max(0.0F, 1.0F - age / entity.getLifetimeTicks());
        if (alpha <= 0.02F) {
            return;
        }

        Vec3d end = entity.getEndPos().subtract(entity.getPos());
        int requestedPoints = entity.getPointCount();
        int totalPoints = requestedPoints > 0
                ? Math.max(2, Math.min(MAX_POINTS, requestedPoints))
                : Math.max(MIN_POINTS, Math.min(MAX_POINTS, (int) Math.ceil(end.length() / 1.1) + 1));
        int points = totalPoints;
        if (entity.isPassiveMode()) {
            points = Math.min(totalPoints, 1 + Math.max(0, entity.age) / PASSIVE_REVEAL_INTERVAL_TICKS);
        }
        if (points <= 0) {
            return;
        }
        if (entity.isPassiveMode()) {
            renderPassiveTrail(entity, tickDelta, age, points, alpha, vertexConsumers, matrices);
            return;
        }

        List<Vec3d> trailPoints = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            trailPoints.add(getPoint(entity, end, totalPoints, i));
        }

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        for (int i = 1; i < trailPoints.size(); i++) {
            Vec3d previous = trailPoints.get(i - 1);
            Vec3d current = trailPoints.get(i);
            CosmicOrbitVisualEntityRenderer.renderLine(
                    matrices, lineConsumer,
                    (float) previous.x, (float) previous.y, (float) previous.z,
                    (float) current.x, (float) current.y, (float) current.z,
                    alpha
            );
        }

        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = getNodeSprite();
        for (Vec3d point : trailPoints) {
            CosmicOrbitVisualEntityRenderer.renderNodeDisc(
                    matrices, nodeConsumer, nodeSprite,
                    (float) point.x, (float) point.y, (float) point.z,
                    0.045F * alpha,
                    alpha
            );
        }
    }

    private void renderPassiveTrail(CosmicStrikeVisualEntity entity, float tickDelta, float age, int visiblePoints, float alpha,
                                    VertexConsumerProvider vertexConsumers, MatrixStack matrices) {
        Vec3d renderPos = new Vec3d(
                lerp(tickDelta, entity.prevX, entity.getX()),
                lerp(tickDelta, entity.prevY, entity.getY()),
                lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        List<Vec3d> points = passivePoints.computeIfAbsent(entity, ignored -> new ArrayList<>());
        while (points.size() < visiblePoints) {
            points.add(passiveSpawnPoint(entity, renderPos, points.size(), tickDelta));
        }

        int renderedPoints = Math.min(visiblePoints, points.size());
        if (renderedPoints <= 0) {
            return;
        }

        List<Vec3d> driftedPoints = new ArrayList<>(renderedPoints);
        for (int i = 0; i < renderedPoints; i++) {
            driftedPoints.add(driftPassivePoint(entity, points.get(i), i).subtract(renderPos));
        }

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        for (int i = 1; i < renderedPoints; i++) {
            Vec3d previous = driftedPoints.get(i - 1);
            Vec3d current = driftedPoints.get(i);
            float lineAlpha = alpha * passiveLineAlpha(age, i, entity.getLifetimeTicks());
            CosmicOrbitVisualEntityRenderer.renderLine(
                    matrices, lineConsumer,
                    (float) previous.x, (float) previous.y, (float) previous.z,
                    (float) current.x, (float) current.y, (float) current.z,
                    lineAlpha
            );

            int extraConnection = findPassiveExtraConnection(entity, points, i);
            if (extraConnection >= 0) {
                Vec3d extra = driftedPoints.get(extraConnection);
                CosmicOrbitVisualEntityRenderer.renderLine(
                        matrices, lineConsumer,
                        (float) extra.x, (float) extra.y, (float) extra.z,
                        (float) current.x, (float) current.y, (float) current.z,
                        lineAlpha * 0.85F
                );
            }
        }

        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = getNodeSprite();
        float nodeAlpha = alpha * PASSIVE_NODE_OPACITY;
        for (Vec3d point : driftedPoints) {
            CosmicOrbitVisualEntityRenderer.renderNodeDisc(
                    matrices, nodeConsumer, nodeSprite,
                    (float) point.x, (float) point.y, (float) point.z,
                    PASSIVE_NODE_RADIUS * alpha,
                    nodeAlpha
            );
        }
    }

    private static Sprite getNodeSprite() {
        return MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);
    }

    private static Vec3d passiveSpawnPoint(CosmicStrikeVisualEntity entity, Vec3d renderPos, int index, float tickDelta) {
        Entity owner = entity.getWorld().getEntityById(entity.getPassiveOwnerId());
        if (owner instanceof PlayerEntity player) {
            Box box = player.getBoundingBox();
            double angle = seededUnit(entity.getId(), index, 0) * Math.PI * 2.0;
            double radius = Math.sqrt(seededUnit(entity.getId(), index, 1)) * PASSIVE_PLAYER_HORIZONTAL_RADIUS;
            double y = box.minY + seededUnit(entity.getId(), index, 2) * Math.max(0.2, box.maxY - box.minY);
            return new Vec3d(
                    lerp(tickDelta, player.prevX, player.getX()) + Math.cos(angle) * radius,
                    y,
                    lerp(tickDelta, player.prevZ, player.getZ()) + Math.sin(angle) * radius
            );
        }
        return renderPos.add(
                jitter(entity.getId(), index, 0) * PASSIVE_PLAYER_HORIZONTAL_RADIUS,
                jitter(entity.getId(), index, 1),
                jitter(entity.getId(), index, 2) * PASSIVE_PLAYER_HORIZONTAL_RADIUS
        );
    }

    private static Vec3d driftPassivePoint(CosmicStrikeVisualEntity entity, Vec3d point, int index) {
        double phase = entity.getId() * 0.37 + index * 1.91;
        double age = entity.age + phase;
        return point.add(
                Math.sin(age * PASSIVE_NODE_DRIFT_SPEED + phase) * PASSIVE_NODE_DRIFT_RADIUS,
                Math.cos(age * PASSIVE_NODE_DRIFT_SPEED * 0.83 + phase * 1.37) * PASSIVE_NODE_DRIFT_RADIUS,
                Math.sin(age * PASSIVE_NODE_DRIFT_SPEED * 0.71 + phase * 0.61) * PASSIVE_NODE_DRIFT_RADIUS
        );
    }

    private static int findPassiveExtraConnection(CosmicStrikeVisualEntity entity, List<Vec3d> points, int index) {
        if (index < 2 || seededChance(entity.getId(), index, 97) > PASSIVE_EXTRA_CONNECTION_CHANCE) {
            return -1;
        }

        Vec3d current = points.get(index);
        int bestIndex = -1;
        double bestDistanceSq = PASSIVE_EXTRA_CONNECTION_DISTANCE_SQ;
        for (int i = 0; i < index - 1; i++) {
            double distanceSq = current.squaredDistanceTo(points.get(i));
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int findPassiveExtraConnection(CosmicStrikeVisualEntity entity, Vec3d end, int totalPoints, int index, Vec3d current) {
        if (index < 2 || seededChance(entity.getId(), index, 97) > PASSIVE_EXTRA_CONNECTION_CHANCE) {
            return -1;
        }

        int bestIndex = -1;
        double bestDistanceSq = PASSIVE_EXTRA_CONNECTION_DISTANCE_SQ;
        for (int i = 0; i < index - 1; i++) {
            Vec3d candidate = getPoint(entity, end, totalPoints, i);
            double distanceSq = current.squaredDistanceTo(candidate);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static float passiveAlpha(float age, int lifetimeTicks) {
        float fadeIn = Math.min(1.0F, age / PASSIVE_FADE_IN_TICKS);
        float fadeOut = Math.min(1.0F, Math.max(0.0F, (lifetimeTicks - age) / PASSIVE_FADE_OUT_TICKS));
        return Math.max(0.0F, Math.min(fadeIn, fadeOut));
    }

    private static float passiveLineAlpha(float age, int nodeIndex, int lifetimeTicks) {
        float connectionAge = age - nodeIndex * PASSIVE_REVEAL_INTERVAL_TICKS;
        float fadeIn = Math.max(0.0F, Math.min(1.0F, connectionAge / PASSIVE_LINE_FADE_IN_TICKS));
        float fadeOut = Math.max(0.0F, Math.min(1.0F,
                (lifetimeTicks - PASSIVE_LINE_EARLY_EXPIRE_TICKS - age) / PASSIVE_LINE_FADE_OUT_TICKS));
        return Math.min(fadeIn, fadeOut);
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }

    private static Vec3d getPoint(CosmicStrikeVisualEntity entity, Vec3d end, int points, int index) {
        float t = points <= 1 ? 0.0F : index / (float) (points - 1);
        float endpointFactor = index == 0 || index == points - 1 ? 0.0F : 1.0F;
        double jitterRadius = entity.isPassiveMode() ? PASSIVE_JITTER_RADIUS : JITTER_RADIUS;
        Vec3d point = new Vec3d(
                end.x * t + jitter(entity.getId(), index, 0) * jitterRadius * endpointFactor,
                end.y * t + jitter(entity.getId(), index, 1) * jitterRadius * endpointFactor,
                end.z * t + jitter(entity.getId(), index, 2) * jitterRadius * endpointFactor
        );
        if (!entity.isPassiveMode()) {
            return point;
        }

        double phase = entity.getId() * 0.37 + index * 1.91;
        double age = entity.age + phase;
        return point.add(
                Math.sin(age * PASSIVE_NODE_DRIFT_SPEED + phase) * PASSIVE_NODE_DRIFT_RADIUS,
                Math.cos(age * PASSIVE_NODE_DRIFT_SPEED * 0.83 + phase * 1.37) * PASSIVE_NODE_DRIFT_RADIUS,
                Math.sin(age * PASSIVE_NODE_DRIFT_SPEED * 0.71 + phase * 0.61) * PASSIVE_NODE_DRIFT_RADIUS
        );
    }

    private static float jitter(int seed, int index, int axis) {
        int mixed = seed * 31 + index * 131 + axis * 17;
        mixed ^= mixed >>> 13;
        mixed *= 0x5bd1e995;
        mixed ^= mixed >>> 15;
        return ((mixed & 0xFF) / 255.0F) - 0.5F;
    }

    private static float seededChance(int seed, int index, int salt) {
        int mixed = seed * 47 + index * 193 + salt * 23;
        mixed ^= mixed >>> 13;
        mixed *= 0x5bd1e995;
        mixed ^= mixed >>> 15;
        return (mixed & 0xFFFF) / 65535.0F;
    }

    private static double seededUnit(int seed, int index, int salt) {
        return seededChance(seed, index, salt);
    }
}
