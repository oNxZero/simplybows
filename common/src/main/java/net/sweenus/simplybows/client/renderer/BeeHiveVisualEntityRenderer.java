package net.sweenus.simplybows.client.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplybows.entity.BeeHiveVisualEntity;

public class BeeHiveVisualEntityRenderer extends EntityRenderer<BeeHiveVisualEntity> {

    public BeeHiveVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BeeHiveVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(BeeHiveVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = MathHelper.clamp(entity.getHeightScale(), 0.0F, 1.0F);
        if (heightScale <= 0.01F) {
            return;
        }

        matrices.push();
        matrices.translate(-0.5, 0.0, -0.5);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.sin((entity.age+tickDelta)*.1)*4));
        matrices.scale(0.9F*heightScale, Math.max(0.05F, heightScale), 0.9F*heightScale);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.BEE_NEST.getDefaultState(),
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
