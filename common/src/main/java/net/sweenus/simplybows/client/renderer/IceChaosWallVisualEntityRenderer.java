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
        float heightScale = Math.max(0.0F, (entity.isPrisonStyle() ? entity.getAnimatedHeightScale(tickDelta) : entity.getHeightScale()));
        if (heightScale <= 0.01F) {
            return;
        }

        float height = Math.max(0.05F, entity.getTargetHeight() * heightScale);
        if (entity.isPrisonStyle()) {
            float time=entity.age+tickDelta;
            matrices.push(); matrices.translate(Math.sin(time*.16)*.045,0,Math.cos(time*.13)*.045);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.sin(time*.11)*2));
            matrices.scale(1+(float)Math.sin(time*.15)*.025F,1+(float)Math.sin(time*.15)*.025F,1+(float)Math.sin(time*.15)*.025F);
            renderPrison(entity, matrices, vertexConsumers, light, height); matrices.pop();
            return;
        }
        if(!entity.isDripstoneStyle()) {
            float forming=Math.max(0,1-heightScale);
            boolean breaking=entity.age>20 && heightScale<.999F;
            for(int piece=0;piece<12;piece++) {
                double angle=piece*2.399963;
                double spread=forming*(breaking ? 2.4 : 1.8);
                matrices.push();
                matrices.translate((piece%2-.5)*.5+Math.cos(angle)*spread,(piece/4)*entity.getTargetHeight()/3 + (breaking ? forming*(1-forming)*2 : forming*.6),((piece/2)%2-.5)*.5+Math.sin(angle)*spread);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(piece*31+forming*(breaking ? 180 : -120)));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(forming*70*(piece%2==0 ? 1 : -1)));
                float size=breaking ? Math.max(0,heightScale) : Math.max(.05F,heightScale);
                matrices.scale(.55F*size,entity.getTargetHeight()*.34F*size,.55F*size);
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(Blocks.ICE.getDefaultState(),matrices,vertexConsumers,light,OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }
            return;
        }
        if(entity.isDripstoneStyle() && entity.age>20 && heightScale<.65F) {
            float breakPhase=1-heightScale/.65F;
            for(int piece=0;piece<8;piece++) {
                matrices.push();
                matrices.translate((piece%2-.5)*(.4+breakPhase*.8),height*(piece/4F)-breakPhase*.6,((piece/2)%2-.5)*(.4+breakPhase*.8));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(piece*27+breakPhase*90));
                matrices.scale(.36F*heightScale,.5F*heightScale,.36F*heightScale);
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(Blocks.DRIPSTONE_BLOCK.getDefaultState(),matrices,vertexConsumers,light,OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }
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
        // Broad, uneven crystal shoulders: no evenly tapering pyramid silhouette.
        cube(matrices, consumers, light, -width*0.52F, 0, -depth*0.48F, width*1.04F, height*0.78F, depth*0.96F);
        cube(matrices, consumers, light, -width*0.39F, height*0.70F, -depth*0.34F, width*0.85F, height*0.30F, depth*0.75F);
        cube(matrices, consumers, light, -width*0.65F, height*0.12F, -depth*0.20F, width*0.25F, height*0.56F, depth*0.63F);

        // Crystal directions are independent from their thickness: branches extend outward.
        spike(matrices, consumers, light, width*0.38F, height*0.35F, -depth*0.15F,
                width*0.92F, height*0.16F, -depth*0.20F, width*0.50F, height*0.28F, depth*0.40F);
        spike(matrices, consumers, light, -width*0.42F, height*0.57F, depth*0.14F,
                -width*0.85F, height*0.12F, depth*0.28F, width*0.46F, height*0.24F, depth*0.45F);
        spike(matrices, consumers, light, width*0.13F, height*0.53F, depth*0.38F,
                width*0.27F, height*0.10F, depth*0.96F, width*0.42F, height*0.26F, depth*0.48F);
        spike(matrices, consumers, light, -width*0.21F, height*0.26F, -depth*0.38F,
                -width*0.29F, height*0.17F, -depth*0.89F, width*0.45F, height*0.30F, depth*0.48F);
        spike(matrices, consumers, light, width*0.35F, height*0.72F, depth*0.29F,
                width*0.59F, height*0.29F, depth*0.60F, width*0.37F, height*0.24F, depth*0.37F);
        spike(matrices, consumers, light, -width*0.30F, height*0.79F, -depth*0.23F,
                -width*0.52F, height*0.27F, -depth*0.51F, width*0.38F, height*0.24F, depth*0.38F);
        // A short, offset crown breaks up the top without dominating the side spikes.
        spike(matrices, consumers, light, width*0.06F, height*0.92F, -depth*0.09F,
                width*0.13F, height*0.34F, -depth*0.08F, width*0.36F, height*0.21F, depth*0.34F);
    }

    private static void spike(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                              float x, float y, float z, float dx, float dy, float dz,
                              float width, float height, float depth) {
        for (int step = 0; step < 5; step++) {
            float progress = step / 4.0F;
            float taper = 1.0F - progress * 0.78F;
            float w = width*taper + Math.abs(dx)/5, h = height*taper + Math.abs(dy)/5, d = depth*taper + Math.abs(dz)/5;
            cube(matrices, consumers, light, x+dx*progress-w/2, y+dy*progress-h/2,
                    z+dz*progress-d/2, w, h, d);
        }
    }

    private static void cube(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                             float x, float y, float z, float width, float height, float depth) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale(width, height, depth);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.ICE.getDefaultState(), matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

}
