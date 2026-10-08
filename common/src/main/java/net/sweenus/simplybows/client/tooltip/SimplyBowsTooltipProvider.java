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
import net.sweenus.simplybows.util.BowUpgradeTooltip;
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

/**
 * Bridge provider that gives Simply Bows items a rich modern tooltip
 * rendered by the Simply Tooltips engine.
 *
 * <p>This class uses only the ST API types and reads bow data directly
 * from {@link BowUpgradeData} rather than parsing the vanilla tooltip lines.
 *
 * <p>Theme colors are resolved by Simply Tooltips' {@code ThemeRegistry} using
 * {@code themeKey = bowKey} (e.g. {@code "vine"}, {@code "bee"}).  Pip colors
 * (String / Frame / Rune) use the ST default theme's fixed accent colors so
 * they remain legible across all bow themes.
 */
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

        // Title
        String title = rawLines.isEmpty()
                ? stack.getName().getString()
                : rawLines.get(0).getString();

        // Build ability text directly from translation key so Simply Tooltips can
        // perform one pixel-width wrap pass (avoids double-wrap artifacts).
        List<String> abilityLines = getAbilityLines(bowKey, rawLines);
        appendEnchantmentLines(abilityLines, rawLines);

        // Fixed accent colors — same as ST default theme, consistent across all bow themes
        TooltipTheme defaults = TooltipTheme.defaultTheme();

        // Build upgrade section from NBT data
        int maxSlots  = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxPerType = BowUpgradeData.getMaxLevelPerType();
        int usedSlots  = upgrades.stringLevel() + upgrades.frameLevel();
        int maxString  = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.frameLevel()));
        int maxFrame   = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.stringLevel()));

        RuneEtching rune = upgrades.runeEtching();
        boolean isNone = rune == RuneEtching.NONE;
        String runeName = Text.translatable("tooltip.simplybows.rune." + rune.id()).getString();
        List<String> runeEffectLines = new ArrayList<>(BowUpgradeTooltip.runeLines(bowKey, upgrades));
        if (!isNone) {
            appendRuneDescription(runeEffectLines, bowKey, rune);
        }

        UpgradeRune upgradeRune = new UpgradeRune(runeName, isNone, defaults.runeColor(), runeEffectLines);

        List<UpgradeRow> rows = List.of(
                new UpgradeRow("◇", "String", defaults.stringColor(), upgrades.stringLevel(), maxString,
                        BowUpgradeTooltip.stringGain(bowKey)),
                new UpgradeRow("◇", "Frame", defaults.frameColor(), upgrades.frameLevel(), maxFrame,
                        BowUpgradeTooltip.frameGain(bowKey))
        );

        UpgradeSection upgradeSection = new UpgradeSection(maxSlots, usedSlots, rows, upgradeRune);

        // STATS tab: numeric string/frame/rune breakdown (more detail than LORE).
        List<String> detailLines = new ArrayList<>(BowUpgradeTooltip.detailLines(bowKey, upgrades));

        String animKeyExtra = "|s:" + upgrades.stringLevel()
                + "|f:" + upgrades.frameLevel()
                + "|r:" + rune.id();

        // themeKey = bowKey: TooltipRenderer looks up the full ThemeDefinition (colors + motif)
        // in ThemeRegistry, so vine bows get the vine theme, bee bows get bee, etc.
        return new ModernTooltipModel(
                title,
                List.of("UNIQUE", "BOW"),
                TooltipBorderStyle.DEFAULT,
                abilityLines,
                detailLines,
                List.of(),
                TooltipTheme.defaultTheme(),
                upgradeSection,
                animKeyExtra,
                bowKey,
                null  // hint=null
        );
    }

    // --- Ability section parsing ---

    private static List<String> getAbilityLines(String bowKey, List<Text> rawLines) {
        String abilityKey = "tooltip.simplybows.bow." + bowKey + ".ability";
        String translated = Text.translatable(abilityKey).getString();

        // Missing keys resolve back to the key string; use raw tooltip parsing as fallback.
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

        // BowTooltipHelper adds: blank → abilityHeader → ability lines → blank → upgradesHeader → ...
        // We look for the ability section using known translation keys.
        String abilityHeaderText  = Text.translatable("tooltip.simplybows.section.ability").getString().trim();
        String upgradesHeaderText = Text.translatable("tooltip.simplybows.section.upgrades").getString().trim();

        List<String> result = new ArrayList<>();
        boolean inAbilitySection = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s       = rawLines.get(i).getString();
            String trimmed = s.trim();

            if (trimmed.equals(abilityHeaderText)) {
                inAbilitySection = true;
                continue;
            }
            if (trimmed.equals(upgradesHeaderText)) {
                break; // stop at upgrades section
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
        // Items are registered as e.g. "vine_bow/vine_bow" — take the last path segment
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.endsWith("_bow") ? name.substring(0, name.length() - 4) : "generic";
    }

    // --- Translation key helpers ---

    private static String getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine"    -> "tooltip.simplybows.bow.vine.string_effect";
            case "earth"   -> "tooltip.simplybows.bow.earth.string_effect";
            case "ice"     -> "tooltip.simplybows.bow.ice.string_effect";
            case "bee"     -> "tooltip.simplybows.bow.bee.string_effect";
            case "bubble"  -> "tooltip.simplybows.bow.bubble.string_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.string_effect";
            default        -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    private static String getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine"    -> "tooltip.simplybows.bow.vine.frame_effect";
            case "earth"   -> "tooltip.simplybows.bow.earth.frame_effect";
            case "ice"     -> "tooltip.simplybows.bow.ice.frame_effect";
            case "bee"     -> "tooltip.simplybows.bow.bee.frame_effect";
            case "bubble"  -> "tooltip.simplybows.bow.bubble.frame_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.frame_effect";
            default        -> "tooltip.simplybows.bow.generic.frame_effect";
        };
    }

    private static void appendRuneDescription(List<String> lines, String bowKey, RuneEtching rune) {
        String key = getRuneEffectKey(bowKey, rune);
        String translated = Text.translatable(key).getString();
        if (translated == null || translated.isBlank() || key.equals(translated)) {
            return;
        }
        String trimmed = translated.replace('\u00A0', ' ').trim();
        if (trimmed.isEmpty()) {
            return;
        }
        for (String part : trimmed.split("\\R")) {
            String line = part.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
    }

    private static String getRuneEffectKey(String bowKey, RuneEtching rune) {
        return switch (bowKey) {
            case "bee", "vine", "earth", "ice", "bubble", "blossom" ->
                "tooltip.simplybows.bow." + bowKey + ".rune." + rune.id();
            default -> "tooltip.simplybows.bow.generic.rune." + rune.id();
        };
    }
}
