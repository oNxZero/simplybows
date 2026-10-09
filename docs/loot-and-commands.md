# Loot, items, and commands

[Documentation home](../README.md)

## Chest drops

Eligible loot tables have a chest context or a path beginning with `chests/`. Global pools apply to vanilla and modded namespaces. A successful bow pool chooses one of the six bows with equal entry weights; the rune pool chooses one of the four runes. These are separate rolls, so a chest may contain multiple kinds of Simply Bows items.

| Pool | Default setting | Chance per eligible table roll |
| :--- | :--- | :--- |
| Random bow | 50 | 5% |
| String | 20 | 2% |
| Frame | 20 | 2% |
| Random rune | 3 | 0.3% |
| Additional structure bow | 15 | 1.5% |
| Additional Ancient City rune | 20 | 2% |

**All chance settings divide by 1000.** `1` means 0.1%, `0.5` means 0.05%, and `1000` means 100%. The former raw-probability exception is removed. Convert a legacy raw chance by multiplying it by 1000: old `0.02` becomes `20` to retain 2%. Current defaults remain unchanged.

Structure pools are additional independent rolls, not replacements for the global roll. They apply only to matching `minecraft` tables:

| Bow or rune | Table paths under `minecraft:` |
| :--- | :--- |
| Bubbleveil | `chests/ocean_monument`, `chests/underwater_ruin_big`, `chests/underwater_ruin_small`, `chests/ocean_ruin_cold`, `chests/ocean_ruin_warm`, `chests/shipwreck_supply`, `chests/shipwreck_map`, `chests/shipwreck_treasure` |
| Everbloom | `chests/jungle_temple` |
| Buzzkill | Any `chests/village/` path |
| Tremorstrike | `chests/abandoned_mineshaft` |
| Petalwind | `chests/buried_treasure` |
| Winterfang | `chests/igloo_chest` |
| Random rune | `chests/ancient_city` |

Matching a table name does not create a chest where none exists. Already generated chest contents do not retroactively change when settings change.

## Player item IDs

| Display name | Registry ID |
| :--- | :--- |
| Everbloom | `simplybows:vine_bow/vine_bow` |
| Winterfang | `simplybows:ice_bow/ice_bow` |
| Bubbleveil | `simplybows:bubble_bow/bubble_bow` |
| Buzzkill | `simplybows:bee_bow/bee_bow` |
| Petalwind | `simplybows:blossom_bow/blossom_bow` |
| Tremorstrike | `simplybows:earth_bow/earth_bow` |
| Enchanted String | `simplybows:upgrades/enchanted_bow_string` |
| Reinforced Frame | `simplybows:upgrades/reinforced_bow_frame` |
| Pain Rune | `simplybows:upgrades/rune_etching_pain` |
| Grace Rune | `simplybows:upgrades/rune_etching_grace` |
| Bounty Rune | `simplybows:upgrades/rune_etching_bounty` |
| Chaos Rune | `simplybows:upgrades/rune_etching_chaos` |

Example:

```mcfunction
/give @s simplybows:ice_bow/ice_bow
/give @s simplybows:upgrades/enchanted_bow_string 30
/give @s simplybows:upgrades/rune_etching_grace
```

The registry also contains model-only items: `{bow}_bow_visual_pull_0`, `_1`, and `_2` for all six internal bow names; `{bow}_bow/{bow}_bow_inventory` for ice, bubble, bee, blossom, and earth. These load animation/inventory models and are not playable bow variants.

## Hostile test command

Requires permission level 2:

```mcfunction
/simplybows spawn_hostile
/simplybows spawn_hostile 5
/simplybows spawn_hostile 1 simplybows:ice_bow/ice_bow pain
```

Syntax: `/simplybows spawn_hostile [count] [bow] [rune]`. Count is 1–50. Rune choices are `none`, `pain`, `grace`, `bounty`, and `chaos`. A rune argument requires a bow argument first. The command accepts the shorter `simplybows:ice_bow` form as a convenience.

Mobs are selected from skeleton, stray, bogged, wither skeleton, and husk and placed within three blocks horizontally of the source. An unspecified bow is random; an unspecified rune has a 50% chance to be a random rune. Strings and Frames each receive a random level from 0–2. Test bows are unbreakable and do not drop; spawned mobs are persistent. Remove test mobs after testing.

Mob bow use is configurable: enabled by default, a roll every 60 ticks with 50% firing chance, 0.5 ability damage and 0.5 projectile damage modifiers, with an extra 0.5 modifier for damage to players. These settings permit bow use; they do not guarantee every mob's AI behaves like a skeleton.
