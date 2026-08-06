package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicArrowEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CosmicArrowEntityRenderer<T extends ArrowEntity> extends SimplyBowsArrowEntityRenderer<T> {

    private static final float NODE_RADIUS = 0.045F;
    private static final int NODE_SEGMENTS = 14;
    private static final Identifier NODE_FILL_SPRITE = new Identifier("minecraft", "block/white_concrete");

    private static final float NODE_R = 0.48F;
    private static final float NODE_G = 0.88F;
    private static final float NODE_B = 1.00F;

    private static final float LINE_R = 0.20F;
    private static final float LINE_G = 0.68F;
    private static final float LINE_B = 1.00F;
    private static final double NODE_DRIFT_RADIUS = 0.08;
    private static final double NODE_DRIFT_SPEED = 0.045;

    private static final Map<ArrowEntity, ConstellationTrail> ACTIVE_TRAILS = new IdentityHashMap<>();
    private static final List<ConstellationTrail> ORPHAN_TRAILS = new ArrayList<>();

    public CosmicArrowEntityRenderer(EntityRendererFactory.Context context, Identifier customTexture) {
        super(context, customTexture);
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity instanceof CosmicArrowEntity cosmicArrow) {
            updateTrail(cosmicArrow);
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        if (entity instanceof CosmicArrowEntity cosmicArrow) {
            renderConstellationTrail(cosmicArrow, tickDelta, matrices, vertexConsumers);
        }
    }

    private void updateTrail(CosmicArrowEntity arrow) {
        ConstellationTrail trail = ACTIVE_TRAILS.get(arrow);
        long worldTick = arrow.getWorld().getTime();

        if (arrow.isOnGround()) {
            if (trail != null) {
                trail.prune(worldTick);
                if (trail.getPoints().isEmpty()) {
                    ACTIVE_TRAILS.remove(arrow);
                }
            }
            return;
        }

        if (trail == null) {
            SimplyBowsConfig.CosmicBowSection cfg = SimplyBowsConfig.INSTANCE.cosmicBow;
            int trailDuration = cfg.trailDurationTicks.get();
            int trailLineDuration = cfg.trailLineDurationTicks.get();
            trail = new ConstellationTrail(
                    trailDuration,
                    trailLineDuration,
                    cfg.trailMaxConnectionDist.get(),
                    cfg.trailConnectionProbability.get(),
                    2
            );
            ACTIVE_TRAILS.put(arrow, trail);
        }

        if (arrow.age % trail.getSampleInterval() == 0) {
            trail.recordPoint(arrow.getPos(), worldTick);
        }
        trail.prune(worldTick);
    }

    private void renderConstellationTrail(CosmicArrowEntity arrow, float tickDelta,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        ConstellationTrail trail = ACTIVE_TRAILS.get(arrow);
        if (trail == null) return;

        long currentTick = arrow.getWorld().getTime();
        Vec3d renderPos = new Vec3d(
                lerp(tickDelta, arrow.prevX, arrow.getX()),
                lerp(tickDelta, arrow.prevY, arrow.getY()),
                lerp(tickDelta, arrow.prevZ, arrow.getZ())
        );

        renderConstellationTrail(trail, currentTick, renderPos, matrices, vertexConsumers);
        if (arrow.isBountyMode()) {
            renderBountyHeadNode(arrow, tickDelta, renderPos, matrices, vertexConsumers);
        }
    }

    public static void clientTick(long worldTick) {
        Iterator<Map.Entry<ArrowEntity, ConstellationTrail>> activeIterator = ACTIVE_TRAILS.entrySet().iterator();
        while (activeIterator.hasNext()) {
            Map.Entry<ArrowEntity, ConstellationTrail> entry = activeIterator.next();
            ArrowEntity arrow = entry.getKey();
            ConstellationTrail trail = entry.getValue();
            trail.prune(worldTick);

            if (trail.getPoints().isEmpty()) {
                activeIterator.remove();
            } else if (arrow.isRemoved()) {
                ORPHAN_TRAILS.add(trail);
                activeIterator.remove();
            }
        }

        ORPHAN_TRAILS.removeIf(trail -> {
            trail.prune(worldTick);
            return trail.getPoints().isEmpty();
        });
    }

    public static void renderOrphanTrails(long worldTick, Vec3d cameraPos,
                                          MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (ORPHAN_TRAILS.isEmpty()) {
            return;
        }

        for (ConstellationTrail trail : ORPHAN_TRAILS) {
            renderConstellationTrail(trail, worldTick, cameraPos, matrices, vertexConsumers);
        }
    }

    public static void clearTrails() {
        ACTIVE_TRAILS.clear();
        ORPHAN_TRAILS.clear();
    }

    private static void renderConstellationTrail(ConstellationTrail trail, long currentTick, Vec3d renderPos,
                                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        List<ConstellationTrail.TrailPoint> points = trail.getPoints();
        if (points.size() < 2) return;

        matrices.push();

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        for (int i = 0; i < points.size() - 1; i++) {
            renderTrailConnection(trail, points, currentTick, renderPos, matrices, lineConsumer, i, i + 1);
        }

        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);

        for (int i = 0; i < points.size(); i++) {
            float alpha = trail.getAlpha(i, currentTick);
            if (alpha < 0.02F) continue;

            ConstellationTrail.TrailPoint point = points.get(i);
            Vec3d driftedPos = driftedPointPos(point, currentTick);
            float x = (float) (driftedPos.x - renderPos.x);
            float y = (float) (driftedPos.y - renderPos.y);
            float z = (float) (driftedPos.z - renderPos.z);

            float visibleAlpha = Math.min(1.0F, alpha * 1.3F);
            float radius = NODE_RADIUS * (0.25F + visibleAlpha * 0.75F);
            renderNodeDisc(matrices, nodeConsumer, nodeSprite, x, y, z, radius, visibleAlpha);
        }

        matrices.pop();
    }

    private static void renderBountyHeadNode(CosmicArrowEntity arrow, float tickDelta, Vec3d renderPos,
                                             MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        float maxCharge = Math.max(1.0F, SimplyBowsConfig.INSTANCE.cosmicBow.bountyMaxChargeTicks.get());
        float charge = Math.min(1.0F, (arrow.getBountyChargeTicks() + tickDelta) / maxCharge);
        float pulse = (float) Math.sin((arrow.age + tickDelta) * 0.45F) * 0.5F + 0.5F;
        float radius = NODE_RADIUS * (4.0F + charge * 30.0F + pulse * charge * 2.4F);
        float r = 0.48F + charge * 0.52F;
        float g = 0.88F - charge * 0.20F;
        float b = 1.00F - charge * 0.88F;
        Vec3d arrowPos = new Vec3d(
                lerp(tickDelta, arrow.prevX, arrow.getX()),
                lerp(tickDelta, arrow.prevY, arrow.getY()),
                lerp(tickDelta, arrow.prevZ, arrow.getZ())
        );

        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);
        renderNodeDisc(
                matrices,
                nodeConsumer,
                nodeSprite,
                (float) (arrowPos.x - renderPos.x),
                (float) (arrowPos.y - renderPos.y),
                (float) (arrowPos.z - renderPos.z),
                radius,
                r,
                g,
                b,
                0.88F
        );
    }

    private static void renderTrailConnection(ConstellationTrail trail,
                                              List<ConstellationTrail.TrailPoint> points,
                                              long currentTick,
                                              Vec3d renderPos,
                                              MatrixStack matrices,
                                              VertexConsumer lineConsumer,
                                              int indexA,
                                              int indexB) {
        float alphaA = trail.getLineAlpha(indexA, currentTick);
        float alphaB = trail.getLineAlpha(indexB, currentTick);
        float alpha = Math.min(alphaA, alphaB);
        if (alpha < 0.02F) return;

        ConstellationTrail.TrailPoint a = points.get(indexA);
        ConstellationTrail.TrailPoint b = points.get(indexB);
        Vec3d driftedA = driftedPointPos(a, currentTick);
        Vec3d driftedB = driftedPointPos(b, currentTick);

        renderLine(
                matrices, lineConsumer,
                (float) (driftedA.x - renderPos.x), (float) (driftedA.y - renderPos.y), (float) (driftedA.z - renderPos.z),
                (float) (driftedB.x - renderPos.x), (float) (driftedB.y - renderPos.y), (float) (driftedB.z - renderPos.z),
                LINE_R, LINE_G, LINE_B, Math.min(1.0F, alpha * 1.15F)
        );
    }

    private static Vec3d driftedPointPos(ConstellationTrail.TrailPoint point, long currentTick) {
        double phase = (point.seed & 0xFFFFL) * 0.0001 + point.birthTick * 0.17;
        double age = currentTick + phase;
        double driftX = Math.sin(age * NODE_DRIFT_SPEED + phase) * NODE_DRIFT_RADIUS;
        double driftY = Math.cos(age * NODE_DRIFT_SPEED * 0.83 + phase * 1.37) * NODE_DRIFT_RADIUS;
        double driftZ = Math.sin(age * NODE_DRIFT_SPEED * 0.71 + phase * 0.61) * NODE_DRIFT_RADIUS;
        return point.pos.add(driftX, driftY, driftZ);
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }

    private static void renderNodeDisc(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
                                       float x, float y, float z, float radius, float alpha) {
        renderNodeDisc(matrices, consumer, sprite, x, y, z, radius, NODE_R, NODE_G, NODE_B, alpha);
    }

    private static void renderNodeDisc(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
                                       float x, float y, float z, float radius,
                                       float r, float g, float b, float alpha) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -MinecraftClient.getInstance().gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                MinecraftClient.getInstance().gameRenderer.getCamera().getPitch()));

        MatrixStack.Entry entry = matrices.peek();
        float u = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float v = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;
        for (int i = 0; i < NODE_SEGMENTS; i++) {
            double angleA = Math.PI * 2.0 * i / NODE_SEGMENTS;
            double angleB = Math.PI * 2.0 * (i + 1) / NODE_SEGMENTS;
            double angleMid = (angleA + angleB) * 0.5;
            emitNodeWedge(
                    consumer,
                    entry,
                    0.0F,
                    0.0F,
                    (float) Math.cos(angleA) * radius,
                    (float) Math.sin(angleA) * radius,
                    (float) Math.cos(angleMid) * radius,
                    (float) Math.sin(angleMid) * radius,
                    (float) Math.cos(angleB) * radius,
                    (float) Math.sin(angleB) * radius,
                    u,
                    v,
                    r,
                    g,
                    b,
                    alpha
            );
        }

        matrices.pop();
    }

    private static void emitNodeWedge(VertexConsumer consumer, MatrixStack.Entry entry,
                                      float centerX, float centerY,
                                      float x1, float y1,
                                      float xMid, float yMid,
                                      float x2, float y2,
                                      float u, float v,
                                      float r, float g, float b,
                                      float alpha) {
        nodeVertex(consumer, entry, centerX, centerY, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, centerX, centerY, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, r, g, b, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, r, g, b, alpha);
    }

    private static void nodeVertex(VertexConsumer consumer, MatrixStack.Entry entry,
                                   float x, float y, float u, float v,
                                   float r, float g, float b, float alpha) {
        consumer.vertex(entry.getPositionMatrix(), x, y, 0.0F)
                .color(r, g, b, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry.getNormalMatrix(), 0.0F, 0.0F, 1.0F)
                .next();
    }

    private static void renderLine(MatrixStack matrices, VertexConsumer consumer,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float r, float g, float b, float alpha) {
        MatrixStack.Entry entry = matrices.peek();

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001F) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, alpha).normal(entry.getNormalMatrix(), nx, ny, nz)
                .next();
        consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, alpha).normal(entry.getNormalMatrix(), nx, ny, nz)
                .next();
    }
}
