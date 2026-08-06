package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicBountyVisualEntity;

public class CosmicBountyVisualEntityRenderer extends EntityRenderer<CosmicBountyVisualEntity> {

    private static final Identifier NODE_FILL_SPRITE = new Identifier("minecraft", "block/white_concrete");
    private static final int NODE_SEGMENTS = 18;
    private static final int STAR_POINTS = 5;
    private static final int EXPANDING_ORBITS = 3;
    private static final int ORBIT_TRAIL_POINTS = 18;
    private static final float SPIRAL_NODE_RADIUS = 0.045F;
    private static final float TRAIL_NODE_R = 0.48F;
    private static final float TRAIL_NODE_G = 0.88F;
    private static final float TRAIL_NODE_B = 1.00F;
    private static final float TRAIL_LINE_R = 0.20F;
    private static final float TRAIL_LINE_G = 0.68F;
    private static final float TRAIL_LINE_B = 1.00F;

    public CosmicBountyVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CosmicBountyVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CosmicBountyVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float charge = Math.min(1.0F, entity.getChargeTicks() / (float) Math.max(1, SimplyBowsConfig.INSTANCE.cosmicBow.bountyMaxChargeTicks.get()));
        float implode = Math.min(1.0F, (entity.getImplodeTicks() + tickDelta) / Math.max(1.0F, entity.getMaxImplodeTicks()));
        float burstAge = Math.max(0.0F, entity.getImplodeTicks() + tickDelta - entity.getMaxImplodeTicks());
        if (entity.hasExploded()) {
            renderSpiralBurst(entity, charge, burstAge, matrices, vertexConsumers);
            return;
        }
        float pulse = (float) Math.sin((entity.age + tickDelta) * 0.55F) * 0.5F + 0.5F;
        float radius = (1.05F + charge * 3.15F) * (1.0F - implode * 0.30F) + pulse * 0.12F;
        float alpha = Math.max(0.15F, 1.0F - implode * 0.18F);

        Sprite nodeSprite = getNodeSprite();

        float orangeBlend = 0.35F + charge * 0.65F;
        float r = 0.62F + orangeBlend * 0.38F;
        float g = 0.88F - orangeBlend * 0.24F;
        float b = 1.00F - orangeBlend * 0.88F;
        renderCoreStar(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
                nodeSprite,
                0.0F,
                0.0F,
                0.0F,
                radius,
                r,
                g,
                b,
                alpha,
                (entity.age + tickDelta) * 2.4F
        );

