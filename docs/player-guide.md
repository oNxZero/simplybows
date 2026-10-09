# Player guide

[Documentation home](../README.md) · [Every bow and rune](bows.md)

## Shooting and cooldowns

Fully draw the bow to use its special ability when it is ready. Default draw times appear in the README and are configurable. Short draws and shots during cooldown can fall back to ordinary shooting; individual bows have their own launch behavior.

The ability cooldown belongs to the **player**, shared across all six bows. Switching bows does not bypass it. The hotbar overlay shows the lockout. An ability's lifetime and its cooldown are different numbers: an effect can end before you can cast again.

For effects up to 5 seconds, the shared cooldown is three times the effect duration. Beyond 5 seconds, each extra second adds two seconds of cooldown. The general calculation is clamped between 2 and 30 seconds. Buzzkill multiplies the resulting cooldown by three, so its ceiling from this calculation is 90 seconds. Some abilities also have their own field or wall lockouts.

Winterfang normally fires `baseQuantity + String level` arrows. When a Pain, Grace, Bounty, or Chaos ability shot is ready, it fires one arrow. During cooldown it can return to the normal fan. Finite ammo is counted across separate inventory stacks. Creative mode and the supported Infinity path bypass the ordinary finite-ammo check; Infinity uses normal arrows, not a blanket exemption for every special ammunition type.

## Strings, Frames, and runes

Strings generally improve reach, duration, or the number of shots. Frames generally improve damage, protection, or the number of repeated attacks. Their effects depend on the bow **and its rune**: the upgrade page previews the next level for your actual bow.

By default, each type can reach level 5, but **String levels + Frame levels cannot exceed 5**. Examples: String 2 + Frame 3 is valid; String 5 + Frame 5 cannot be newly applied. A rune occupies a separate slot and does not consume those five slots.

| Component | Application | XP level cost |
| :--- | :--- | :--- |
| Enchanted String | Adds one String level | 2 + resulting String level |
| Reinforced Frame | Adds one Frame level | 2 + resulting Frame level |
| Any rune | Sets or replaces the equipped rune | 5 |
| Rename while upgrading | Changes or removes a custom name | Adds 1 |

First String/Frame costs 3 levels; fifth costs 7. Costs are XP **levels**, not individual XP points. Replacing a rune does not return the previous rune. Applying the same rune or exceeding a cap produces no upgrade output.

### Correct anvil recipe

1. Put the bow in the **left** slot.
2. Put a String, Frame, or rune in the **middle** slot.
3. Check the preview and XP cost. You can rename the bow at the same time.
4. Take the bow from the right slot: **one** component is consumed, even if the middle slot holds 30.

Reversing bow and component produces no output. Component + component produces no output. Upgrades cannot be applied through the former right-click/two-hand shortcut. Ordinary rename-only use still follows the vanilla anvil path.

## Reading tooltips

The first page shows the equipped upgrades and the current ability. With a rune equipped, the description explains that rune's ability on this bow. The Rune row itself only names the rune.

| Page | Content |
| :--- | :--- |
| Bow Description | Current ability and upgrade overview |
| Rune Description | Equipped rune's behavior, present only with a rune |
| Upgrade Description | String and Frame values, next-level bonuses, and resulting values |
| Combat Stats | Hit damage, repeated damage, timing, healing, or buffs relevant to the build |

Press the next-page binding while hovering (**G** by default). Rebinding changes both the control and footer label. `+` symbols use the bow color; the current page is white. Hold **Alt** for short numeric bonuses beside String/Frame. A rune item has an introduction and a separate page for each bow.

Numbers display at most one decimal: `6.785` becomes `6.8`. Calculations retain their precision. Bow attribute lore such as range, ranged damage, and pull time is hidden; this does not remove the underlying attributes.

## Understanding combat numbers

**2 damage = 1 heart. 20 ticks = 1 second.** A pulse interval is the gap between repeated hits; a duration is the lifetime of the effect. Healing restores health, while Absorption grants temporary extra hearts. Resistance reduces incoming damage; Strength improves melee damage.

Arrow hit values are estimates before armor, enchantments, and other mods. Impact speed and vanilla critical randomness can change the final hit. Ability damage is separate from the arrow hit and does not automatically receive arrow critical damage. Multiple enemies, time in the area, missed shots, and cooldowns all change real DPS: a single theoretical total is not a universal ranking.

Grace effects support eligible allies; targeting and friendly-fire rules differ between abilities. Everbloom's garden heals players and animals and damages monsters. Read the individual bow descriptions for direct-hit support and area effects.
