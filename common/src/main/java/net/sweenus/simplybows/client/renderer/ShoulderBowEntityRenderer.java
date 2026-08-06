package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.ShoulderBowEntity;
import net.sweenus.simplybows.registry.ItemRegistry;

public class ShoulderBowEntityRenderer extends EntityRenderer<ShoulderBowEntity> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/item/bow.png");
    // Item models use a different intrinsic forward axis than entity facing.
    private static final float LOCAL_YAW_OFFSET = -90.0F;
    private static final float LOCAL_PITCH_OFFSET = 0.0F;
    private static final float LOCAL_ROLL_OFFSET = -45.0F;
    private static final double RENDER_BOB_AMPLITUDE = 0.06;
    private static final double RENDER_BOB_SPEED = 0.22;

    public ShoulderBowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ShoulderBowEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ShoulderBowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float renderYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        float renderPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
        Entity ownerEntity = entity.getWorld().getEntityById(entity.getOwnerEntityId());
        
        if (entity.getPullStage() == 0) {
            if (ownerEntity instanceof LivingEntity owner) {
                Vec3d ownerLook = owner.getRotationVec(tickDelta).normalize();
                renderYaw = (float) (Math.atan2(ownerLook.x, ownerLook.z) * (180.0F / Math.PI));
                renderPitch = (float) (-(Math.atan2(ownerLook.y, ownerLook.horizontalLength()) * (180.0F / Math.PI)));
            }
        }

        ItemStack renderStack = getStackForPullStage(entity, entity.getPullStage());

        matrices.push();
        if (ownerEntity instanceof LivingEntity owner) {
            applyRenderFollowAnchor(entity, owner, tickDelta, matrices);
        }
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderPitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(LOCAL_YAW_OFFSET));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(LOCAL_PITCH_OFFSET));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(LOCAL_ROLL_OFFSET));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getSide() < 0 ? -10.0F : 10.0F));
        matrices.scale(1.0F, 1.0F, 1.0F);
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                renderStack,
                ModelTransformationMode.FIXED,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                entity.getId()
        );
        matrices.pop();
    }

    private static void applyRenderFollowAnchor(ShoulderBowEntity shoulderBow, LivingEntity owner, float tickDelta, MatrixStack matrices) {
        float ownerYaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevYaw, owner.getYaw());
        Vec3d ownerPos = new Vec3d(
                MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ())
        );
        Vec3d forward = Vec3d.fromPolar(0.0F, ownerYaw).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        double sideOffset = ShoulderBowEntity.SHOULDER_SIDE_OFFSET * (double) shoulderBow.getSide();
        double bob = Math.sin((shoulderBow.age + tickDelta + (shoulderBow.getSide() > 0 ? 6.0 : 0.0)) * RENDER_BOB_SPEED) * RENDER_BOB_AMPLITUDE;
        Vec3d anchor = ownerPos
                .add(0.0, owner.getStandingEyeHeight() + ShoulderBowEntity.SHOULDER_HEIGHT_OFFSET + bob, 0.0)
                .add(right.multiply(sideOffset))
                .add(forward.multiply(ShoulderBowEntity.SHOULDER_BACK_OFFSET));
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, shoulderBow.prevX, shoulderBow.getX()),
                MathHelper.lerp(tickDelta, shoulderBow.prevY, shoulderBow.getY()),
                MathHelper.lerp(tickDelta, shoulderBow.prevZ, shoulderBow.getZ())
        );
        Vec3d correction = anchor.subtract(entityPos);
        matrices.translate(correction.x, correction.y, correction.z);
    }

    private static ItemStack getStackForPullStage(ShoulderBowEntity entity, int stage) {
        if (entity.isMirroringOffhand()) {
            Item mirroredItem = Item.byRawId(entity.getMirroredItemRawId());
            if (mirroredItem != null && mirroredItem != Items.AIR) {
                if (stage > 0) {
                    ItemStack pullProxy = getMirrorPullProxy(mirroredItem, stage);
                    if (!pullProxy.isEmpty()) {
                        return pullProxy;
                    }
                }
                return new ItemStack(mirroredItem);
            }
        }
        return switch (stage) {
            case 1 -> new ItemStack(ItemRegistry.ECHO_BOW_VISUAL_PULL_0.get());
            case 2 -> new ItemStack(ItemRegistry.ECHO_BOW_VISUAL_PULL_1.get());
            case 3 -> new ItemStack(ItemRegistry.ECHO_BOW_VISUAL_PULL_2.get());
            default -> new ItemStack(ItemRegistry.ECHO_BOW.get());
        };
    }

    private static ItemStack getMirrorPullProxy(Item mirroredItem, int stage) {
        if (mirroredItem == ItemRegistry.VINE_BOW.get()) {
            return switch (stage) {
                case 1 -> new ItemStack(ItemRegistry.VINE_BOW_VISUAL_PULL_0.get());
                case 2 -> new ItemStack(ItemRegistry.VINE_BOW_VISUAL_PULL_1.get());
                default -> new ItemStack(ItemRegistry.VINE_BOW_VISUAL_PULL_2.get());
            };
        }
        if (mirroredItem == ItemRegistry.ICE_BOW.get()) {
            return switch (stage) {
                case 1 -> new ItemStack(ItemRegistry.ICE_BOW_VISUAL_PULL_0.get());
                case 2 -> new ItemStack(ItemRegistry.ICE_BOW_VISUAL_PULL_1.get());
                default -> new ItemStack(ItemRegistry.ICE_BOW_VISUAL_PULL_2.get());
            };
        }
        if (mirroredItem == ItemRegistry.BUBBLE_BOW.get()) {
            return switch (stage) {
                case 1 -> new ItemStack(ItemRegistry.BUBBLE_BOW_VISUAL_PULL_0.get());
                case 2 -> new ItemStack(ItemRegistry.BUBBLE_BOW_VISUAL_PULL_1.get());
                default -> new ItemStack(ItemRegistry.BUBBLE_BOW_VISUAL_PULL_2.get());
            };
        }
        if (mirroredItem == ItemRegistry.BEE_BOW.get()) {
            return switch (stage) {
                case 1 -> new ItemStack(ItemRegistry.BEE_BOW_VISUAL_PULL_0.get());
                case 2 -> new ItemStack(ItemRegistry.BEE_BOW_VISUAL_PULL_1.get());
                default -> new ItemStack(ItemRegistry.BEE_BOW_VISUAL_PULL_2.get());
            };
        }
        if (mirroredItem == ItemRegistry.BLOSSOM_BOW.get()) {
            return switch (stage) {
                case 1 -> new ItemStack(ItemRegistry.BLOSSOM_BOW_VISUAL_PULL_0.get());
                case 2 -> new ItemStack(ItemRegistry.BLOSSOM_BOW_VISUAL_PULL_1.get());
                default -> new ItemStack(ItemRegistry.BLOSSOM_BOW_VISUAL_PULL_2.get());
            };
        }
        if (mirroredItem == ItemRegistry.EARTH_BOW.get()) {
            return switch (stage) {
                case 1 -> new ItemStack(ItemRegistry.EARTH_BOW_VISUAL_PULL_0.get());
                case 2 -> new ItemStack(ItemRegistry.EARTH_BOW_VISUAL_PULL_1.get());
                default -> new ItemStack(ItemRegistry.EARTH_BOW_VISUAL_PULL_2.get());
            };
        }
        return ItemStack.EMPTY;
    }
}
