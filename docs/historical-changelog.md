# Historical changelog

These notes describe earlier revisions. Current installation, tooltip behavior, and balance are documented in the main guides; historical instructions can be obsolete.

# Simply Bows Reforged

Fork of [Sweenus/simplybows](https://github.com/Sweenus/simplybows). See [README.md](README.md) for install and overview.

Item ids of the six remaining bows are unchanged. Echo and Starweave items become invalid.

Install `neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar` on the server and every client. Delete `config/simplybows/config.toml` once after updating so renamed keys appear. Live files keep old values: Winterfang `painDamageMultiplier = 1.2`, Everbloom `friendlyHeal = 2.0`, Bubbleveil `painShotSpeedLand = 0.9` / `painShotSpeedWater = 1.05`.

## Everbloom

- Full draw plants the flower field. Short draw is still a healing arrow.
- Players, animals, and iron golems take no arrow damage. Direct hit heals (1 heart Frame 0, 3.5 Frame 5).
- Field heal only on the visible flower patch. Without Grace: lowest absolute HP. With Grace: all valid targets.
- One field per player across every Everbloom bow. Shared cooldown. Direct heals still work while locked.
- Field regen drips across the second; overlapping fields do not stack. Frame 0 ≈ 0.5 heart/s, Frame 5 ≈ 2 hearts/s at `friendlyHeal = 2.0`.
- Monsters take damage in the larger field radius. String grows the visible patch; tooltip shows that radius.
- Patch fills center → grows out → sinks outside-in. Grace cherry tree matches.

## Winterfang

- Pain default damage multiplier `1.2`, short cooldown. Grace stacks Slowness (short CD). Bounty extra arrows (short CD). Chaos frost wall keeps its longer cooldown.

## Bubbleveil

- Pain axolotl line uses `painShotSpeedLand` / `painShotSpeedWater` so full-draw land shots have real range.
- Grace and Bounty columns spawn on land or underwater, with axolotl visuals and effects.
- Chaos wave on full draw. Pain / Grace / Bounty / Chaos have cooldowns. Normal Pain shots during CD still fire a basic bubble arrow.

## Buzzkill

- Pain bees home strongly; CD ~4s. Grace orbits struck target at ~2 blocks, Resistance I–V from Frame for nearby players; CD matches shield length. Bounty hive CD matches hive life. Chaos honey storm ~13s then quick fade (no leftover campfire smoke); longer post-storm CD.

## Petalwind

- Pain: small ring (~2.8 + little String), ~45% storm damage, CD after. Grace: nearest ally at arrow or hit target. Bounty: ground trap ring, triggers on step-in. Chaos: koi at impact; orbit hit target if any. Cooldowns scale with strength.

## Tremorstrike

- Pain waves knock up once lightly, not every step into the sky. Chaos: wider moving sunder trail (multi-spike visual) with CD after. Spike field lockout unchanged.

## Shared systems

- Short `RuneUseCooldown` (and longer existing locks) for rune specials that used to spam.
- Hotbar ability overlay is translucent so the bow icon stays visible. Does not use vanilla item cooldown lock.
- Hold Alt: String / Frame gains plus a short per-bow rune description.
- Upgrades in `simplybows:upgrades` component; legacy `custom_data` migrates once.
- Out-animations on zones (flowers, hive, honey, petal storms, etc.).

## Removed

- Starweave (Cosmic) and Echo removed from items, loot, tooltips, models, and config. Koi visuals stay for Petalwind Chaos.
