# Simply Bows Reforged

A balance and feel fork of [Sweenus/simplybows](https://github.com/Sweenus/simplybows) for **NeoForge 1.21.1**.

Item IDs for the six remaining bows are unchanged, so existing Winterfang, Everbloom, Bubbleveil, Buzzkill, Petalwind, and Tremorstrike items stay valid. Echo and Starweave are removed.

Upstream author: [Sweenus](https://github.com/Sweenus). This fork keeps the Timefall Development License 1.2 from upstream.

---

## Install

Build (Gradle **8.9**, Java **21**):

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk   # or your JDK 21 path
./gradlew :neoforge:remapJar
```

Install `neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar` on the **server and every client**. Do not run it beside the Modrinth upstream jar.

After a first update (or if the server fails to start with `ExceptionInInitializerError`), delete `config/simplybows/config.toml` once so renamed keys are rewritten. A live file can keep old numbers or break on invalid ranges.

Requires [Simply Tooltips](https://modrinth.com/mod/simply-tooltips) for modern bow tooltips (rune prose under Upgrades; Alt tabs for String/Frame detail).

---

## Shared rules

- Unique ability shots need a **full draw** where that bow has a special.
- **One shared ability cooldown per player** across every Simply Bow. Swapping bows does not reset it.
- Cooldown length is based on effect duration (roughly **3×** up to 5s of effect, then **2×** on the remainder, hard-capped around **~30s**). Burst abilities use a short default window.
- The hotbar shows a dark cooldown wash on every unique bow while locked; normal shots still work.
- Hold **Alt** on a unique bow for String/Frame gains and rune details (Simply Tooltips). String/Frame alt text updates based on the etched rune.
- Upgrades live in the `simplybows:upgrades` component so they survive mods that wipe `minecraft:custom_data` on death.
- Support runes (Grace) never hurt players, animals, iron golems, or villagers.

### Upgrades

| Upgrade | Role |
|---------|------|
| **Enchanted String** | Size, count, duration, or radius — depends on the bow and rune |
| **Reinforced Frame** | Power, duration, pulses, hops, or poison level — depends on the bow and rune |
| **Rune Etching** | Pain / Grace / Bounty / Chaos — replaces the unique ability (does not use a String/Frame slot) |

---

## The bows

### Everbloom (`vine_bow`)

Nature support field. Impact plants a flower patch that heals allies and chips monsters. Fully drawn shots also heal the ally they hit.

| Rune | Effect | String | Frame |
|------|--------|--------|-------|
| *(none)* | Flower field — heal allies, light damage to hostiles | Radius + arrow speed | Heal / field damage |
| **Pain** | Wither-rose field (dark smoke/ink). No heal. Faster monster damage | Radius | Damage |
| **Grace** | Heals every ally in the patch; cherry tree cleanses negatives | Radius | Heal strength |
| **Bounty** | Three cherry trees volley petal bolts at hostiles; Blindness + Nausea in the area | Radius | Bolt damage |
| **Chaos** | Spore blossom with draining tendrils; burst buffs allies when charged | Radius | Duration |

One Everbloom field per player at a time.

---

### Winterfang (`ice_bow`)

Fan of homing frost arrows. Vanilla crits are disabled so multi-arrow shots do not spike unfairly.

| Rune | Effect | String | Frame |
|------|--------|--------|-------|
| *(none)* | Homing frost fan | +1 arrow | Arrow damage |
| **Pain** | One focused arrow. 4s frost zone — Slowness II, Mining Fatigue, light chip damage | Zone size | Damage |
| **Grace** | Soft tip. Sanctuary buffs players, animals, and golems (Res + Speed + Regen); hostiles get Slow | Sanctuary size | — |
| **Bounty** | One arrow. Frost bloom pulses once per second | Pulse radius | Pulse count |
| **Chaos** | Frost wall that blocks entities and projectiles | Wall width | Duration |

---

### Bubbleveil (`bubble_bow`)

Arrows ignore water drag and leave short bubble columns.

| Rune | Effect | String | Frame |
|------|--------|--------|-------|
| *(none)* | Bubble column on impact | Duration | Radius / height |
| **Pain** | Full draw fires a line of axolotl shots (no columns) | Axolotl count | — |
| **Grace** | Guardian column — blocks shots, Resistance II for allies, Slowness for hostiles | Duration | Size |
| **Bounty** | Axolotl swarm column that chips hostiles inside (low sustained DoT) | Duration | Soft damage scale |
| **Chaos** | Full-draw forward water wave with heavy knockback | Wave length | Wave damage |

---

### Buzzkill (`bee_bow`)

Fires bees. Fully drawn shots apply stacking poison.

| Rune | Effect | String | Frame |
|------|--------|--------|-------|
| *(none)* | Bee arrows; full draw stacks poison | Poison duration / strength | Bee damage |
| **Pain** | Fan of short-lived homing bees at reduced damage | Bee count | Damage |
| **Grace** | Bees sting allies for Resistance II, then hop to the next ally within 10 blocks. Skips targets that already have Res; wanders until a new ally appears | Bee count | Hops per bee |
| **Bounty** | Hive releases exploding splash bees that stack poison. Short acquire range; bees launch out and search / wander if nothing is nearby | Bee count | Splash damage + poison level (up to III) |
| **Chaos** | ~5s honey storm — slowing cloud and dive-bombs | Storm radius | Dive damage |

---

### Petalwind (`blossom_bow`)

Arrows create small blossom storms that leap between targets.

| Rune | Effect | String | Frame |
|------|--------|--------|-------|
| *(none)* | Leaping petal storm | Duration | Storm damage |
| **Pain** | Damaging petal ring at impact (softer than a leaping storm) | Ring radius | Damage |
| **Grace** | Short blossom puddle; **all** allies inside (players, animals, golems) get Strength II | Puddle size | Buff duration |
| **Bounty** | Up to **3** petal storms that lock onto separate targets | Duration | Damage |
| **Chaos** | Orbiting koi that damage hostiles and reflect shots | — | Koi count / duration |

---

### Tremorstrike (`earth_bow`)

Impact erupts a dripstone spike field with knock-up.

| Rune | Effect | String | Frame |
|------|--------|--------|-------|
| *(none)* | Spike field on impact | Radius | Damage + knock-up |
| **Pain** | Seismic fissure along the shot path — spikes, damage, brief Slowness II | Wave travel | Damage |
| **Grace** | Launches nearby allies with Resistance + Slow Falling. Raises a **half-oval dripstone wall** that blocks walking and shots (frost-wall style). Hit ally gets **Absorption III**. String = radius, Frame = duration | Wall radius | Wall duration |
| **Bounty** | Three spike pulses (up / down / up) half a second apart | Radius | Center spike power |
| **Chaos** | Smaller orbit; a filled **25% wedge** of spikes sweeps around | Duration | Damage |

---

## Registry ids

| In-game      | Item id       |
|--------------|---------------|
| Winterfang   | `ice_bow`     |
| Everbloom    | `vine_bow`    |
| Bubbleveil   | `bubble_bow`  |
| Buzzkill     | `bee_bow`     |
| Petalwind    | `blossom_bow` |
| Tremorstrike | `earth_bow`   |

---

## Removed

- Echo and Starweave (Cosmic) are gone from items, loot, models, and config.

## Docs

Detailed patch notes live in [CHANGELOG.md](CHANGELOG.md).
