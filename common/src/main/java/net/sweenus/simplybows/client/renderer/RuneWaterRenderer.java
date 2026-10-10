package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Animated vanilla flowing-water faces; no glass borders or ice textures. */
final class RuneWaterRenderer {
    private RuneWaterRenderer() {}
    static void box(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                    double x, double y, double z, double width, double height, double depth, int color, float alpha) {
        Sprite sprite = MinecraftClient.getInstance().getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(Identifier.ofVanilla("block/water_flow"));
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        MatrixStack.Entry entry = matrices.peek();
        float x0=(float)(x-width/2), x1=(float)(x+width/2), y0=(float)y, y1=(float)(y+height), z0=(float)(z-depth/2), z1=(float)(z+depth/2);
        float u0=sprite.getMinU(), u1=sprite.getMaxU(), v0=sprite.getMinV(), v1=sprite.getMaxV();
        float red=((color>>16)&255)/255F, green=((color>>8)&255)/255F, blue=(color&255)/255F;
        quadDoubleSided(vc,entry,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,u0,v0,u1,v1,light,red,green,blue,alpha,0,1,0);
        quadDoubleSided(vc,entry,x0,y0,z1,x1,y0,z1,x1,y0,z0,x0,y0,z0,u0,v0,u1,v1,light,red,green,blue,alpha,0,-1,0);
        quadDoubleSided(vc,entry,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,u0,v0,u1,v1,light,red,green,blue,alpha,0,0,-1);
        quadDoubleSided(vc,entry,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,u0,v0,u1,v1,light,red,green,blue,alpha,0,0,1);
        quadDoubleSided(vc,entry,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,u0,v0,u1,v1,light,red,green,blue,alpha,-1,0,0);
        quadDoubleSided(vc,entry,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,u0,v0,u1,v1,light,red,green,blue,alpha,1,0,0);
    }
    private static void quadDoubleSided(
            VertexConsumer vc,
            MatrixStack.Entry entry,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            int light,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        vertex(vc, entry, x0, y0, z0, u0, v1, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x1, y1, z1, u1, v1, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x2, y2, z2, u1, v0, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x3, y3, z3, u0, v0, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x3, y3, z3, u0, v0, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x2, y2, z2, u1, v0, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x1, y1, z1, u1, v1, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x0, y0, z0, u0, v1, light, r, g, b, a, -nx, -ny, -nz);
    }

    private static void vertex(
            VertexConsumer vc,
            MatrixStack.Entry entry,
            float x, float y, float z,
            float u, float v,
            int light,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        vc.vertex(entry, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, nx, ny, nz);
    }

}
