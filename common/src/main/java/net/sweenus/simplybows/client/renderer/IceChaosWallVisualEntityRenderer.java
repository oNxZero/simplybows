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
import net.sweenus.simplybows.entity.IceChaosWallVisualEntity;

public class IceChaosWallVisualEntityRenderer extends EntityRenderer<IceChaosWallVisualEntity> {

    public IceChaosWallVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(IceChaosWallVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(IceChaosWallVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = Math.max(0.0F, entity.getHeightScale());
        if (heightScale <= 0.01F) {
            return;
        }

        float height = Math.max(0.05F, entity.getTargetHeight() * heightScale);
        matrices.push();
        matrices.translate(-0.5, 0.0, -0.5);
        matrices.scale(1.0F, height, 1.0F);
        var blockState = entity.isDripstoneStyle()
                ? Blocks.DRIPSTONE_BLOCK.getDefaultState()
                : Blocks.PACKED_ICE.getDefaultState();
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                blockState,
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
