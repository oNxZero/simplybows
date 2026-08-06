package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.entity.HomingArrowEntity;

public class HomingArrowEntityRenderer extends ProjectileEntityRenderer<HomingArrowEntity> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/projectiles/arrow.png");
    private static final Identifier TIPPED_TEXTURE = new Identifier("minecraft", "textures/entity/projectiles/tipped_arrow.png");
    private static final Identifier SPECTRAL_TEXTURE = new Identifier("minecraft", "textures/entity/projectiles/spectral_arrow.png");
    private static final Identifier ICE_ARROW_TEXTURE = new Identifier(SimplyBows.MOD_ID, "textures/item/ice_bow/ice_bow_arrow.png");
    private int currentColor = -1;

    public HomingArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(HomingArrowEntity entity) {
        if (entity.getColor() > 0) {
            return TIPPED_TEXTURE;
        }
        return MinecraftClient.getInstance().getResourceManager().getResource(ICE_ARROW_TEXTURE).isPresent()
                ? ICE_ARROW_TEXTURE
                : TEXTURE;
    }

    @Override
    public void render(HomingArrowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.currentColor = getArrowColor(entity);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        this.currentColor = -1;
    }

    @Override
    public void vertex(org.joml.Matrix4f positionMatrix, org.joml.Matrix3f normalMatrix, VertexConsumer consumer,
                       int x, int y, int z, float u, float v,
                       int normalX, int normalY, int normalZ, int light) {
        consumer.vertex(positionMatrix, (float) x, (float) y, (float) z)
                .color(this.currentColor)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, (float) normalX, (float) normalY, (float) normalZ)
                .next();
    }

    private int getArrowColor(HomingArrowEntity entity) {
        int color = entity.getColor();
        if (color > 0) {
            return 0xFF000000 | color;
        }
        return -1;
    }
}
