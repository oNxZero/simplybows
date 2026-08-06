package net.sweenus.simplybows.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;
import net.sweenus.simplytooltips.api.UpgradeRow;
import net.sweenus.simplytooltips.api.UpgradeRune;
import net.sweenus.simplytooltips.api.UpgradeSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SimplyBowsTooltipProvider implements TooltipProvider {

    private static final String SLOT_HEADER_PREFIX = "item.modifiers.";

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SimplyBowItem;
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String bowKey = getBowKey(stack);
        BowUpgradeData upgrades = BowUpgradeData.from(stack);

        String title = rawLines.isEmpty()
                ? stack.getName().getString()
                : rawLines.get(0).getString();

        List<String> abilityLines = getAbilityLines(bowKey, rawLines);
        appendEnchantmentLines(abilityLines, rawLines);

        TooltipTheme defaults = TooltipTheme.defaultTheme();

        int maxSlots = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxPerType = BowUpgradeData.getMaxLevelPerType();
        int usedSlots = upgrades.stringLevel() + upgrades.frameLevel();
        int maxString = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.frameLevel()));
        int maxFrame = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.stringLevel()));

        RuneEtching rune = upgrades.runeEtching();
        boolean isNone = rune == RuneEtching.NONE;
        String runeName = Text.translatable("tooltip.simplybows.rune." + rune.id()).getString();
        String runeEffectKey = isNone
                ? "tooltip.simplybows.rune.none_effect"
                : getRuneEffectKey(bowKey, rune);
        String runeEffectRaw = Text.translatable(runeEffectKey).getString();
        List<String> runeEffectLines = runeEffectRaw.isBlank() ? List.of() : List.of("  " + runeEffectRaw);

        UpgradeRune upgradeRune = new UpgradeRune(runeName, isNone, defaults.runeColor(), runeEffectLines);

        List<UpgradeRow> rows = List.of(
                new UpgradeRow("◇", "String", defaults.stringColor(), upgrades.stringLevel(), maxString,
                        Text.translatable(getStringEffectKey(bowKey)).getString()),
                new UpgradeRow("◇", "Frame", defaults.frameColor(), upgrades.frameLevel(), maxFrame,
                        Text.translatable(getFrameEffectKey(bowKey)).getString())
        );

        UpgradeSection upgradeSection = new UpgradeSection(maxSlots, usedSlots, rows, upgradeRune);

        String animKeyExtra = "|s:" + upgrades.stringLevel()
                + "|f:" + upgrades.frameLevel()
                + "|r:" + rune.id();

        return new ModernTooltipModel(
                title,
                List.of("UNIQUE", "BOW"),
                TooltipBorderStyle.DEFAULT,
                abilityLines,
                List.of(),
                List.of(),
                TooltipTheme.defaultTheme(),
                upgradeSection,
                animKeyExtra,
                bowKey,
                null
        );
    }

    private static List<String> getAbilityLines(String bowKey, List<Text> rawLines) {
        String abilityKey = "tooltip.simplybows.bow." + bowKey + ".ability";
        String translated = Text.translatable(abilityKey).getString();

        if (translated == null || translated.isBlank() || abilityKey.equals(translated)) {
            return parseAbilityLinesFromRaw(rawLines);
        }

        List<String> lines = new ArrayList<>();
        for (String line : translated.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line.trim());
            }
        }
        return lines.isEmpty() ? parseAbilityLinesFromRaw(rawLines) : lines;
    }

    private static List<String> parseAbilityLinesFromRaw(List<Text> rawLines) {
        if (rawLines.size() < 2) return List.of();

        String abilityHeaderText = Text.translatable("tooltip.simplybows.section.ability").getString().trim();
        String upgradesHeaderText = Text.translatable("tooltip.simplybows.section.upgrades").getString().trim();

        List<String> result = new ArrayList<>();
        boolean inAbilitySection = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
            String trimmed = s.trim();

            if (trimmed.equals(abilityHeaderText)) {
                inAbilitySection = true;
                continue;
            }
            if (trimmed.equals(upgradesHeaderText)) {
                break;
            }
            if (inAbilitySection && !trimmed.isEmpty()) {
                result.add(s);
            }
        }

        return result;
    }

    private static void appendEnchantmentLines(List<String> abilityLines, List<Text> rawLines) {
        List<String> enchantmentLines = getEnchantmentLines(rawLines);
        if (enchantmentLines.isEmpty()) {
            return;
        }

        abilityLines.add(ModernTooltipModel.SECTION_MARKER + "Enchantments");
        abilityLines.addAll(enchantmentLines);
    }

    private static List<String> getEnchantmentLines(List<Text> rawLines) {
        if (rawLines.size() < 2) return List.of();

        String holdAltText = Text.translatable("tooltip.simplybows.hold_alt").getString().trim();
        boolean afterBowSections = false;
        boolean inAttributeBlock = false;
        List<String> result = new ArrayList<>();

        for (int i = 1; i < rawLines.size(); i++) {
            Text line = rawLines.get(i);
            String trimmed = line.getString().trim();

            if (!afterBowSections) {
                if (trimmed.equals(holdAltText)) {
                    afterBowSections = true;
                }
                continue;
            }

            if (trimmed.isEmpty()) {
                inAttributeBlock = false;
                continue;
            }
            if (isAttributeContextLine(line)) {
                inAttributeBlock = true;
                continue;
            }
            if (inAttributeBlock) {
                continue;
            }

            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    private static boolean isAttributeContextLine(Text line) {
        if (hasTranslatableKey(line, key -> key.startsWith(SLOT_HEADER_PREFIX))) {
            return true;
        }

        String s = line.getString().replace('\u00A0', ' ').trim();
        String lower = s.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("when held:")
                || (lower.startsWith("when in ") && lower.endsWith(":"));
    }

    private static boolean hasTranslatableKey(Text text, Predicate<String> matcher) {
        if (text == null) return false;
        return hasTranslatableKey0(text, matcher, 0);
    }

    private static boolean hasTranslatableKey0(Text text, Predicate<String> matcher, int depth) {
        if (depth > 8) return false;

        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            if (matcher.test(translatable.getKey())) return true;
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Text nested && hasTranslatableKey0(nested, matcher, depth + 1)) {
                    return true;
                }
            }
        }

        for (Text sibling : text.getSiblings()) {
            if (hasTranslatableKey0(sibling, matcher, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    // --- Bow key from registry ID ---

    private static String getBowKey(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id == null || !"simplybows".equals(id.getNamespace())) return "generic";
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.endsWith("_bow") ? name.substring(0, name.length() - 4) : "generic";
    }

    private static String getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine"    -> "tooltip.simplybows.bow.vine.string_effect";
            case "earth"   -> "tooltip.simplybows.bow.earth.string_effect";
            case "echo"    -> "tooltip.simplybows.bow.echo.string_effect";
            case "cosmic"  -> "tooltip.simplybows.bow.cosmic.string_effect";
            case "ice"     -> "tooltip.simplybows.bow.ice.string_effect";
            case "bee"     -> "tooltip.simplybows.bow.bee.string_effect";
            case "bubble"  -> "tooltip.simplybows.bow.bubble.string_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.string_effect";
            default -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    private static String getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine"    -> "tooltip.simplybows.bow.vine.frame_effect";
            case "earth"   -> "tooltip.simplybows.bow.earth.frame_effect";
            case "echo"    -> "tooltip.simplybows.bow.echo.frame_effect";
            case "cosmic"  -> "tooltip.simplybows.bow.cosmic.frame_effect";
            case "ice"     -> "tooltip.simplybows.bow.ice.frame_effect";
            case "bee"     -> "tooltip.simplybows.bow.bee.frame_effect";
            case "bubble"  -> "tooltip.simplybows.bow.bubble.frame_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.frame_effect";
            default -> "tooltip.simplybows.bow.generic.frame_effect";
        };
    }

    private static String getRuneEffectKey(String bowKey, RuneEtching rune) {
        return switch (bowKey) {
            case "bee", "vine", "earth", "ice", "echo", "cosmic", "bubble", "blossom" ->
                "tooltip.simplybows.bow." + bowKey + ".rune." + rune.id();
            default -> "tooltip.simplybows.bow.generic.rune." + rune.id();
        };
    }
}
