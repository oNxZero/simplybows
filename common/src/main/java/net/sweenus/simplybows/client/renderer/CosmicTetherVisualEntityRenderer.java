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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.CosmicTetherVisualEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmicTetherVisualEntityRenderer extends EntityRenderer<CosmicTetherVisualEntity> {

    private static final Identifier NODE_FILL_SPRITE = new Identifier("minecraft", "block/white_concrete");
    private static final int POINTS = 9;
    private static final float JITTER_RADIUS = 0.30F;
    private static final float DRIFT_RADIUS = 0.08F;
    private static final float DRIFT_SPEED = 0.045F;
    private static final int COCOON_POINTS = 12;
    private static final float COCOON_RADIUS = 0.82F;
    private static final float COCOON_HEIGHT = 1.55F;
    private static final double COCOON_FOLLOW_LERP = 0.12;
    private static final double COCOON_FOLLOW_SNAP_DISTANCE_SQ = 16.0;

    private final Map<UUID, SmoothedCocoonCenter> smoothedCocoonCenters = new HashMap<>();

    public CosmicTetherVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CosmicTetherVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CosmicTetherVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        Vec3d renderOrigin = interpolatedPos(entity, tickDelta);
        Vec3d end = entity.getEndPos().subtract(renderOrigin);
        Vec3d tetherStart = Vec3d.ZERO;
        Vec3d tetherEnd = end;
        Vec3d cocoonCenter = end;
        if (entity.isCocoonMode()) {
            Vec3d snappedEnd = CosmicOrbitVisualEntityRenderer.snapToLatestFieldOrbitNode(
                    entity.getWorld(),
                    renderOrigin.add(end),
                    entity.getWorld().getTime()
            ).subtract(renderOrigin);
            Vec3d smoothedCenter = smoothedCocoonCenter(entity, renderOrigin).subtract(renderOrigin);
            end = snappedEnd;
            tetherStart = end;
            tetherEnd = smoothedCenter;
            cocoonCenter = smoothedCenter;
        }
        Vec3d tether = tetherEnd.subtract(tetherStart);
        Vec3d direction = tether.lengthSquared() > 1.0E-6 ? tether.normalize() : new Vec3d(0.0, 1.0, 0.0);
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 1.0E-6) {
            side = direction.crossProduct(new Vec3d(1.0, 0.0, 0.0));
        }
        side = side.normalize();
        Vec3d up = direction.crossProduct(side).normalize();

        float age = entity.age + tickDelta;
        Vec3d[] tetherPoints = new Vec3d[POINTS];
        tetherPoints[0] = tetherStart;
        for (int i = 1; i < POINTS; i++) {
            float t = i / (float) (POINTS - 1);
            float endpointFactor = i == POINTS - 1 ? 0.0F : 1.0F;
            float phase = entity.getId() * 0.73F + i * 1.91F;
            float driftA = (float) Math.sin(age * DRIFT_SPEED + phase) * DRIFT_RADIUS * endpointFactor;
            float driftB = (float) Math.cos(age * DRIFT_SPEED * 0.83F + phase * 1.37F) * DRIFT_RADIUS * endpointFactor;
            Vec3d drift = side.multiply(driftA).add(up.multiply(driftB));
            Vec3d point = tetherStart.lerp(tetherEnd, t).add(
                    jitter(entity.getId(), i, 0) * JITTER_RADIUS * endpointFactor + drift.x,
                    jitter(entity.getId(), i, 1) * JITTER_RADIUS * endpointFactor + drift.y,
                    jitter(entity.getId(), i, 2) * JITTER_RADIUS * endpointFactor + drift.z
            );
            tetherPoints[i] = point;
        }

        Vec3d[] cocoonPoints = entity.isCocoonMode() ? createCocoonPoints(entity, age, cocoonCenter) : null;

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        renderTetherLines(tetherPoints, matrices, lineConsumer);
        if (entity.isCocoonMode()) {
            renderCocoonLines(cocoonPoints, matrices, lineConsumer);
        }

        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);
        renderTetherNodes(tetherPoints, matrices, nodeConsumer, nodeSprite);
        if (entity.isCocoonMode()) {
            renderCocoonNodes(cocoonPoints, matrices, nodeConsumer, nodeSprite);
        }
    }

    private static void renderTetherLines(Vec3d[] points, MatrixStack matrices, VertexConsumer lineConsumer) {
        for (int i = 1; i < points.length; i++) {
            Vec3d previous = points[i - 1];
            Vec3d point = points[i];
            CosmicOrbitVisualEntityRenderer.renderLine(
                    matrices,
                    lineConsumer,
                    (float) previous.x, (float) previous.y, (float) previous.z,
                    (float) point.x, (float) point.y, (float) point.z,
                    0.82F
            );
        }
    }

    private static void renderTetherNodes(Vec3d[] points, MatrixStack matrices, VertexConsumer nodeConsumer, Sprite nodeSprite) {
        for (int i = 1; i < points.length; i++) {
            Vec3d point = points[i];
            CosmicOrbitVisualEntityRenderer.renderNodeDisc(
                    matrices,
                    nodeConsumer,
                    nodeSprite,
                    (float) point.x,
                    (float) point.y,
                    (float) point.z,
                    0.034F,
                    0.95F
            );
        }
    }

    private static Vec3d[] createCocoonPoints(CosmicTetherVisualEntity entity, float age, Vec3d end) {
        Vec3d[] points = new Vec3d[COCOON_POINTS];
        for (int i = 0; i < COCOON_POINTS; i++) {
            float layer = i / (float) (COCOON_POINTS - 1);
            double y = (layer - 0.5) * COCOON_HEIGHT;
            double ringRadius = COCOON_RADIUS * (0.45 + 0.55 * Math.sin(Math.PI * layer));
            double angle = age * 0.055 + i * 2.399 + entity.getId() * 0.17;
            double wobble = Math.sin(age * 0.037 + i * 1.41) * 0.07;
            points[i] = end.add(Math.cos(angle) * (ringRadius + wobble), y, Math.sin(angle) * (ringRadius + wobble));
        }
        return points;
    }

    private static void renderCocoonLines(Vec3d[] points, MatrixStack matrices, VertexConsumer lineConsumer) {
        for (int i = 0; i < COCOON_POINTS; i++) {
            Vec3d point = points[i];
            if (i > 0) {
                Vec3d previous = points[i - 1];
                CosmicOrbitVisualEntityRenderer.renderLine(
                        matrices,
                        lineConsumer,
                        (float) previous.x, (float) previous.y, (float) previous.z,
                        (float) point.x, (float) point.y, (float) point.z,
                        0.72F
                );
            }
            if (i > 2 && i % 3 == 0) {
                Vec3d cross = points[i - 3];
                CosmicOrbitVisualEntityRenderer.renderLine(
                        matrices,
                        lineConsumer,
                        (float) cross.x, (float) cross.y, (float) cross.z,
                        (float) point.x, (float) point.y, (float) point.z,
                        0.45F
                );
            }
        }
    }

    private static void renderCocoonNodes(Vec3d[] points, MatrixStack matrices, VertexConsumer nodeConsumer, Sprite nodeSprite) {
        for (Vec3d point : points) {
            CosmicOrbitVisualEntityRenderer.renderNodeDisc(
                    matrices,
                    nodeConsumer,
                    nodeSprite,
                    (float) point.x,
                    (float) point.y,
                    (float) point.z,
                    0.038F,
                    0.92F
            );
        }
    }

    private static float jitter(int seed, int index, int axis) {
        int mixed = seed * 31 + index * 131 + axis * 17;
        mixed ^= mixed >>> 13;
        mixed *= 0x5bd1e995;
        mixed ^= mixed >>> 15;
        return ((mixed & 0xFF) / 255.0F) - 0.5F;
    }

    private Vec3d smoothedCocoonCenter(CosmicTetherVisualEntity entity, Vec3d desiredCenter) {
        UUID entityId = entity.getUuid();
        long worldTick = entity.getWorld().getTime();
        smoothedCocoonCenters.entrySet().removeIf(entry -> entry.getValue().lastSeenTick + 80L < worldTick);

        SmoothedCocoonCenter previous = smoothedCocoonCenters.get(entityId);
        Vec3d previousPos = previous == null ? null : previous.pos;
        Vec3d smoothed = previousPos == null || previousPos.squaredDistanceTo(desiredCenter) > COCOON_FOLLOW_SNAP_DISTANCE_SQ
                ? desiredCenter
                : previousPos.lerp(desiredCenter, COCOON_FOLLOW_LERP);
        smoothedCocoonCenters.put(entityId, new SmoothedCocoonCenter(smoothed, worldTick));
        return smoothed;
    }

    private static Vec3d interpolatedPos(CosmicTetherVisualEntity entity, float tickDelta) {
        return new Vec3d(
                lerp(tickDelta, entity.prevX, entity.getX()),
                lerp(tickDelta, entity.prevY, entity.getY()),
                lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }

    private record SmoothedCocoonCenter(Vec3d pos, long lastSeenTick) {
    }
}
