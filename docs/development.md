# Development

[Documentation home](../README.md) · [Test checklist](testing.md)

## Build prerequisites

Java 21 and the checked-in Gradle 8.9 wrapper. Version pins live in `gradle.properties`; the project uses Architectury with common, Fabric, and NeoForge modules.

The build also requires a **separate Simply Tooltips common development API jar** at:

```text
../simplytooltips/common/build/devlibs/SimplyTooltips-common-*-dev.jar
```

This path is configured by `simplyTooltipsCommonDevJar` in the root `build.gradle`. Obtain a compatible development artifact from a matching Simply Tooltips source build before building this project. A release loader jar is not automatically a substitute for the mapped common API artifact. The local workspace currently uses an API stub named `SimplyTooltips-common-stub-dev.jar`; that external artifact is not included in this repository. A fresh clone alone therefore does not supply every build prerequisite.

```bash
./gradlew build --no-daemon
```

With dependencies already cached:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew --offline build --no-daemon
```

Install the production jars listed in [installation](installation.md), not `sources`, development, or shadow artifacts. Build success verifies compilation and packaging; it does not execute Minecraft integration tests.

## Source map

| Location | Responsibility |
| :--- | :--- |
| `common/.../item/unique` | Charge/release behavior, ammunition, rune activation, launch damage |
| `common/.../entity` | Projectile flight, impact effects, visual entities, saved projectile state |
| `common/.../world` | Ticking fields, swarms, storms, walls, target selection, cooldowns; IcePrisonManager enforces freeze, IcePainVolley reserves targets, IceFrostSlowManager handles status-immune bosses |
| `common/.../upgrade` | Rune enum, String/Frame levels, item data codecs and caps |
| `common/.../item/upgrade` | Upgrade component application rules and XP costs |
| `common/.../mixin/AnvilScreenHandlerMixin.java` | Slot order, one-item consumption, renaming while upgrading |
| `common/.../client/tooltip` | Modern tooltip provider, page tracking, navigation |
| `common/.../util/BowTooltipPages.java` | Short contextual descriptions |
| `common/.../util/BowUpgradeTooltip.java` | Current/next upgrade values and numeric formatting |
| `common/.../util/BowCombatStats.java` | Combat estimates and relevant support stats |
| `common/.../util/BowAbilityBalance.java` | Shared balance factors and wave range calculation |
| `common/.../loot` | Shared chest pools and uniform chance conversion |
| `common/.../config` | All 212 validated settings |
| `common/.../command` | Operator hostile spawning command |
| `common/.../registry` | Item, entity, and component registration |
| `fabric/...` and `neoforge/...` | Loader entrypoints, loot hooks, renderer mixins |
| `common/src/main/resources` | Models, textures, translations, common mixin configuration |

Here `common/...` abbreviates `common/src/main/java/net/sweenus/simplybows` and equivalent platform packages. The repository's Java source is the complete implementation reference for manager-specific clamps, cleanup, rendering, and targeting.

## Tooltip compatibility

Simply Tooltips uses different runtime descriptors on the two loaders. Each platform has its own `TooltipRendererMixin` under the same class name. NeoForge targets Mojmap types; Fabric targets intermediary types. Keep the platform classes separate, and verify the packaged annotations against the actual dependency jar rather than only compiling against an API stub.

Common keyboard and attribute hooks handle queued key taps and removal of unwanted attribute lore. Page state includes item changes and a 500 ms hover gap. Modern rendering uses custom page headings, rune-specific descriptions, grouped upgrade previews, colored page markers, and the configured key label.

## Save data

Bow stacks use the `simplybows:upgrades` data component. Legacy `simplybows_upgrades` custom NBT is read for compatibility. String, Frame, and rune data are copied into projectile NBT.

| Projectile | Additional state preserved |
| :--- | :--- |
| Vine | Flower-field placement and ability-spawn flags |
| Blossom | Ability-spawn flag |
| Bubble | Ability-spawn flag and column owner UUID |
| Bee | Full draw, sound/hive/storm flags, homing mode, hive/dive mode, dive damage and radius |
| Earth | Field-spawn and Chaos-on-impact flags |
| Homing arrow and homing spectral arrow | Spread/target-lock flags, rune modes, upgrade levels, bloom/wall creation flags, homing enablement, flight age, locked target UUID |

Vanilla superclass save/read methods remain in use. Saved flags prevent repeated one-shot effects after reload. A missing or invalid target can require reacquisition. Missing old tags retain fallback behavior; data never saved by older builds cannot be reconstructed.

Active world managers and shared cooldown maps are runtime state, not comprehensive world-persistent systems. Saving projectile settings does not guarantee every already active field or swarm survives a server restart. Existing over-cap upgrade stacks are not globally redistributed or discarded merely because the slot cap changes.

## Regression commands

Run from the repository root with Java 21:

```bash
javac -d /tmp/simplybows-balance-check common/src/main/java/net/sweenus/simplybows/upgrade/RuneEtching.java common/src/main/java/net/sweenus/simplybows/util/BowAbilityBalance.java tests/ability-balance/BalanceRegression.java
java -cp /tmp/simplybows-balance-check BalanceRegression
javac -d /tmp/simplybows-navigation-check common/src/main/java/net/sweenus/simplybows/client/tooltip/TooltipPageState.java tests/tooltip-navigation/NavigationRegression.java
java -cp /tmp/simplybows-navigation-check NavigationRegression
javac -d /tmp/simplybows-loot-check common/src/main/java/net/sweenus/simplybows/loot/LootChance.java tests/loot-chance/LootChanceRegression.java
java -cp /tmp/simplybows-loot-check LootChanceRegression
python3 tests/tooltip-navigation/check_renderer_target.py neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar /path/to/SimplyTooltips-neoforge-0.1.5.jar
python3 tests/tooltip-navigation/check_renderer_target.py fabric/build/libs/SimplyBows-fabric-0.1.4.jar /path/to/SimplyTooltips-fabric-0.1.5.jar
```

Balance checks cover damage ratios, external bonuses, knockback, and exact wave endpoints. Navigation checks cover quick taps, wraparound, nested rendering, item changes, and hover resets. Loot checks cover fractional inputs, the former boundary at 1, unchanged defaults, clamps, and invalid numeric inputs. Renderer checks inspect packaged target descriptors and invocation sites with `javap`; they do not launch the game or prove compatibility with every other mixin.


Winterfang's arrow NBT also stores the Pain volley UUID and Grace sanctuary enablement. The locked-target UUID is reserved at launch and retained across unloading. Grace cooldown shots persist as harmless without becoming an active sanctuary on reload. Ice prison visuals save their remaining lifetime so orphan shells expire. Freeze state itself is temporary server runtime state.

Additional rune regression:

```bash
javac -d /tmp/simplybows-winterfang-check common/src/main/java/net/sweenus/simplybows/util/WinterfangAbilityRules.java tests/winterfang/WinterfangRegression.java
java -cp /tmp/simplybows-winterfang-check WinterfangRegression
```

Packaged freeze hook inspection (supply the corresponding Loom mapped Minecraft jar for each loader):

```bash
python3 tests/winterfang/check_freeze_hooks.py /path/to/production-bows.jar /path/to/mapped-minecraft.jar
```

This verifies every new freeze/action hook names a method in the runtime namespace. It does not execute the injection or validate other mods' overrides.