        renderImplodingGraceOrbits(entity, charge, implode, matrices, vertexConsumers, nodeSprite);
    }

    private static void renderSpiralBurst(CosmicBountyVisualEntity entity, float charge, float burstAge,
                                          MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        float fade = Math.max(0.0F, 1.0F - burstAge / 28.0F);
        if (fade <= 0.01F) {
            return;
        }

        Sprite nodeSprite = getNodeSprite();

        float progress = Math.min(1.0F, burstAge / 28.0F);
        float coreRadius = (1.22F + charge * 3.85F) * (1.0F + progress * 0.18F);
        renderCoreStar(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
                nodeSprite,
                0.0F,
                0.0F,
                0.0F,
                coreRadius,
                1.0F,
                0.76F,
                0.18F,
                fade * 0.92F,
                (entity.age + burstAge) * 4.8F
        );
        for (int orbit = 0; orbit < EXPANDING_ORBITS; orbit++) {
            float delay = orbit * 2.5F;
            float orbitAge = Math.max(0.0F, burstAge - delay);
            float orbitAlpha = fade * Math.min(1.0F, orbitAge / 5.0F);
            if (orbitAlpha <= 0.015F) {
                continue;
            }
            float baseRadius = 1.15F + orbit * 0.72F;
            float radius = baseRadius + progress * (2.6F + charge * 1.2F + orbit * 0.45F);
            renderExpandingGraceOrbit(
                    entity,
                    orbit,
                    orbitAge,
                    radius,
                    orbitAlpha,
                    matrices,
                    vertexConsumers,
                    nodeSprite
            );
        }
    }

    private static void renderExpandingGraceOrbit(CosmicBountyVisualEntity entity, int orbitIndex, float orbitAge,
                                                  float radius, float alpha, MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers, Sprite nodeSprite) {
        Vec3d previous = null;
        double phase = entity.getId() * 0.11 + orbitIndex * Math.PI * 2.0 / EXPANDING_ORBITS;
        double speed = 0.105 + orbitIndex * 0.018;
        for (int pointIndex = 0; pointIndex < ORBIT_TRAIL_POINTS; pointIndex++) {
            float t = pointIndex / (float) (ORBIT_TRAIL_POINTS - 1);
            double sampleAge = Math.max(0.0, orbitAge - (1.0 - t) * 17.0);
            double angle = sampleAge * speed + phase;
            double sampleRadius = radius - (1.0 - t) * (0.75 + orbitIndex * 0.18);
            double height = 0.45 + Math.sin(angle * 2.1) * 0.34 + orbitIndex * 0.18;
            Vec3d point = new Vec3d(
                    Math.cos(angle) * sampleRadius + jitter(entity.getId(), orbitIndex * 71 + pointIndex, 0) * 0.22,
                    height + jitter(entity.getId(), orbitIndex * 71 + pointIndex, 1) * 0.16,
                    Math.sin(angle) * sampleRadius + jitter(entity.getId(), orbitIndex * 71 + pointIndex, 2) * 0.22
            );
            float pointAlpha = alpha * (0.25F + t * 0.75F);
            if (previous != null) {
                renderLine(
                        matrices,
                        vertexConsumers.getBuffer(RenderLayer.getLines()),
                        (float) previous.x, (float) previous.y, (float) previous.z,
                        (float) point.x, (float) point.y, (float) point.z,
                        TRAIL_LINE_R, TRAIL_LINE_G, TRAIL_LINE_B, Math.min(1.0F, pointAlpha * 1.08F)
                );
            }
            renderNodeDisc(
                    matrices,
                    vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)),
                    nodeSprite,
                    (float) point.x, (float) point.y, (float) point.z,
                    SPIRAL_NODE_RADIUS * (0.62F + pointAlpha * 0.55F),
                    TRAIL_NODE_R,
                    TRAIL_NODE_G,
                    TRAIL_NODE_B,
                    Math.min(1.0F, pointAlpha * 1.22F)
            );
            previous = point;
        }
    }

    private static void renderImplodingGraceOrbits(CosmicBountyVisualEntity entity, float charge, float implode,
                                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                                   Sprite nodeSprite) {
        float age = entity.getImplodeTicks();
        float alpha = Math.min(1.0F, 0.55F + implode * 0.45F);
        for (int orbit = 0; orbit < EXPANDING_ORBITS; orbit++) {
            float startRadius = 4.9F + charge * 1.25F + orbit * 0.8F;
            float endRadius = 1.0F + orbit * 0.36F;
            float radius = startRadius + (endRadius - startRadius) * implode;
            renderExpandingGraceOrbit(
                    entity,
                    orbit,
                    age + orbit * 4.0F,
                    radius,
                    alpha * (1.0F - orbit * 0.08F),
                    matrices,
                    vertexConsumers,
                    nodeSprite
            );
        }
    }

    private static Sprite getNodeSprite() {
        return MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);
    }

    private static void renderCoreStar(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
                                       float x, float y, float z, float radius,
                                       float r, float g, float b, float alpha,
                                       float rotationDegrees) {
        renderStarBillboard(matrices, consumer, sprite, x, y, z, radius * 1.10F, radius * 0.43F, r, g, b, alpha * 0.42F, rotationDegrees + 18.0F);
        renderStarBillboard(matrices, consumer, sprite, x, y, z, radius, radius * 0.38F, r, g, b, alpha, rotationDegrees);
        renderStarBillboard(matrices, consumer, sprite, x, y, z, radius * 0.34F, radius * 0.22F, 1.0F, 0.96F, 0.72F, Math.min(1.0F, alpha * 1.18F), rotationDegrees * -0.35F);
    }

    private static void renderStarBillboard(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
                                            float x, float y, float z, float outerRadius, float innerRadius,
                                            float r, float g, float b, float alpha, float rotationDegrees) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -MinecraftClient.getInstance().gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                MinecraftClient.getInstance().gameRenderer.getCamera().getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationDegrees));

        MatrixStack.Entry entry = matrices.peek();
        float u = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float v = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;
        int vertices = STAR_POINTS * 2;
        for (int i = 0; i < vertices; i++) {
            double angleA = -Math.PI / 2.0 + Math.PI * 2.0 * i / vertices;
            double angleB = -Math.PI / 2.0 + Math.PI * 2.0 * (i + 1) / vertices;
            float radiusA = (i & 1) == 0 ? outerRadius : innerRadius;
            float radiusB = ((i + 1) & 1) == 0 ? outerRadius : innerRadius;
            vertex(consumer, entry, 0.0F, 0.0F, u, v, r, g, b, alpha);
            vertex(consumer, entry, (float) Math.cos(angleA) * radiusA, (float) Math.sin(angleA) * radiusA, u, v, r, g, b, alpha);
            vertex(consumer, entry, (float) Math.cos(angleB) * radiusB, (float) Math.sin(angleB) * radiusB, u, v, r, g, b, alpha);
        }
        matrices.pop();
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
            vertex(consumer, entry, 0.0F, 0.0F, u, v, r, g, b, alpha);
            vertex(consumer, entry, (float) Math.cos(angleA) * radius, (float) Math.sin(angleA) * radius, u, v, r, g, b, alpha);
            vertex(consumer, entry, (float) Math.cos(angleB) * radius, (float) Math.sin(angleB) * radius, u, v, r, g, b, alpha);
        }
        matrices.pop();
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry, float x, float y, float u, float v,
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
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float r, float g, float b, float alpha) {
        MatrixStack.Entry entry = matrices.peek();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001F) {
            return;
        }
        consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, alpha).normal(entry.getNormalMatrix(), dx / len, dy / len, dz / len)
                .next();
        consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, alpha).normal(entry.getNormalMatrix(), dx / len, dy / len, dz / len)
                .next();
    }

    private static float jitter(int seed, int index, int axis) {
        int mixed = seed * 31 + index * 131 + axis * 17;
        mixed ^= mixed >>> 13;
        mixed *= 0x5bd1e995;
        mixed ^= mixed >>> 15;
        return ((mixed & 0xFF) / 255.0F) - 0.5F;
    }
}
