package net.sweenus.simplybows.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;
import java.util.List;

/** Keep equipment attributes out of bow lore without changing actual attributes. */
@Mixin(ItemStack.class)
public abstract class BowAttributeTooltipMixin {
    @ModifyReturnValue(method = "getTooltip", at = @At("RETURN"))
    private List<Text> simplybows$removeAttributeLore(List<Text> lines) {
        if (!(((ItemStack) (Object) this).getItem() instanceof SimplyBowItem)) return lines;
        List<Text> result = new ArrayList<>(lines);
        result.removeIf(BowAttributeTooltipMixin::simplybows$isAttribute);
        return result;
    }

    @Unique
    private static boolean simplybows$isAttribute(Text text) {
        if (text.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();
            if (key.startsWith("attribute.") || key.startsWith("item.modifiers.")) return true;
            for (Object arg : content.getArgs()) {
                if (arg instanceof Text nested && simplybows$isAttribute(nested)) return true;
            }
        }
        return text.getSiblings().stream().anyMatch(BowAttributeTooltipMixin::simplybows$isAttribute);
    }
}
