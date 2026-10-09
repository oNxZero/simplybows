# Installation and updates

[Documentation home](../README.md)

## Required environment

Minecraft **1.21.1**, Java **21**, and one matching mod loader. The project builds against NeoForge 21.1.166 or Fabric Loader 0.17.2. The reported client environment used NeoForge 21.1.255; this is not a claim that every modpack combination has been tested.

| Dependency | NeoForge client | NeoForge server | Fabric client | Fabric server |
| :--- | :---: | :---: | :---: | :---: |
| Simply Bows 0.1.4, matching loader | Required | Required | Required | Required |
| Architectury API 13.0.8 or compatible | Required | Required | Required | Required |
| Fzzy Config, matching loader | Required | Required | Required | Required |
| Fabric API for 1.21.1 | No | No | Required | Required |
| Simply Tooltips 0.1.5, matching loader | Required | Not required by manifest | Required | Required by manifest |

Fzzy Config is a runtime dependency used by the config code even though the mod manifests do not explicitly declare it. Build dependency: 0.7.6+1.21. Install its loader-specific dependencies too. Simply Tooltips is compiled separately and is not bundled. Its manifest minimum is 0.1.4, but the renderer hooks in this fork are specifically checked against **0.1.5**. Recheck hooks before changing that version.

## Which jar goes where?

| Loader | Production build output |
| :--- | :--- |
| NeoForge | `neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar` |
| Fabric | `fabric/build/libs/SimplyBows-fabric-0.1.4.jar` |

Copy the same loader's production jar to the client and server `mods` folders. Do not use source, development, shadow, or recovery jars as the installed mod. Do not install the Fabric and NeoForge versions together. Connector is not needed to load the native NeoForge jar.

## Updating an existing world

1. Stop the game and server. Back up the world, installed jars, and config.
2. Remove the previous Simply Bows jar and install the replacement on both sides.
3. Keep the matching dependencies. For the tooltip work here, keep Simply Tooltips 0.1.5.
4. Review `config/simplybows/config.toml`. Existing values can override new defaults: compare them with the [reference](configuration.md), rather than deleting your configuration blindly.
5. Convert legacy raw loot probabilities to the uniform per-thousand scale if needed: old `0.05` (5%) becomes `50`. Defaults such as `50`, `20`, and `3` need no conversion. Values up to `1` no longer have a special interpretation.
6. Restart and check tooltips, anvil output, and a test shot before returning to normal play.

The six current bow IDs remain unchanged even though their display names differ from their registry names. Rune item IDs still contain `rune_etching`; the visible name is now simply “Pain Rune”, “Grace Rune”, etc. Removed Echo/Starweave content is not restored by this fork.

Previously saved projectiles without the new state cannot recover upgrades that were never stored. Newly saved projectiles preserve their ability settings. See [save data](development.md#save-data).
