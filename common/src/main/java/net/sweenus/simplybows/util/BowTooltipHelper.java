package net.sweenus.simplybows.util;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;

import java.util.List;

public final class BowTooltipHelper {
    private static final int TOOLTIP_WRAP_CHARS = 36;

    public static final Style STYLE_UNIQUE_NAME = Style.EMPTY.withColor(TextColor.fromRgb(0xE2A834));
    public static final Style STYLE_SECTION = Style.EMPTY.withColor(TextColor.fromRgb(0xFFD5A0));
    public static final Style STYLE_BODY = Style.EMPTY.withColor(TextColor.fromRgb(0xE6ECF5));
    public static final Style STYLE_DIM = Style.EMPTY.withColor(TextColor.fromRgb(0xD3DCE9));
    public static final Style STYLE_STRING = Style.EMPTY.withColor(TextColor.fromRgb(0x9D62CA));
    public static final Style STYLE_FRAME = Style.EMPTY.withColor(TextColor.fromRgb(0x9D62CA));
    public static final Style STYLE_RUNE = Style.EMPTY.withColor(TextColor.fromRgb(0xDB5E71));
    public static final Style STYLE_HINT = Style.EMPTY.withColor(TextColor.fromRgb(0xC7D2E2));

    private BowTooltipHelper() {
    }

    public static void appendBowTooltip(String bowKey, ItemStack stack, List<Text> tooltip) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        boolean altDown = isAltDown();
        int usedSlots = upgrades.stringLevel() + upgrades.frameLevel();
        int maxSlots = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxString = Math.max(0, Math.min(BowUpgradeData.getMaxLevelPerType(), maxSlots - upgrades.frameLevel()));
        int maxFrame = Math.max(0, Math.min(BowUpgradeData.getMaxLevelPerType(), maxSlots - upgrades.stringLevel()));
        RuneEtching rune = upgrades.runeEtching();

        tooltip.add(Text.literal(" "));
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.section.ability"), STYLE_SECTION);
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.bow." + bowKey + ".ability"), STYLE_BODY);

        tooltip.add(Text.literal(" "));
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.section.upgrades"), STYLE_SECTION);
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.upgrades.slots", usedSlots, maxSlots), STYLE_BODY);
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.upgrades.string_level", upgrades.stringLevel(), maxString), STYLE_STRING);
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.upgrades.frame_level", upgrades.frameLevel(), maxFrame), STYLE_FRAME);
        addWrappedLine(tooltip, Text.translatable("tooltip.simplybows.upgrades.rune", Text.translatable("tooltip.simplybows.rune." + rune.id())), STYLE_RUNE);
        if (rune != RuneEtching.NONE) {
            addWrappedLine(tooltip, Text.literal(BowTooltipPages.rune(bowKey, rune)), STYLE_BODY);
        }
        if (altDown) {
            addWrappedLine(tooltip, Text.literal("String: " + BowUpgradeTooltip.stringGain(bowKey, upgrades)), STYLE_STRING);
            addWrappedLine(tooltip, Text.literal("Frame: " + BowUpgradeTooltip.frameGain(bowKey, upgrades)), STYLE_FRAME);
        }
    }

    public static void addWrappedLine(List<Text> tooltip, Text text, Style style) {
        String raw = text.getString();
        for (String line : wrap(raw, TOOLTIP_WRAP_CHARS)) {
            tooltip.add(Text.literal(line).setStyle(style));
        }
    }

    private static String getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine" -> "tooltip.simplybows.bow.vine.string_effect";
            case "earth" -> "tooltip.simplybows.bow.earth.string_effect";
            case "ice" -> "tooltip.simplybows.bow.ice.string_effect";
            case "bee" -> "tooltip.simplybows.bow.bee.string_effect";
            case "bubble" -> "tooltip.simplybows.bow.bubble.string_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.string_effect";
            default -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    private static String getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine" -> "tooltip.simplybows.bow.vine.frame_effect";
            case "earth" -> "tooltip.simplybows.bow.earth.frame_effect";
            case "ice" -> "tooltip.simplybows.bow.ice.frame_effect";
            case "bee" -> "tooltip.simplybows.bow.bee.frame_effect";
            case "bubble" -> "tooltip.simplybows.bow.bubble.frame_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.frame_effect";
            default -> "tooltip.simplybows.bow.generic.frame_effect";
        };
    }

    private static String getRuneEffectKey(String bowKey, RuneEtching rune) {
        String special = "tooltip.simplybows.bow." + bowKey + ".rune." + rune.id();
        return switch (bowKey) {
            case "bee", "vine", "earth", "ice", "bubble", "blossom" -> special;
            default -> "tooltip.simplybows.bow.generic.rune." + rune.id();
        };
    }

    private static boolean isAltDown() {
        if (SimplyBows.modernTooltipsEnabled()) return false;
        try {
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screen.Screen");
            Object result = screenClass.getMethod("hasAltDown").invoke(null);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<String> wrap(String text, int maxChars) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty() || maxChars <= 0) {
            lines.add(text == null ? "" : text);
            return lines;
        }

        int leadCount = 0;
        while (leadCount < text.length() && Character.isWhitespace(text.charAt(leadCount))) {
            leadCount++;
        }
        String leading = text.substring(0, leadCount);
        String content = text.substring(leadCount).trim();
        if (content.isEmpty()) {
            lines.add(text);
            return lines;
        }

        String[] words = content.split("\\s+");
        StringBuilder current = new StringBuilder(leading);
        int currentLen = leading.length();

        for (String word : words) {
            int needed = (currentLen > leading.length() ? 1 : 0) + word.length();
            if (currentLen + needed > maxChars && currentLen > leading.length()) {
                lines.add(current.toString());
                current = new StringBuilder(leading).append(word);
                currentLen = leading.length() + word.length();
                continue;
            }
            if (currentLen > leading.length()) {
                current.append(' ');
                currentLen++;
            }
            current.append(word);
            currentLen += word.length();
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}
