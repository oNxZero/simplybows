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
        if (entity.isPrisonStyle()) {
            renderPrison(entity, matrices, vertexConsumers, light, height);
            return;
        }
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
    private static void renderPrison(IceChaosWallVisualEntity entity, MatrixStack matrices, VertexConsumerProvider consumers, int light, float height) {
        float width = Math.max(0.5F, entity.getPrisonWidth()), depth = Math.max(0.5F, entity.getPrisonDepth());
        float thickness = 0.12F;
        cube(matrices, consumers, light, -width/2, 0, -depth/2, width, thickness, depth);
        cube(matrices, consumers, light, -width/2, height-thickness, -depth/2, width, thickness, depth);
        cube(matrices, consumers, light, -width/2, 0, -depth/2, thickness, height, depth);
        cube(matrices, consumers, light, width/2-thickness, 0, -depth/2, thickness, height, depth);
        cube(matrices, consumers, light, -width/2, 0, -depth/2, width, height, thickness);
        cube(matrices, consumers, light, -width/2, 0, depth/2-thickness, width, height, thickness);
        // Stepped square crystals keep the same voxel style as the packed-ice wall.
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            for (int layer = 0; layer < 4; layer++) {
                float size = 0.34F - layer * 0.07F;
                cube(matrices, consumers, light, x*width/2-size/2, height+layer*0.16F,
                        z*depth/2-size/2, size, 0.18F, size);
            }
            cube(matrices, consumers, light, x*width/2-0.22F, height*0.5F, z*depth/2-0.22F, 0.44F, 0.3F, 0.44F);
        }
    }

    private static void cube(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                             float x, float y, float z, float width, float height, float depth) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale(width, height, depth);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.PACKED_ICE.getDefaultState(), matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

}
