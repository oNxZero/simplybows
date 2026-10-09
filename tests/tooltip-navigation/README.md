Run the navigation regression checks with Java 21 or newer:

```sh
javac -d /tmp/simplybows-navigation-check common/src/main/java/net/sweenus/simplybows/client/tooltip/TooltipPageState.java tests/tooltip-navigation/NavigationRegression.java
java -cp /tmp/simplybows-navigation-check NavigationRegression
```

Checks quick keyboard taps between frames, duplicate render/build calls, wraparound, all per-bow rune pages, item changes, held mouse bindings, and hover reset. These checks do not launch Minecraft or exercise Mixin application.

After building, check the packaged NeoForge mixin against the actual Simply Tooltips release jar:

```sh
python3 tests/tooltip-navigation/check_renderer_target.py neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar /tmp/SimplyTooltips-neoforge-0.1.5.jar
```

This checks the exact overload descriptors and both native tab call sites. It does not execute Mixin or launch Minecraft.
