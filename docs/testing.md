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

Test Bounty against a moving player and bosses: three seconds of no movement, jumping, punching, item use, block breaking, or inventory actions, followed by normal control. Confirm it does not extend when another arrow hits during the freeze and that the frozen target can still take follow-up damage. Check shields, dragon body parts, mounts, death, disconnect, chunk unload, and restart cleanup. Confirm the crystal shell matches the target's size and never places real blocks.

Test Grace against hostile mobs and players, allies, and the caster, with normal, tipped, and spectral arrows and external damage attributes. Health must never decrease because of the shot; enemies must receive no debuffs. Partial shots consume no ammo. Fully drawn cooldown shots remain harmless and do not place another sanctuary. Verify String radius and Frame buff duration in tooltips and gameplay.
