package net.sweenus.simplybows.client.tooltip;

/** Hover navigation independent of renderer caching and keyboard bindings. */
public final class TooltipPageState {
    private String item = "";
    private int page;
    private long lastHover;
    private boolean held;
    private int pendingPresses;

    public void keyPressed(long now) {
        if (!item.isEmpty() && now - lastHover <= 500_000_000L) pendingPresses++;
    }

    public int update(String currentItem, int count, boolean keyDown, long now) {
        if (!currentItem.equals(item) || now - lastHover > 500_000_000L) {
            item = currentItem;
            page = 0;
            held = keyDown;
            pendingPresses = 0;
        }
        page = (page + pendingPresses) % count;
        pendingPresses = 0;
        if (keyDown && !held) page = (page + 1) % count;
        held = keyDown;
        lastHover = now;
        page %= count;
        return page;
    }
}
