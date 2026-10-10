package net.sweenus.simplybows.client.renderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.entity.BubbleGraceVisualEntity;
/** Restorative fountain ward: rotating water ribs and an elevated protective crown. */
public class BubbleGraceVisualEntityRenderer extends EntityRenderer<BubbleGraceVisualEntity> {
    public BubbleGraceVisualEntityRenderer(EntityRendererFactory.Context context) { super(context); }
    public Identifier getTexture(BubbleGraceVisualEntity e) { return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE; }
    public void render(BubbleGraceVisualEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider c,int light) {
        float fade=Math.max(0,Math.min(1,e.getHeightScale())); if(fade<=.01) return;
        // The ward is a floor particle effect; no solid water geometry.
    }
}
