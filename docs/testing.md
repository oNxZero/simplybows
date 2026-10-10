# Testing and troubleshooting

[Documentation home](../README.md)

## Validation status

Automated build, standalone regressions, and release-jar renderer target inspection are available. Full gameplay and a complete modpack integration pass have not been performed as part of these changes.

## In-game acceptance checklist

| Area | Check |
| :--- | :--- |
| Anvil consumption | Bow left + 30 Strings/Frames/runes middle consumes exactly one; reversed inputs and component pairs produce no output. |
| Upgrade limits | Reach the combined cap, try another component, try the same rune, then replace it with a different rune. |
| Rename | Apply an upgrade while setting/removing a custom name; check output name and extra XP level. |
| Shortcut removal | Right-click a component with a bow in the other hand: no upgrade or item consumption. |
| Tooltip controls | Tap and hold the page binding; switch hovered items; rebind the key; inspect all rune-item pages. |
| Tooltip layout | No duplicated headings, no numeric page counter, active plus white, no attribute lore; Alt gives compact bonuses. |
| Ammo | Split Winterfang arrows across stacks; test Pain multi-target volley, Grace/Bounty single shots, and rune-specific cooldown fallback; test creative, Infinity, and special ammo separately. |
| Persistence | Save/unload each of the seven projectile classes before impact with String/Frame/rune settings; reload and verify flight and exactly one impact ability. Include spectral Winterfang arrows. |
| Balance | Compare Everbloom rose/tree damage, Petalwind normal/Bounty pulses, Bubbleveil Bounty damage, and Chaos wave range/knockback. |
| Support | Check direct ally hits, area buffs/healing, hostile targets, teams, and friendly fire for every Grace bow. |
| Cooldown | Cast, swap bows, and verify shared lockout. Check Buzzkill's longer cooldown and ability-specific wall/field lockouts. |
| Loot | Use test loot tables and many rolls to check rates; do not infer a probability from one chest. Confirm `1` now means 0.1%. |
| Loaders | Repeat on native NeoForge and Fabric with Simply Tooltips 0.1.5, then with the intended modpack. |

## Tooltip crash

A crash mentioning `simplybows$useBowPages`, `TooltipRendererMixin`, and “0/1 succeeded” indicates the tooltip hook failed to match the renderer. Check the exact installed Simply Tooltips version and loader, remove duplicate Simply Bows jars, and verify that the new production jar is installed. The former common renderer hook was replaced by platform-specific hooks checked against 0.1.5.

Connector appearing in the report does not by itself establish it as the cause. The supplied crash's failure names the Simply Bows tooltip mixin. Keep the complete `latest.log` and crash report when diagnosing an additional conflict.

## First inventory open pauses, then works

A one-time pause can involve lazy class loading, mixin transformation, model initialization, or tooltip/rendering work. This pattern alone does not identify which mod causes it. Reproduce in a copy of the pack, compare logs around the first open, and use a Java profiler if needed. Avoid claiming this has been fixed without measuring the first-open path.

## Known limits

Config comments include historical descriptions: effective behavior may use hard-coded clamps or manager constants. The config reference calls out examples. Tooltip combat values are estimates before armor, enchantments, impact speed changes, and external mods.

The modern-tooltip-disabled Alt fallback has not received equivalent loader integration validation. Active fields/swarms and shared cooldowns are not comprehensively persisted through server restarts. Existing projectiles saved before the new state tags cannot recover unsaved settings. Other mods that replace anvil or tooltip behavior still need integration testing.

When reporting an issue, include loader/version, exact jar filenames, relevant config, bow/rune/String/Frame levels, reproduction steps, and the complete log. Mention whether the issue also happens with only the required dependencies.


## Winterfang redesign checks

Test Pain with zero and five Strings against spaced enemies, overlapping groups, a hostile player, a wither, and an ender dragon. Confirm distinct assignments, five-second slowing, no lingering damage zone, and no repeated splash damage to a single victim within one volley. Test an arrow unloading before impact.

Test Bounty against a moving player and bosses: 3 seconds plus 0.5 seconds per String of no movement, jumping, punching, item use, block breaking, or inventory actions, followed by normal control. Confirm it does not extend when another arrow hits during the freeze and that the frozen target can still take follow-up damage. Check shields, dragon body parts, mounts, death, disconnect, chunk unload, and restart cleanup. Confirm the crystal shell matches the target's size and never places real blocks.

Test Grace against hostile mobs and players, allies, and the caster, with normal, tipped, and spectral arrows and external damage attributes. Health must never decrease because of the shot; enemies must receive no debuffs. Partial shots consume no ammo. Fully drawn cooldown shots remain harmless and do not place another sanctuary. Verify String radius and Frame buff duration in tooltips and gameplay.

Bounty follow-up: check fixed 18-second cooldown at every Frame level, Frost I expires 10 seconds after impact despite paused victim ticks, and damage pulses occur every second while trapped (1 damage plus 0.5 per Frame before mitigation). Verify six-tick growth and eight-tick release/shatter, including unloading during either animation.

### Projectile enchantment checks

