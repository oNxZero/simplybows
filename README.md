# Simply Bows Reforged

A balance and feel fork of [Sweenus/simplybows](https://github.com/Sweenus/simplybows) for NeoForge 1.21.1.

Item IDs for the six remaining bows are unchanged, so existing Winterfang, Everbloom, Bubbleveil, Buzzkill, Petalwind, and Tremorstrike items stay valid. Echo and Starweave are removed.

Upstream author: [Sweenus](https://github.com/Sweenus). This fork keeps the Timefall Development License 1.2 from upstream.

## Install

Build:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
gradle :neoforge:remapJar
```

Install `neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar` on the **server and every client**. Do not run it beside the Modrinth jar.

After a first update, delete `config/simplybows/config.toml` once so renamed keys are written. A live file keeps old numbers:

- `[winterfang] painDamageMultiplier` should be `1.2` (not `1.5`)
- `[everbloom] friendlyHeal` should stay `2.0`
- Bubble Pain uses `painShotSpeedLand` / `painShotSpeedWater` (not the old multiplier keys)

Requires [Simply Tooltips](https://modrinth.com/mod/simply-tooltips) for the modern bow tooltips.

## What Reforged changes

### Shared rules

- Unique ability shots need a full draw where that bow has a special.
- Runes that used to fire every shot now have short cooldowns. Stronger effects keep longer locks. The hotbar shows a see-through cooldown overlay; the bow itself still fires.
- Hold **Alt** on a unique bow to see String and Frame gains, plus a short description of the etched rune on that bow.
- Upgrades live in the `simplybows:upgrades` component so they survive mods that wipe `minecraft:custom_data` on death. Already-wiped upgrades cannot be restored.
- Effect zones grow in and sink out instead of popping.

### Everbloom

Healing bow. Full draw plants flowers; short draw still heals on hit.

- Players, animals, and iron golems take no arrow damage and are healed on direct hit.
- Field heal only on the visible flower patch. Without Grace: lowest absolute HP. With Grace: all of those targets.
- One Everbloom field per player at a time, across every Everbloom item. Shared cooldown. Direct heals still work while locked.
- String grows the visible patch. Tooltip shows that radius, not the larger monster aura.

### Winterfang

- Pain focuses one target at `1.2x` damage, with a short cooldown.
- Grace stacks Slowness; Bounty fires more arrows; Chaos raises a frost wall with its own longer cooldown.

### Bubbleveil

- Pain fires a line of axolotl shots with useful land range (`painShotSpeedLand` / `Water`).
- Grace and Bounty plant a bubble column on land or in water (axolotl visuals, Resistance / swarm damage).
- Chaos casts a forward wave on full draw. All of those have cooldowns.

### Buzzkill

- Pain bees home hard. Grace bees orbit the struck target (~2 blocks) and give nearby players Resistance I–V from Frame.
- Bounty plants a hive. Chaos is a ~13s honey storm that fades cleanly (no leftover smoke).
- Rune cooldowns scale with how strong the effect is.

### Petalwind

- Pain: small petal ring, reduced damage.
- Grace: buffs the nearest ally at the arrow, or the hit target; you only if closest.
- Bounty: visible ground trap that triggers when a hostile steps in.
- Chaos: koi spawn at the arrow; on a hit they orbit that target, not the shooter.

### Tremorstrike

- Pain waves no longer launch mobs into the sky every step.
- Chaos is a wider moving spike trail that steers between hostiles.
- Field lockout and Chaos cooldown stay longer than the light runes.

### Removed

- Echo and Starweave (Cosmic) are gone from items, loot, models, and config.
- Petalwind Chaos still uses the koi visuals.

## Bows (registry ids)

| In-game      | Item id      |
|--------------|--------------|
| Winterfang   | `ice_bow`    |
| Everbloom    | `vine_bow`   |
| Bubbleveil   | `bubble_bow` |
| Buzzkill     | `bee_bow`    |
| Petalwind    | `blossom_bow`|
| Tremorstrike | `earth_bow`  |

## Docs

Detailed notes live in [CHANGELOG.md](CHANGELOG.md).
