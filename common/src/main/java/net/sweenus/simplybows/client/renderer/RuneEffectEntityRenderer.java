package net.sweenus.simplybows.client.renderer;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.RuneEffectEntity;
import net.sweenus.simplybows.util.RuneEffectRules;

/** Stepped Minecraft geometry: rising jaws, travelling stars, stingers, rainclouds and opening petals. */
public class RuneEffectEntityRenderer extends EntityRenderer<RuneEffectEntity> {
    private final BeeEntityRenderer beeRenderer;
    private net.minecraft.entity.passive.BeeEntity renderBee;
    public RuneEffectEntityRenderer(EntityRendererFactory.Context context) { super(context); beeRenderer = new BeeEntityRenderer(context); }
    public Identifier getTexture(RuneEffectEntity entity) { return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE; }
    @Override public boolean shouldRender(RuneEffectEntity entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().expand(10, 6, 10));
    }
    public void render(RuneEffectEntity entity, float yaw, float delta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        float t = entity.elapsed() + delta;
        int duration = entity.duration();
        float fade = RuneEffectRules.animationScale(t, entity.finishAt());
        if (fade <= 0) return;
        double width = Math.min(4, entity.targetWidth());
        double height = Math.min(6, entity.targetHeight());
        double radius = RuneEffectRules.radius(entity.kind(), entity.strings());
        switch (entity.kind()) {
            case 7,8 -> { /* Animated support particles are emitted by the visual entity. */ }
            case 9 -> { /* A server-simulated particle bolt, with no solid body. */ }
            case 10 -> {
                matrices.push();
                double a=Math.atan2(entity.getVelocity().x,entity.getVelocity().z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(a*180/Math.PI)));
                for(int tier=0;tier<4;tier++) {
                    double thickness=(.34-tier*.075)*fade;
                    block(matrices,consumers,light,Blocks.ICE,0,0,tier*.22*fade,
                            thickness,thickness,.3*fade,0,0);
                }
                matrices.pop();
            }
            case RuneEffectRules.STONE -> {
                matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
                for (int pair=0; pair<RuneEffectRules.STONE_PAIRS; pair++) {
                    double close = RuneEffectRules.stoneClosing(t,pair);
                    double rise=smooth((t-(RuneEffectRules.stoneStrikeTick(pair)-18))/10);
                    double breakAge=t-RuneEffectRules.stoneStrikeTick(pair);
                    double pairFade = fade * smooth((t-(RuneEffectRules.stoneStrikeTick(pair)-18))/8) * (1-smooth((float)((breakAge)/12)));
                    double scatter = breakAge>0 ? smooth((float)(breakAge/12)) : 0;
                    if (pairFade <= .01) continue;
                    double z = RuneEffectRules.stonePairZ(pair,radius);
                    if(breakAge>0) {
                        double age=Math.min(12,breakAge)/12;
                        for(int fragment=0;fragment<18;fragment++) {
                            double angle=fragment*2.399963;
                            double speed=.65+(fragment%4)*.22;
                            double x=Math.cos(angle)*age*speed*2;
                            double y=.7+(fragment%3)*.45+age*1.4-age*age*3;
                            double dz=Math.sin(angle)*age*speed;
                            matrices.push(); matrices.translate(x,y,z+dz);
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(fragment*31+breakAge*14)));
                            block(matrices,consumers,light,fragment%3==0 ? Blocks.POINTED_DRIPSTONE : Blocks.DRIPSTONE_BLOCK,
                                    0,0,0,.22*pairFade,(fragment%3==0 ? .65 : .28)*pairFade,.22*pairFade,0,0);
                            matrices.pop();
                        }
                        continue;
                    }

                    for (int side : new int[]{-1,1}) {
                        double x = side*(radius*(1-close)+.3+scatter*1.15+.25*Math.sin(rise*Math.PI));
                        double a=Math.toRadians(entity.getYaw());
                        double ground = entity.groundOffset(x*Math.cos(a)-z*Math.sin(a),x*Math.sin(a)+z*Math.cos(a))-scatter*.8-(1-rise)*2.5;
                        block(matrices,consumers,light,Blocks.DRIPSTONE_BLOCK,x,ground-.25,z,.9,1.5*pairFade,.95,0,side*12);
                        // Layered tapered stone shoulders, with a long tooth aimed inward.
                        spike(matrices,consumers,light,x,ground+.35,z,2.6*pairFade,.78,(float)(side*(58+scatter*55)));
                        spike(matrices,consumers,light,x+side*.2,ground+.2,z-.35,1.65*pairFade,.5,(float)(side*(30+scatter*65)));
                        spike(matrices,consumers,light,x+side*.15,ground+.2,z+.35,2.0*pairFade,.55,(float)(side*(42+scatter*50)));
                    }
                }
                matrices.pop();
            }
            case RuneEffectRules.STAR -> {
                double phase = Math.min(t,duration)%30;
                double progress = RuneEffectRules.starProgress(phase);
                for (RuneEffectRules.StarCell cell : RuneEffectRules.starCells(entity.strings())) {
                    double ground = entity.groundOffset(cell.x(),cell.z());
                    double filled = smooth((float)((progress-cell.fraction())/.22+.5));
                    double rise = filled*fade;
                    // A continuous fractured floor supports broad, staggered crystalline teeth.
                    block(matrices,consumers,light,Blocks.DRIPSTONE_BLOCK,cell.x(),ground-.18,cell.z(),.56,.2*fade,.56,0,0);
                    if (rise <= .01) continue;
                    double h = (cell.height()+.8*(1-cell.fraction()))*.5*rise;
                    double angle = Math.atan2(cell.z(),cell.x());
                    matrices.push(); matrices.translate(cell.x(),ground-.22*(1-rise),cell.z());
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90-angle*180/Math.PI)));
                    spike(matrices,consumers,light,0,0,0,h,.53,(float)(-18*cell.fraction()));
                    matrices.pop();
                }
            }
            case RuneEffectRules.SWARM -> {
                for (int bee = 0; bee < 7; bee++) {
                    double angle = t*.15 + bee*Math.PI*2/7;
                    double swoop = 1 - .35*Math.max(0, Math.sin(t*.52 + bee));
                    if (entity.finishAt() >= 0) swoop += (1-fade)*1.2;
                    matrices.push(); matrices.translate(Math.cos(angle)*(width*.5+.7)*swoop,
                            height*.5+Math.sin(t*.16+bee)*height*.32, Math.sin(angle)*(width*.5+.7)*swoop);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-angle*180/Math.PI)));
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null) {
                        if (renderBee == null || renderBee.getWorld() != client.world) renderBee = net.minecraft.entity.EntityType.BEE.create(client.world);
                        if (renderBee != null) {
                            renderBee.age = entity.age + bee*3; renderBee.setAngerTime(100); renderBee.setNoGravity(true);
                            renderBee.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), 0, 0);
                            renderBee.bodyYaw = renderBee.prevBodyYaw = renderBee.headYaw = renderBee.prevHeadYaw = 0;
                            matrices.scale(.42F*fade,.42F*fade,.42F*fade);
                            beeRenderer.render(renderBee, 0, delta, matrices, consumers, light);
                        }
                    }
                    matrices.pop();
                }
            }
            case RuneEffectRules.VORTEX -> {
                double rainRadius=RuneEffectRules.radius(entity.kind(),entity.strings());
                // Overlapping tiered lobes make a continuous 3D raincloud, with a dark belly and bright crown.
                for(int puff=0;puff<17;puff++) {
                    double a=puff*Math.PI/4;
                    double ring=puff==16 ? 0 : puff<8 ? .68 : .31;
                    double x=Math.cos(a)*rainRadius*ring*fade;
                    double z=Math.sin(a)*rainRadius*ring*fade;
                    double size=(rainRadius*(puff==16 ? .6 : .5)+.18*Math.sin(puff*2.3))*fade;
                    double bob=.06*Math.sin(t*.07+puff);
                    for(int tier=0;tier<3;tier++) {
                        double taper=tier==1 ? 1 : .72;
                        RuneCloudRenderer.box(matrices,consumers,light,x,6.1+tier*.7*fade+bob,z,
                                size*taper,.85*fade,size*taper,tier==0 ? 0x8392A4 : tier==1 ? 0xCFDCE8 : 0xF0F5FA,.8F*fade);
                    }
                }
                for(int ring=0;ring<3;ring++) {
                    double phase=(t*.045+ring/3.0)%1;
                    for(int edge=0;edge<20;edge++) {
                        double a=edge*Math.PI/10;
                        double r=(.25+phase*rainRadius)*fade;
                        RuneWaterRenderer.box(matrices,consumers,light,Math.cos(a)*r,.04,Math.sin(a)*r,
                                .18*fade,.025*fade,.18*fade,0xB9E6FF,(float)((1-phase)*.45*fade));
                    }
                }
            }
            case RuneEffectRules.WATER_DROP -> {
                matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t*12));
                // A rounded stepped water body with a tapered neck, rather than a flat particle.
                for(int tier=0;tier<5;tier++) {
                    double taper=tier==2 ? .4 : tier==1 || tier==3 ? .25 : .1;
                    RuneWaterRenderer.box(matrices,consumers,light,0,(tier*.18-.4)*fade,0,
                            taper*fade,.2*fade,taper*fade,0x79CFFF,.85F*fade);
                }
                matrices.pop();
            }
            case RuneEffectRules.PETAL_BLADE -> {
                matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t*25));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30));
                block(matrices,consumers,light,Blocks.PINK_STAINED_GLASS,0,0,0,.75*fade,.12*fade,.3*fade,0,0);
                block(matrices,consumers,light,Blocks.CHERRY_LEAVES,.35*fade,.03,0,.4*fade,.16*fade,.23*fade,0,0);
                block(matrices,consumers,light,Blocks.PINK_STAINED_GLASS,-.4*fade,.05,0,.3*fade,.1*fade,.18*fade,0,0);
                matrices.pop();
            }
            case RuneEffectRules.LOTUS -> {
                double open=smooth(t/20);
                // An anchored blossom opens into a rotating crown, then sheds hunting petal blades.
                for(int petal=0;petal<8;petal++) {
                    double angle=petal*Math.PI/4+t*.035;
                    matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(angle*180/Math.PI)));
                    for(int tier=0;tier<4;tier++) {
                        double curl=Math.sin(tier*Math.PI/4);
                        double spread=(.35+curl*1.3*open)*fade;
                        block(matrices,consumers,light,petal%2==0 ? Blocks.PINK_STAINED_GLASS : Blocks.CHERRY_LEAVES,
                                0,tier*.28*fade,spread,(.65-tier*.1)*fade,.33*fade,.48*fade,0,0);
                    }
                    matrices.pop();
                }
                for(int petal=0;petal<3;petal++) {
                    if(t>=40) continue;
                    double angle=petal*Math.PI*2/3+t*.17;
                    double r=(1.45+1.2*smooth((t-16)/16)*(1-smooth((t-32)/8)))*fade;
                    block(matrices,consumers,light,Blocks.PINK_STAINED_GLASS,Math.cos(angle)*r,
                            (1.6+.3*Math.sin(angle*2))*fade,Math.sin(angle)*r,.5*fade,.09*fade,.23*fade,
                            (float)(-angle*180/Math.PI),35);
                }
            }

        }
    }
    /** Broad stepped base, narrowing shoulders and a pointed cap in the same voxel style as our walls. */
    private static void spike(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                              double x,double y,double z,double height,double width,float tilt) {
        matrices.push(); matrices.translate(x,y,z); matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));
        for (int tier=0;tier<3;tier++) {
            double taper=1-tier*.24;
            block(matrices,consumers,light,Blocks.DRIPSTONE_BLOCK,0,height*tier*.19,0,
                    width*taper,height*.25,width*taper,0,0);
        }
        block(matrices,consumers,light,Blocks.POINTED_DRIPSTONE,0,height*.55,0,width*.5,height*.55,width*.5,0,0);
        matrices.pop();
    }
    private static float smooth(float value) { float v=Math.max(0,Math.min(1,value)); return v*v*(3-2*v); }
    private static void block(MatrixStack matrices, VertexConsumerProvider consumers, int light, Block block,
                              double x, double y, double z, double sx, double sy, double sz, float yaw, float tilt) {
        matrices.push(); matrices.translate(x,y,z); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt)); matrices.scale((float)sx,(float)sy,(float)sz);
        matrices.translate(-.5,0,-.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(block.getDefaultState(), matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
