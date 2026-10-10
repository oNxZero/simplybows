package net.sweenus.simplybows.client.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.BubblePainArrowEntity;

/** A rotating water core with trailing fins, rather than the old axolotl volley. */
public class BubblePainArrowEntityRenderer extends EntityRenderer<BubblePainArrowEntity> {
    public BubblePainArrowEntityRenderer(EntityRendererFactory.Context context) { super(context); }
    public Identifier getTexture(BubblePainArrowEntity entity) { return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE; }
    public void render(BubblePainArrowEntity entity, float yaw, float delta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((entity.age+delta)*16));
        for (int i=0;i<5;i++) {
            matrices.push();
            if (i==0) matrices.scale(.32F,.32F,.32F);
            else { matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i*90)); matrices.translate(.24,0,-.15); matrices.scale(.12F,.3F,.5F); }
            int waterColor=net.minecraft.client.color.world.BiomeColors.getWaterColor(entity.getWorld(),entity.getBlockPos());
            RuneWaterRenderer.box(matrices,consumers,light,0,-.5,0,1,1,1,waterColor,.8F);
            matrices.pop();
        }
        matrices.pop();
    }
}