For each bow, compare unenchanted and Power V direct hits at the same draw, distance and upgrade level; test normal and spectral Winterfang arrows and Bubbleveil Pain shots. Compare Punch II knockback and Flame ignition. Verify ability damage does not increase, ability-spawned bees do not inherit these enchants, Winterfang Grace never damages or ignites enemies/allies, and other support shots never burn protected allies. Check Bounty remains anchored despite Punch, and newly fired arrows keep their enchantments after unload/reload. Existing arrows cannot recover enchantments they never saved.

### Infinity pickup checks

In Survival, fire Infinity normal arrows into a wall using every bow, including Winterfang fans and special rune shots. Neither the shooter nor another Survival player should collect free arrows. Without Infinity, spent recoverable arrows should still be collected. Repeat with consumed tipped/spectral arrows, Creative shots, and chunk unload/reload; pickup permissions must survive reload. Projectiles that discard on impact cannot be recovered regardless of enchantments.

### Homing Enderman checks

Shoot Endermen with Winterfang normal/Pain arrows, both normal and spectral ammo, and Buzzkill Pain bees. After an attempted entity hit the homing projectile must disappear even when the Enderman teleports and rejects damage. Check ordinary targets still receive normal hits and effects; Enderman subclasses should follow the same rule.

Also verify airborne homing arrows expire by 10 seconds when no collision happens, while grounded arrows keep normal recovery behavior.

Test the Legendary Monsters Pursuer and other modded teleporters: after acquiring a target, a position jump over four blocks in one tick must discard the homing projectile. Failed entity impacts also discard it regardless of entity class. Verify ordinary movement and switching targets do not trigger this cleanup. Verify bow enchantment rows use square pips in both loaders with Simply Tooltips 0.1.5.

## Rune formation regression and manual checks

Run the standalone cast-schedule regression:

```bash
javac -d /tmp/simplybows-rune-tests common/src/main/java/net/sweenus/simplybows/upgrade/RuneEtching.java common/src/main/java/net/sweenus/simplybows/util/RuneEffectRules.java tests/rune-formations/RuneFormationRegression.java
java -cp /tmp/simplybows-rune-tests RuneFormationRegression
```

It checks sting/pulse counts, continuation from saved elapsed times, reachable five-slot damage budgets, exactly three star waves, filled-star tips/interior/indentations and outward/inward motion, and rune routing. This does not start Minecraft or prove rendering/collision behavior.

Manual checks still required on each loader:

- Grace: walk into the wall from both sides as caster, ally and enemy. Try sprinting, jumping and riding. Shoot normal and high-speed arrows and an ender pearl through a segment. Go around the open side.
- Chaos: inspect stone growth, pull, closing hit at 1.5s and sinking animation; verify protected players are unaffected.
- Bounty star: inspect the filled eight-point shape on flat ground and steps. Watch three out/in cycles. A stationary target should take at most three hits; returning spikes must not add extra hits. Check targets between the old rays.
- Pain swarm: one delivery bee should attach seven animated visual bees only after a successful hit; count five stings at String 0, ten at String 5. Check rejected hits, target death and teleport cleanup.
- Pain vortex: inspect water-textured projectile, downward-flowing water, falling droplets and draining finish. Count four pulses plus a burst at String 0, six plus burst at String 5; compare one moving target against multiple targets.
- Bounty blossom: strike a group with more than three enemies. Exactly three distinct eligible enemies should receive blossoms; each takes one closing burst. Walk/run during the opening animation.
- Reload active formations and verify earlier damage does not repeat. Confirm new effects disappear after their durations and with absent owners. Existing manager effects retain their previous restart limitations.
- Compare tooltip String/Frame values at 0 and 5, and confirm no old axolotl-volley or rotating-sunder descriptions remain in the active pages.

Check Chaos while shooting as a player: the 7.5s cooldown should start once at launch, and the stone jaws must still appear on impact. During cooldown, subsequent arrows should have no new jaws. If Multishot is supplied by another mod, only the first arrow carries the Chaos effect. For each attached formation, kill its target or teleport it far away: damage stops immediately, but the visual should finish over 0.6s rather than disappear. Count all normal final pulses before the finish begins; save/reload during the finish must not resume damage.

`python3 tests/rune-formations/check_cast_lifecycle.py <production-bows.jar>` checks the packaged launch/impact cooldown contract, saved finish marker and water renderer for each platform. It also checks all six projectile factories honor the partial-draw vanilla fallback. In multiplayer, spam partial draws with Bubbleveil Pain during cooldown: no new clouds should appear. Repeat with Buzzkill runes, and verify full draws during cooldown cannot add ability fields. Switch bows and dimensions while cooling down: the shared lock should remain active.

### Area raincloud and hunting blades

Run `python3 tests/rune-formations/check_area_projectiles.py <production jar>` for each loader. It checks both impact entry points, the guarded area cast, saved projectile damage/target state, terrain collision, flight/teleport limits and packaged water geometry. The renderer source check rejects wool geometry. This does not launch Minecraft.

In Minecraft, shoot Bubbleveil Pain into the floor with several enemies nearby: check the continuous 3D cloud shape and particle edges, three visible homing drops per volley, terrain cover, final downpour at all six String levels, and fading. Shoot Tremorstrike Pain through a crowd and verify each enemy is damaged/launched once, including the directly struck enemy. Check Bounty's audible rise/retract on all three cycles, Chaos teeth breaking on collision, and Petalwind Bounty's three distinct hunting blades. Visual quality, sound mixing and PvP balance remain in-game checks.
