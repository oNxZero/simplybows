<div align="center">
<img src="common/src/main/resources/assets/simplybows/icon.png" width="112" alt="Simply Bows icon">

# Simply Bows Reforged

**Six bows. Four runes. A different way to fight with every build.**

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62b47a)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Version 0.1.4](https://img.shields.io/badge/version-0.1.4-blue)
![Loaders](https://img.shields.io/badge/loaders-NeoForge%20%7C%20Fabric-9b7fd4)

[Player guide](docs/player-guide.md) · [All bows and runes](docs/bows.md) · [Installation](docs/installation.md) · [Configuration](docs/configuration.md)
</div>

## Meet the bows

| Bow | Specialty | Default full draw |
| :--- | :--- | :--- |
| 🌿 **Everbloom** | Healing gardens, wither roses, cherry trees, and draining spores | 1.5 seconds |
| ❄️ **Winterfang** | Homing frost arrows, freezing zones, and ice walls | 3 seconds |
| 🌊 **Bubbleveil** | Underwater shots, protective columns, axolotl swarms, and water waves | 1 second |
| 🐝 **Buzzkill** | Poison bees, ally protection, exploding hives, and honey storms | 1 second |
| 🌸 **Petalwind** | Chasing petal storms, Strength support, and orbiting koi | 2 seconds |
| 🪨 **Tremorstrike** | Dripstone eruptions, protective walls, and sweeping spikes | 2 seconds |

Find bows and upgrades in loot chests. Combine **Strings** and **Frames** to shape your build, then choose **Pain**, **Grace**, **Bounty**, or **Chaos** to change its ability. Each rune behaves differently on each bow.

Abilities share a cooldown across your bows. Grace builds give allies a reason to fight together: healing, protection, movement, or Strength depending on the bow.

## Install and start playing

Use **Minecraft 1.21.1 and Java 21**. Put the matching Simply Bows jar on the **client and server**. Install Architectury API and Fzzy Config for that loader. Fabric also needs Fabric API. Use **Simply Tooltips 0.1.5** on the client; Fabric's manifest also requires it on the server.

Replace the previous Simply Bows jar instead of keeping two copies. See the [installation guide](docs/installation.md) for dependencies, updates, and troubleshooting.

To upgrade: **bow in the left anvil slot, upgrade in the middle, upgraded bow on the right**. One upgrade item is consumed. Strings and Frames share five slots by default; a rune uses no slot. Right-clicking an upgrade does not apply it.

Hover a bow for its overview. Press the configured next-page key (**G** by default) to cycle ability, rune, upgrades, and combat stats. Hold **Alt** for compact upgrade bonuses. Colored `+` symbols mark the pages; the current page is white.

## Documentation

| Guide | What it covers |
| :--- | :--- |
| [Player guide](docs/player-guide.md) | Drawing, cooldowns, ammo, upgrades, XP costs, tooltips, and damage units |
| [Bows and runes](docs/bows.md) | Every normal ability and all 24 rune variants, plus String and Frame effects |
| [Configuration reference](docs/configuration.md) | Every validated setting, exact default, allowed range, and source comment |
| [Loot, items, and commands](docs/loot-and-commands.md) | Chest chances, structure bonuses, registry IDs, and hostile test commands |
| [Development](docs/development.md) | Building both loaders, source layout, regression checks, and save data |
| [Testing and troubleshooting](docs/testing.md) | In-game checks, tooltip crash diagnosis, and known limits |
| [Changelog](CHANGELOG.md) | Current fixes and balance changes |

**Validation:** both loader jars build, standalone balance/navigation checks pass, and tooltip mixin targets are checked against Simply Tooltips 0.1.5. These checks do not replace in-game testing with your modpack.

## Credits and license

A fork of Simply Bows by **Sweenus and Hootea**, maintained in [oNxZero/simplybows](https://github.com/oNxZero/simplybows). Original art and code retain their attribution.

Distributed under the [Timefall Development License 1.2](LICENSE.txt). Read the license before redistributing or reusing assets and code.
