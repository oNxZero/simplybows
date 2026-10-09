import net.sweenus.simplybows.client.tooltip.TooltipPageState;

public class NavigationRegression {
    public static void main(String[] args) {
        var state = new TooltipPageState();
        long now = 1_000_000_000L;
        check(state.update("bow", 5, false, now) == 0, "overview on first hover");
        state.keyPressed(now + 1);
        // Press and release happened before the next frame; the queued event must survive.
        check(state.update("bow", 5, false, now + 2) == 1, "quick G tap advances");
        check(state.update("bow", 5, false, now + 3) == 1, "nested render/build must not advance twice");
        for (int expected : new int[]{2,3,4,0}) {
            state.keyPressed(++now + 3);
            check(state.update("bow", 5, false, now + 4) == expected, "one page per press and wraparound");
        }
        state.keyPressed(now + 5);
        check(state.update("rune", 7, false, now + 6) == 0, "moving to a different item clears stale keypress");
        for (int expected : new int[]{1,2,3,4,5,6,0}) {
            state.keyPressed(++now + 6);
            check(state.update("rune", 7, false, now + 7) == expected, "all six per-bow rune pages reachable");
        }
        check(state.update("other bow", 4, true, now + 8) == 0, "held mouse binding does not skip overview");
        check(state.update("other bow", 4, true, now + 9) == 0, "holding binding does not repeat");
        state.update("other bow", 4, false, now + 10);
        check(state.update("other bow", 4, true, now + 11) == 1, "fresh mouse press advances");
        check(state.update("other bow", 4, false, now + 1_000_000_000L) == 0, "returning to item resets overview");
        System.out.println("Passed: quick taps, nested render caching, wraparound, seven rune pages, item changes, held bindings, hover reset.");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
