# Bows and runes

[Documentation home](../README.md) · [Upgrade rules](player-guide.md#strings-frames-and-runes) · [Every configuration value](configuration.md)

This catalog covers all six normal abilities and all 24 rune variants. Upgrade descriptions refer to the equipped rune, not a generic bonus. Values change with configuration: use the in-game upgrade preview for the next numeric bonus.

## Everbloom

<img src="../common/src/main/resources/assets/simplybows/textures/item/vine_bow/vine_bow.png" width="64" alt="Everbloom">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Plant a healing garden that restores allies and damages monsters. Your arrows can also heal allies directly. | Larger flower patch and faster arrows. | More healing and monster damage. |
| Pain | Plant a patch of cursed wither roses. Monsters inside take repeated magic damage. The patch does not heal allies. | Larger area. | More rose damage. |
| Grace | Grow a healing cherry tree. It heals every ally in the patch and removes harmful effects. | Larger area. | More healing. |
| Bounty | Grow three cherry trees that fire petal bolts at nearby monsters. Enemies in the area also receive Blindness and Nausea. | Larger area. | Stronger tree bolts. |
| Chaos | Plant a spore blossom that drains and roots monsters. Players inside move faster. | Larger spore area. | Longer spore field. |

## Winterfang

<img src="../common/src/main/resources/assets/simplybows/textures/item/ice_bow/ice_bow.png" width="64" alt="Winterfang">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Fire a spread of frost arrows that home in on enemies. | One more arrow per normal fan. | More arrow damage. |
| Pain | Fire one frost arrow that leaves a freezing area for 4 seconds. Enemies inside take damage and receive Slowness and Mining Fatigue. | Larger frost area. | More arrow damage. |
| Grace | Create a frost sanctuary. Allies inside gain Resistance, Speed, and Regeneration. Monsters are slowed. | Larger sanctuary. | More enemy arrow damage; sanctuary buffs unchanged. |
| Bounty | Fire one arrow that creates repeated frost bursts. Each burst damages and slows nearby enemies. | Larger frost area. | Another frost pulse. |
| Chaos | Raise a wall of ice where your arrow lands. It blocks movement and incoming projectiles. | Wider ice wall. | Longer ice wall. |

## Bubbleveil

<img src="../common/src/main/resources/assets/simplybows/textures/item/bubble_bow/bubble_bow.png" width="64" alt="Bubbleveil">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Fire arrows that travel freely through water and create bubble columns on impact. | Longer column duration. | Wider and taller column. |
| Pain | Fire a row of axolotl shots. They damage enemies on impact without creating bubble columns. | Another axolotl and +5% of base volley damage, capped at +25%. | More axolotl shot damage. |
| Grace | Create a protective bubble column with a guardian axolotl. Allies gain Resistance, enemies are slowed, and incoming shots are blocked. | Longer column duration. | Wider and taller column. |
| Bounty | Create a bubble column filled with axolotls. The swarm repeatedly damages enemies inside. | Longer column duration. | Wider/taller column and slightly more swarm damage. |
| Chaos | Send a water wave forward that damages enemies and knocks them back. | Longer wave travel. | More wave damage; tuned knockback. |

## Buzzkill

<img src="../common/src/main/resources/assets/simplybows/textures/item/bee_bow/bee_bow.png" width="64" alt="Buzzkill">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Fire bees at enemies. Fully drawn shots poison their target. | Stronger, longer poison. | More bee damage. |
| Pain | Fire a spread of homing bees that chase enemies and poison them. | More bees and longer poison. | More bee damage. |
| Grace | Send bees to protect allies with Resistance. Each bee moves between allies who still need protection. | Another protective bee. | Each bee can protect another ally. |
| Bounty | Create a hive that sends out exploding bees. Their explosions damage and poison nearby enemies. | More exploding bees. | More explosion damage and poison strength. |
| Chaos | Summon a honey storm. It slows enemies and attacks them with diving bees. | Larger honey storm. | More bee damage and faster dives. |

## Petalwind

<img src="../common/src/main/resources/assets/simplybows/textures/item/blossom_bow/blossom_bow.png" width="64" alt="Petalwind">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Create a petal storm that follows enemies and leaps between targets. | Longer storm duration. | More petal damage. |
| Pain | Create a ring of petals that repeatedly damages enemies inside. | Larger petal ring. | More petal damage. |
| Grace | Create a blossom puddle that grants Strength to every ally inside, increasing their melee damage. | Larger, longer support puddle. | Longer Strength buff. |
| Bounty | Create up to three petal storms, each following a different enemy. | Longer storm duration. | More petal damage with the Frame-dependent reduction. |
| Chaos | Summon koi that circle the impact area or struck target. They damage enemies and reflect incoming shots. | Larger koi area and shorter orbit time. | More koi, larger orbits, and longer duration. |

## Tremorstrike

<img src="../common/src/main/resources/assets/simplybows/textures/item/earth_bow/earth_bow.png" width="64" alt="Tremorstrike">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Erupt a field of dripstone spikes that damages enemies and knocks them upward. | Larger spike field. | More spike damage and knockback. |
| Pain | Send a fissure of spikes along the shot direction. It damages enemies, briefly slows them, and knocks them upward. | Wider spike field; fissure length stays fixed. | More spike damage and knockback. |
| Grace | Raise a protective dripstone wall that blocks movement and shots. Launch nearby allies safely with Resistance and Slow Falling. A directly struck ally also gains Absorption. | Larger protective wall. | Longer protective wall. |
| Bounty | Erupt three waves of spikes. Enemies near the center take stronger hits and knockback. | Larger spike field. | More spike damage, knockback, and visual spike height. |
| Chaos | Create a sweeping arc of spikes that repeatedly damages enemies as it circles the impact area. | Wider, longer sweeping spikes. | More spike damage and knockback; wider target acquisition. |

## Balance formulas

These are implementation formulas for this fork, not promises of damage through armor. Let `F` be Frame level and `S` String level. Configuration values and external attribute bonuses remain separate inputs.

| Effect | Current tuning |
| :--- | :--- |
| Everbloom Pain pulse | Configured hostile/undead damage × shared Frame damage multiplier × 0.15; external ability bonus × 0.5. Direct magic pulses avoid the old Wither overlap. |
| Everbloom Bounty tree bolt | `(1 + 0.425 × F) × 0.125`; undead bonus `(0.3 + 0.1 × F) × 0.125`; external ability bonus × 0.5. |
| Everbloom Bounty tree spacing | Ring radius adds `3 / sqrt(3)` blocks: triangle neighbors are 3 blocks farther apart. |
| Bubbleveil Bounty pulse | Configured base damage × `(1 + 0.06 × F) × 0.075`; external ability bonus × 0.3. |
| Petalwind normal storm | Normal storm contribution and external ability bonus are halved. |
| Petalwind Pain | Storm damage scale 0.35; external bonus scale 1. |
| Petalwind Bounty | Storm scale `0.7 × (1 − 0.06 × clamp(F, 0, 5))`; external bonus scale `1 − 0.06 × clamp(F, 0, 5)`. At Frame 5 the extra factor is 0.7. |
| Petalwind Grace | Storm damage contribution is zero; grants Strength support. |
| Bubbleveil Chaos range | `(baseSteps + S × stepsPerString) × stepDistance + 4` blocks; endpoint clips to the exact distance. |
| Bubbleveil Chaos knockback | Both horizontal and vertical force multiply by `1 − 0.04 × clamp(F, 0, 5)`: Frame 5 uses 0.8 of the previous force. |
| Winterfang arrow Frame multiplier | `1 + 0.18 × F`; Grace arrow damage additionally uses 0.35. |

Petalwind Chaos String changes area and orbit speed, rather than extending lifetime. Its Frame changes duration, fish count, and orbit size. Winterfang Pain currently produces a four-second zone with damage, Slowness, and Mining Fatigue; the older config comment describing Slowness-only behavior is stale.

## Choosing a build

For repeated area damage, compare Everbloom Pain/Bounty, Petalwind Pain/Bounty, and Bubbleveil Bounty with the expected number of targets and time in the field. For a single activation, include every pulse or swarm hit that actually lands. Winterfang offers homing and control; Tremorstrike trades sustained uptime for eruptions and displacement. Grace variants primarily provide team utility.

There is no verified universal DPS ordering in this repository. Combat stats are estimates and cooldowns are shared: test the configured build against your intended target, with armor and the same external attributes.

### Bubbleveil Pain buff

Pain now uses a total base damage coefficient of `0.35 × (1 + 0.05 × clamp(String, 0, 5))`, divided across the volley. This is a 25% base buff over the former 0.28 coefficient, with up to 25% additional String scaling. Frame retains its existing multiplier; cooldown, ammo costs, speeds, and critical behavior stay unchanged. No extra Slowness is added. Impact rounding, invulnerability frames, armor, and external attributes still affect realized damage: this coefficient is not guaranteed total health loss.
