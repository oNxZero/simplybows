# Simply Bows Reforged

A balance and feel fork of [Sweenus/simplybows](https://github.com/Sweenus/simplybows) for NeoForge 1.21.1.

Item IDs for the six remaining bows are unchanged, so existing Winterfang, Everbloom, Bubbleveil, Buzzkill, Petalwind, and Tremorstrike items stay valid. Echo and Starweave are removed.

Upstream author: [Sweenus](https://github.com/Sweenus). This fork keeps the Timefall Development License 1.2 from upstream.

## Install

Build (Gradle **8.9**, Java **21**):

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
gradle :neoforge:remapJar
```

Install `neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar` on the **server and every client**. Do not run it beside the Modrinth upstream jar.

After a first update (or if the server fails to start with `ExceptionInInitializerError`), delete `config/simplybows/config.toml` once so renamed keys are written. A live file can keep old numbers or break on invalid ranges.

Requires [Simply Tooltips](https://modrinth.com/mod/simply-tooltips) for the modern bow tooltips (rune prose under Upgrades; Alt tabs for String/Frame detail).

## What Reforged changes

### Shared rules

- Unique ability shots need a full draw where that bow has a special.
- **One shared ability cooldown per player** across every Simply Bow. Swap-spam with five bows does nothing — bring a party with different roles.
- Cooldown length is based on effect duration: **3×** up to 5s of effect, then **2×** on the remainder, hard-capped at **~30s**. Burst abilities use a short default window.
- The hotbar shows a dark see-through cooldown wash on every unique bow while locked; normal shots still work.
- Hold **Alt** on a unique bow for String/Frame gains and rune details (Simply Tooltips).
- Upgrades live in the `simplybows:upgrades` component so they survive mods that wipe `minecraft:custom_data` on death. Already-wiped upgrades cannot be restored.
- Effect zones grow in and sink out instead of popping.

### Everbloom

Healing bow. Full draw plants flowers; short draw still heals on hit.

- Players, animals, and iron golems take no arrow damage and are healed on direct hit.
- Field heal on the visible flower patch. Pain: damage only, faster pulses. Grace: heal everyone + cleanse via cherry tree. **Bounty: no heal — DPS aura** (stronger pulses than base, a bit slower than Pain). Chaos: spore blossom drain field.
- One Everbloom field per player at a time. Shared global cooldown with all other unique bows.

### Winterfang

- Pain focuses one target at `1.2x` damage.
- Grace stacks Slowness; Bounty fires more arrows at reduced damage each; Chaos raises a frost wall.

### Bubbleveil

- Pain fires a line of axolotl shots (`painShotSpeedLand` / `Water`).
- Grace and Bounty plant a bubble column (Resistance / swarm damage).
- Chaos casts a forward wave on full draw.

### Buzzkill

- Pain: fan of weakly homing bees. Grace: orbiting shield bees + Resistance for nearby allies.
- Bounty: short-lived hive that fires homing bees. Chaos: honey storm that slows and dive-bombs (no leftover smoke).

### Petalwind

- Base / Bounty: leaping petal storms. **Bounty locks onto up to 3 targets** (retarget only if one dies); config max is 3.
- Pain: planted petal ring at half storm damage (AOE).
- Grace: short filled blossom puddle — Strength II for players inside.
- Chaos: orbiting koi at impact (~8s base, shorter Frame scaling than before).

### Tremorstrike

- Pain: outward spike waves (moderate damage). Grace: ally launch + Resistance / Slow Falling.
- Bounty: center spike burst with controlled knock-up.
- Chaos: **dense spike disc** that orbits the impact for **exactly two full turns** (10s+, longer with String). Softened per-hit damage vs. the wider coverage.

### Removed

- Echo and Starweave (Cosmic) are gone from items, loot, models, and config.
- Petalwind Chaos still uses the koi visuals.

## Bows (registry ids)

| In-game      | Item id       |
|--------------|---------------|
| Winterfang   | `ice_bow`     |
| Everbloom    | `vine_bow`    |
| Bubbleveil   | `bubble_bow`  |
| Buzzkill     | `bee_bow`     |
| Petalwind    | `blossom_bow` |
| Tremorstrike | `earth_bow`   |

## Docs

Detailed notes live in [CHANGELOG.md](CHANGELOG.md).
