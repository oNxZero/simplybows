package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.item.unique.BeeBowItem;
import net.sweenus.simplybows.item.unique.BlossomBowItem;
import net.sweenus.simplybows.item.unique.BubbleBowItem;
import net.sweenus.simplybows.item.unique.EarthBowItem;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Unique
    private static final ThreadLocal<Boolean> SIMPLYBOWS_SUBSTITUTING = ThreadLocal.withInitial(() -> false);

    // Default placeholder — used for any bow without a dedicated inventory model.
    @Unique
    private static final ModelIdentifier VINE_BOW_INVENTORY_MODEL_ID =
            ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "vine_bow/vine_bow"));

    // Per-bow overrides. Key = exact item class; value = inventory model to use.
    // Add an entry here whenever a bow gets its own small inventory textures.
    @Unique
    private static final Map<Class<? extends Item>, ModelIdentifier> PER_BOW_INVENTORY_MODELS = Map.ofEntries(
            Map.entry(EarthBowItem.class, ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "earth_bow/earth_bow_inventory"))),
            Map.entry(IceBowItem.class, ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "ice_bow/ice_bow_inventory"))),
            Map.entry(BlossomBowItem.class, ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "blossom_bow/blossom_bow_inventory"))),
            Map.entry(BubbleBowItem.class, ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "bubble_bow/bubble_bow_inventory"))),
            Map.entry(BeeBowItem.class, ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "bee_bow/bee_bow_inventory")))
    );

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void simplybows$swapInventoryModel(
            ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, int overlay, BakedModel model, CallbackInfo ci) {

        if (SIMPLYBOWS_SUBSTITUTING.get()) return;
        if (!(stack.getItem() instanceof SimplyBowItem)) return;
        if (renderMode != ModelTransformationMode.GUI
                && renderMode != ModelTransformationMode.GROUND
                && renderMode != ModelTransformationMode.FIXED) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getBakedModelManager() == null) return;

        // Pick the right base model for this bow, falling back to vine_bow.
        ModelIdentifier inventoryModelId = PER_BOW_INVENTORY_MODELS.getOrDefault(
                stack.getItem().getClass(), VINE_BOW_INVENTORY_MODEL_ID);

        BakedModel baseInventoryModel = client.getBakedModelManager().getModel(inventoryModelId);
        if (baseInventoryModel == null) return;

        // Re-apply the inventory model's overrides using the original bow stack so that
        // the pulling predicates (which check entity.getActiveItem() == stack) fire correctly.
        ClientWorld world = client.world;
        LivingEntity entity = client.player;
        BakedModel resolvedModel = baseInventoryModel.getOverrides().apply(baseInventoryModel, stack, world, entity, 0);
        if (resolvedModel == null) resolvedModel = baseInventoryModel;

        // If the resolved model is already what's in use (e.g. vine_bow rendering itself), skip.
        if (resolvedModel == model) return;

        SIMPLYBOWS_SUBSTITUTING.set(true);
        try {
            ((ItemRenderer) (Object) this).renderItem(
                    stack, renderMode, leftHanded, matrices, vertexConsumers, light, overlay, resolvedModel);
        } finally {
            SIMPLYBOWS_SUBSTITUTING.set(false);
        }
        ci.cancel();
    }
}
