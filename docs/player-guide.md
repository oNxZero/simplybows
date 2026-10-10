# Player guide

[Documentation home](../README.md) · [Every bow and rune](bows.md)

## Shooting and cooldowns

Fully draw the bow to use its special ability when it is ready. Default draw times appear in the README and are configurable. Short draws and shots during cooldown can fall back to ordinary shooting; individual bows have their own launch behavior.

The ability cooldown belongs to the **player**, shared across all six bows. Switching bows does not bypass it. The hotbar overlay shows the lockout. An ability's lifetime and its cooldown are different numbers: an effect can end before you can cast again.

For effects up to 5 seconds, the shared cooldown is three times the effect duration. Beyond 5 seconds, each extra second adds two seconds of cooldown. The general calculation is clamped between 2 and 30 seconds. Buzzkill multiplies the resulting cooldown by three, so its ceiling from this calculation is 90 seconds. Some abilities also have their own field or wall lockouts.

Winterfang normally fires `baseQuantity + String level` arrows. Pain fires one arrow plus one per String, seeking separate targets when ready. Grace, Bounty, and ready Chaos shots use one arrow. Grace and Bounty remain straight single shots during cooldown; Grace remains harmless, while Bounty does not create a prison. Pain can return to the normal fan during cooldown. Finite ammo is counted across separate inventory stacks. Creative mode and the supported Infinity path bypass the ordinary finite-ammo check; Infinity uses normal arrows, not a blanket exemption for every special ammunition type.

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

### Bow enchantments

Power increases direct projectile hit damage, Punch adds hit knockback, and Flame ignites damaging shots and their victims. Ability damage (storms, swarms, frost splashes, and prison pulses) keeps its own scaling. Winterfang Grace remains harmless, including fire and tipped/spectral effects. Support shots do not ignite protected allies. Winterfang Bounty still holds its prisoner in place despite Punch. Infinity, Unbreaking, and Mending retain their existing handling. Enchantments are saved with newly fired custom arrows, including across chunk unloading; old arrows fired before this fix have no recoverable bow snapshot.

Equipped enchantments appear on the first bow tooltip page as named level bars beside String and Frame. Filled squares show the current level and empty squares show remaining levels. Curses appear red; other enchantments use the bow color. Hold Alt to see the numeric enchantment level. Enchantments do not use String/Frame upgrade slots.

Infinity-created normal arrows cannot be recovered in Survival, including extra arrows in Winterfang and other volleys. Creative-fired arrows are Creative-only pickups. Consumed tipped/spectral ammunition remains recoverable when the projectile survives, matching vanilla Infinity restrictions.

## Reworked offensive runes

Tremorstrike Chaos crushes enemies between rising stone jaws. Bounty grows and shrinks a filled star three times. Grace's wall now blocks you and your allies too: move around its open side.

Buzzkill Pain sends one homing bee that attaches a damaging swarm. Bubbleveil Pain creates a particle raincloud at impact, including ground hits; it drops three water crystals per volley, with splash damage and Slowness I for 2s. Petalwind Bounty opens a gathering blossom, draws enemies together, then releases a focused burst. Their String upgrades improve duration or reach, and Frame increases ability damage. The upgrade and combat tooltip pages show the equipped values.

## Effect sounds

Bow effects use positional Minecraft sounds, so nearby players can hear an ability form, strike, or fade away. No extra sound pack is required. Adjust **Players** in Minecraft's sound settings to change their volume.

| Bow | Sound character |
|:---|:---|
| Everbloom | Grass, moss and leaves for flowers and vines; darker Wither sounds for Pain; leaf rustling for Bounty trees; crystal and moss sounds for Chaos. |
| Winterfang | Frost cracks and shattering ice for attacks, walls and prisons; soft crystal chimes for Grace's sanctuary. Bounty's damage pulses have quiet ice taps. |
| Tremorstrike | Dripstone and stone impacts for fissures and walls. Chaos has a grinding windup and a heavier crushing impact. Bounty's three stars sound as they rise and retract. |
| Buzzkill | Bee buzzing, stings, hive sounds and honey drips. Pain's attached swarm buzzes quietly and sounds its stings; Grace uses softer pollination cues. |
| Bubbleveil | Bubbles, moving water and splashes. Pain's raincloud sounds moving water while active and splashes at its final downpour; Grace's shield pops bubbles when blocking projectiles. |
| Petalwind | Cherry leaves, blossom sounds and gentle support chimes. Bounty's lotus rustles as it opens, sounds its strike, and rustles as it closes. Chaos retains its koi movement and impact sounds. |

New repeating cues are quieter than formation and impact sounds. Nearby overlapping effects share a short sound limit, reducing repeated stings, water pops and impacts during crowded fights. Ambient cues are short sounds rather than permanently running audio loops, so unloading or ending an effect cannot leave a new loop playing.

Every bow/rune build has an ability sound palette: Everbloom uses plants, Wither and spores; Winterfang uses frost/crystal cues; Tremorstrike uses stone and dripstone; Buzzkill uses buzzing/stings/honey; Bubbleveil uses water/bubbles; Petalwind uses blossoms/leaves and sweep sounds. These are existing and new finite positional sounds, rather than one added generic sound for every effect.

Tremorstrike Bounty sounds five successive growing layers and five returning layers in each of its three cycles. Chaos sounds each pair forming and each pair breaking; its limiter permits the eight-tick sequence. Bubbleveil Pain has an overlapping translucent 3D cloud body, particle edges, formation/fade animations and homing water drops. Petalwind Bounty's three glass petals leave the visible crown at 1.2s, 1.8s and 2.4s, travel outward briefly, then seek their selected enemies.
