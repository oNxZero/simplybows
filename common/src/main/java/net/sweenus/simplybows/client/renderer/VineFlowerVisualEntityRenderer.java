package net.sweenus.simplybows.client.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.VineFlowerVisualEntity;

public class VineFlowerVisualEntityRenderer extends EntityRenderer<VineFlowerVisualEntity> {

    public VineFlowerVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(VineFlowerVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    private static BlockState lichenFace(Direction dir) {
        return Blocks.GLOW_LICHEN.getDefaultState()
                .with(Properties.NORTH, dir == Direction.NORTH)
                .with(Properties.SOUTH, dir == Direction.SOUTH)
                .with(Properties.EAST,  dir == Direction.EAST)
                .with(Properties.WEST,  dir == Direction.WEST)
                .with(Properties.UP,    dir == Direction.UP)
                .with(Properties.DOWN,  dir == Direction.DOWN);
    }

    @Override
    public void render(VineFlowerVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = Math.max(0.0F, entity.getHeightScale());
        if (heightScale <= 0.01F) {
            return;
        }

        int type = entity.getFlowerType();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.translate(-0.5, 0.0, -0.5);

        if (type == 6) {
            float w = 0.80F + Math.min(2.0F, heightScale) * 0.80F;
            float h = Math.max(0.05F, heightScale);
            matrices.translate(0.0, h, 0.0);
            matrices.scale(w, -h, w);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                    Blocks.SPORE_BLOSSOM.getDefaultState(), matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
            return;
        }

        if (type >= 9 && type <= 16) {
            BlockState state = switch (type) {
                case 9,  13 -> lichenFace(Direction.EAST);
                case 10, 14 -> lichenFace(Direction.WEST);
                case 11, 15 -> lichenFace(Direction.NORTH);
                default     -> lichenFace(Direction.SOUTH); // 12, 16
            };
            int lt = type >= 13 ? 0x00F000F0 : light;
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                    state, matrices, vertexConsumers, lt, OverlayTexture.DEFAULT_UV);
            matrices.pop();
            return;
        }

        boolean lichenFlat = type == 7 || type == 8;
        BlockState state = switch (type) {
            case 1 -> Blocks.FERN.getDefaultState();
            case 2 -> Blocks.DANDELION.getDefaultState();
            case 3 -> Blocks.POPPY.getDefaultState();
            case 4 -> Blocks.CHERRY_LOG.getDefaultState();
            case 5 -> Blocks.CHERRY_LEAVES.getDefaultState();
            case 7, 8 -> lichenFace(Direction.UP);
            case 17 -> Blocks.WITHER_ROSE.getDefaultState();
            case 18 -> Blocks.ALLIUM.getDefaultState();
            case 19 -> Blocks.CORNFLOWER.getDefaultState();
            case 20 -> Blocks.RED_TULIP.getDefaultState();
            default -> Blocks.SHORT_GRASS.getDefaultState();
        };

        float xzScale   = type == 4 ? 0.48F : lichenFlat ? 1.0F : 0.92F;
        float yScale    = lichenFlat ? 0.03F : Math.max(0.05F, heightScale);
        int renderLight = type == 8 ? 0x00F000F0 : light;

        matrices.scale(xzScale, yScale, xzScale);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                state, matrices, vertexConsumers, renderLight, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
