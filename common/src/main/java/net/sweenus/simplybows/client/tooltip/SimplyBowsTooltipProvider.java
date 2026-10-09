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
import net.sweenus.simplybows.util.BowTooltipPages;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Player-facing pages rendered by Simply Tooltips. */
public final class SimplyBowsTooltipProvider implements TooltipProvider {

    private static final String SLOT_HEADER_PREFIX = "item.modifiers.";
    private static SimplyBowsTooltipProvider activeProvider;

    public SimplyBowsTooltipProvider() { activeProvider = this; }

    public static void onKeyPress(int keyCode, int scanCode) {
        if (activeProvider == null) return;
        var key = activeProvider.cycleKey();
        boolean matches = key != null ? key.matchesKey(keyCode, scanCode) : keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_G;
        if (matches) activeProvider.navigation.keyPressed(System.nanoTime());
    }

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.getItem() instanceof SimplyBowItem || stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem);
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        List<Page> pages = new ArrayList<>();
        String bowKey = getBowKey(stack);
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        if (stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem component) {
            var kind = component.getUpgradeKind();
            RuneEtching rune = component.getRuneEtching();
            boolean isRune = kind == net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING;
            String intro = isRune ? BowTooltipPages.runeIntro(rune)
                    : kind == net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem.UpgradeKind.ENCHANTED_STRING
                    ? "Improve your bow's special ability with Enchanted String."
                    : "Strengthen your bow's special ability with a Reinforced Frame.";
            pages.add(new Page("Overview", List.of(intro,
                    "Apply in an anvil: bow in the first slot, upgrade in the second.",
                    isRune ? "Replaces the bow's current rune. Does not use an upgrade slot."
                    : "String and Frame share " + BowUpgradeData.getMaxTotalUpgradeSlots() + " upgrade slots.")));
            BowUpgradeData sample = new BowUpgradeData(0, 0, rune);
            for (String bow : BowTooltipPages.BOWS) {
                String name = Text.translatable("item.simplybows." + bow + "_bow." + bow + "_bow").getString();
                pages.add(new Page(name, List.of(isRune ? BowTooltipPages.rune(bow, rune)
                        : kind == net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem.UpgradeKind.ENCHANTED_STRING
                        ? BowTooltipPages.stringEffect(bow, sample) : BowTooltipPages.frameEffect(bow, sample))));
            }
        } else {
            List<String> overview = new ArrayList<>();
            overview.add(upgrades.runeEtching() == RuneEtching.NONE ? BowTooltipPages.ability(bowKey)
                    : BowTooltipPages.rune(bowKey, upgrades.runeEtching()));
            overview.add("Fully draw the bow to use its special ability.");
            if (upgrades.runeEtching() != RuneEtching.NONE) {
                pages.add(new Page("Bow Description", overview));
                pages.add(new Page("Rune Description", List.of(BowTooltipPages.rune(bowKey, upgrades.runeEtching()))));
            } else {
                pages.add(new Page("Bow Description", overview));
            }
            appendEnchantmentLines(overview, rawLines);
            pages.add(new Page("Upgrade Description", net.sweenus.simplybows.util.BowUpgradeTooltip.previewLines(bowKey, upgrades)));
            var player = net.minecraft.client.MinecraftClient.getInstance().player;
            pages.add(new Page("Combat Stats", net.sweenus.simplybows.util.BowCombatStats.lines(bowKey, upgrades,
                    net.sweenus.simplybows.util.CombatTargeting.getRangedWeaponDamageBonus(player, "projectile"),
                    net.sweenus.simplybows.util.CombatTargeting.getRangedWeaponDamageBonus(player, "ability"))));
        }
        Page active = pages.get(selectPage(stack, pages.size()));
        List<String> lines = new ArrayList<>();
        lines.add(ModernTooltipModel.SECTION_MARKER + active.name());
        for (String line : active.lines()) {
            if (line.isBlank()) continue;
            if (line.endsWith(":")) {
                if (lines.size() > 1 && !lines.getLast().isBlank()) lines.add("");
                lines.add(ModernTooltipModel.SECTION_MARKER + line.substring(0, line.length() - 1));
            } else if (active.name().equals("Upgrade Description") || active.name().equals("Combat Stats")) {
                lines.add(line);
            } else {
                String[] sentences = line.split("(?<=[.!?])\\s+(?=[A-Z])");
                for (String sentence : sentences) {
                    if (lines.size() > 1 && !lines.getLast().isBlank()) lines.add("");
                    lines.add(sentence);
                }
            }
        }
        net.sweenus.simplytooltips.api.UpgradeSection upgradeSection = null;
        if (stack.getItem() instanceof SimplyBowItem && pageIndex == 0) {
            var theme = TooltipTheme.defaultTheme();
            int maxSlots = BowUpgradeData.getMaxTotalUpgradeSlots();
            int maxLevel = BowUpgradeData.getMaxLevelPerType();
            var rows = List.of(
                    new net.sweenus.simplytooltips.api.UpgradeRow("◇", "String", theme.stringColor(), upgrades.stringLevel(),
                            Math.max(0, Math.min(maxLevel, maxSlots - upgrades.frameLevel())), altDown ? net.sweenus.simplybows.util.BowUpgradeTooltip.stringGain(bowKey, upgrades) : ""),
                    new net.sweenus.simplytooltips.api.UpgradeRow("◇", "Frame", theme.frameColor(), upgrades.frameLevel(),
                            Math.max(0, Math.min(maxLevel, maxSlots - upgrades.stringLevel())), altDown ? net.sweenus.simplybows.util.BowUpgradeTooltip.frameGain(bowKey, upgrades) : ""));
            var rune = new net.sweenus.simplytooltips.api.UpgradeRune(
                    Text.translatable("tooltip.simplybows.rune." + upgrades.runeEtching().id()).getString(),
                    upgrades.runeEtching() == RuneEtching.NONE, theme.runeColor(), List.of());
            upgradeSection = new net.sweenus.simplytooltips.api.UpgradeSection(maxSlots,
                    upgrades.stringLevel() + upgrades.frameLevel(), rows, rune);
        }
        return new ModernTooltipModel(stack.getName().getString(), List.of(stack.getItem() instanceof SimplyBowItem ? "UNIQUE BOW" : "BOW UPGRADE"), TooltipBorderStyle.DEFAULT,
                lines, List.of(), List.of(), TooltipTheme.defaultTheme(), upgradeSection,
                "|page:" + pageIndex + "|" + upgrades, bowKey,
                null);
    }

    private record Page(String name, List<String> lines) {}
    private int pageIndex;
    private final TooltipPageState navigation = new TooltipPageState();
    private net.minecraft.client.option.KeyBinding cycleKey;

    private net.minecraft.client.option.KeyBinding cycleKey() {
        if (cycleKey == null) {
            try {
                // Optional client integration: the compile-only API jar contains no keybind classes.
                cycleKey = (net.minecraft.client.option.KeyBinding) Class.forName("net.sweenus.simplytooltips.client.TooltipKeybinds")
                        .getField("CYCLE_TAB").get(null);
            } catch (ReflectiveOperationException ignored) { }
        }
        return cycleKey;
    }

    public void drawPageFooter(net.minecraft.client.gui.DrawContext context, int centerX, int y, ItemStack stack) {
        var font = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        int count = stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem
                ? 7 : BowUpgradeData.from(stack).runeEtching() == RuneEtching.NONE ? 3 : 4;
        int markerWidth = font.getWidth("+ ");
        var key = cycleKey();
        Text keyLabel = key == null ? Text.literal("G") : key.getBoundKeyLocalizedText();
        int keyWidth = font.getWidth(keyLabel);
        int x = centerX - (count * markerWidth + keyWidth + 4) / 2;
        boolean component = stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
        for (int i = 0; i < count; i++) {
            String bow = component && i > 0 ? BowTooltipPages.BOWS.get(i - 1) : getBowKey(stack);
            int color = i == pageIndex ? 0xFFFFFF : bowColor(bow);
            context.drawText(font, Text.literal("+"), x, y - 4, color, false);
            x += markerWidth;
        }
        context.drawText(font, keyLabel, x + 4, y - 4, 0xFFFFFF, false);
    }

    private static int bowColor(String bow) {
        return switch (bow) {
            case "vine" -> 0x85C76B;
            case "ice" -> 0x8BD4F2;
            case "bubble" -> 0x55C8E8;
            case "bee" -> 0xEDC65B;
            case "blossom" -> 0xF19DBC;
            case "earth" -> 0xC29A69;
            default -> 0xDB5E71;
        };
    }

    public boolean isOverviewRuneRow(ItemStack stack, Text text) {
        if (pageIndex != 0 || !(stack.getItem() instanceof SimplyBowItem)) return false;
        String value = text.getString();
        return value.equals("◎") || value.equals("Rune: ")
                || value.equals(Text.translatable("tooltip.simplybows.rune." + BowUpgradeData.from(stack).runeEtching().id()).getString());
    }

    /** Put the current page in the raw tooltip cache key, without adding visible text. */
    public List<Text> withPageCacheKey(ItemStack stack, List<Text> rawLines) {
        int count = stack.getItem() instanceof net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem
                ? 7 : BowUpgradeData.from(stack).runeEtching() == RuneEtching.NONE ? 3 : 4;
        selectPage(stack, count);
        List<Text> keyed = new ArrayList<>(rawLines);
        keyed.add(Text.empty().setStyle(net.minecraft.text.Style.EMPTY.withInsertion("simplybows:page:" + pageIndex)));
        return keyed;
    }

    private int selectPage(ItemStack stack, int count) {
        long now = System.nanoTime();
        String item = Registries.ITEM.getId(stack.getItem()) + "|" + BowUpgradeData.from(stack);
        var key = cycleKey();
        var bound = net.minecraft.client.util.InputUtil.fromTranslationKey(
                key == null ? "key.keyboard.g" : key.getBoundKeyTranslationKey());
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        // Keyboard events are queued, so even taps between rendered frames are retained.
        // Mouse-bound navigation uses a held-button edge because it has no keyboard event.
        boolean held = bound.getCategory() == net.minecraft.client.util.InputUtil.Type.MOUSE
                && org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, bound.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        pageIndex = navigation.update(item, count, held, now);
        return pageIndex;
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

}
