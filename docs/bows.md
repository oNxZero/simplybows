# Bows and runes

[Documentation home](../README.md) · [Upgrade rules](player-guide.md#strings-frames-and-runes) · [Every configuration value](configuration.md)

This catalog covers all six normal abilities and all 24 rune variants. Upgrade descriptions refer to the equipped rune, not a generic bonus. Values change with configuration: use the in-game upgrade preview for the next numeric bonus.

## Everbloom

<img src="../common/src/main/resources/assets/simplybows/textures/item/vine_bow/vine_bow.png" width="64" alt="Everbloom">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Plant a healing garden that restores allies and damages monsters. Your arrows can also heal allies directly. | Larger flower patch and faster arrows. | More healing and monster damage. |
| Pain | Plant a cursed patch mixing wither roses, alliums, cornflowers and red tulips. Monsters inside take repeated magic damage. The patch does not heal allies. | Larger area. | More rose damage. |
| Grace | Grow a healing cherry tree. It heals every ally in the patch and removes harmful effects. | Larger area. | More healing. |
| Bounty | Grow three cherry trees that fire petal bolts at nearby monsters. Enemies in the area also receive Blindness and Nausea. | Larger area. | Stronger tree bolts. |
| Chaos | Plant a spore blossom that drains and roots monsters. Players inside move faster. | Larger spore area. | Longer spore field. |

## Winterfang

<img src="../common/src/main/resources/assets/simplybows/textures/item/ice_bow/ice_bow.png" width="64" alt="Winterfang">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Fire a spread of frost arrows that home in on enemies. | One more arrow per normal fan. | More arrow damage. |
| Pain | Fire 1 + String homing arrows, each assigned a different visible enemy. Hits apply Slowness III for 5 seconds and a small frost splash. | +1 arrow per level; extra arrows do not reuse targets. | +18% base arrow damage per level; splash scales with arrow damage. |
| Grace | Harmless arrow homes toward a nearby player, or flies straight when none is available, and creates a swirling frost sanctuary with Resistance I, Speed I, and Regeneration I. No damage or enemy debuffs. | +0.35 blocks sanctuary radius per level. | +1 second buff duration per level after each refresh. |
| Bounty | Straight arrow freezes the struck enemy in a spiky ice prison for 3 seconds before upgrades. Blocks movement, attacks, and item use. | +0.5 seconds of freeze per level. | +0.5 damage per prison pulse (every second). |
| Chaos | Raise a wall of ice where your arrow lands. It blocks movement and incoming projectiles. | Wider ice wall. | Longer ice wall. |

## Bubbleveil

<img src="../common/src/main/resources/assets/simplybows/textures/item/bubble_bow/bubble_bow.png" width="64" alt="Bubbleveil">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Fire arrows that travel freely through water and create bubble columns on impact. | Longer column duration. | Wider and taller column. |
| Pain | An impact creates a stationary 3D raincloud with particle edges. Three crystals per volley fall straight down and splash on the ground, followed by a stronger final volley. Ground hits work too. | +0.4s duration and +0.5 radius. | +0.4 pulse damage and +1 final burst damage. |
| Grace | Create a shimmering floor water circle. All players inside, plus friendly creatures, gain Resistance II and Regeneration I. Incoming hostile shots are blocked. | Longer column duration. | Wider and taller column. |
| Bounty | Create a bubble column filled with axolotls. The swarm repeatedly damages enemies inside. | Longer column duration. | Wider/taller column and slightly more swarm damage. |
| Chaos | Send a water wave forward that damages enemies and knocks them back. | Longer wave travel. | More wave damage; tuned knockback. |

## Buzzkill

<img src="../common/src/main/resources/assets/simplybows/textures/item/bee_bow/bee_bow.png" width="64" alt="Buzzkill">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Fire bees at enemies. Fully drawn shots poison their target. | Stronger, longer poison. | More bee damage. |
| Pain | One homing bee attaches a swarm that circles and stings the struck enemy. | +0.6s duration and +1 sting. | +0.5 damage per sting. |
| Grace | Send bees to protect allies with Resistance. Each bee moves between allies who still need protection. | Another protective bee. | Each bee can protect another ally. |
| Bounty | Create a hive that sends out exploding bees. Their explosions damage and poison nearby enemies. | More exploding bees. | More explosion damage and poison strength. |
| Chaos | Summon a honey storm. It slows enemies and attacks them with diving bees. | Larger honey storm. | More bee damage and faster dives. |

## Petalwind

<img src="../common/src/main/resources/assets/simplybows/textures/item/blossom_bow/blossom_bow.png" width="64" alt="Petalwind">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Create a petal storm that follows enemies and leaps between targets. | Longer storm duration. | +10% pulse damage per Frame. |
| Pain | Create a ring of petals that repeatedly damages enemies inside. | Larger petal ring. | +10% pulse damage per Frame. |
| Grace | Create a swirling blossom circle that grants Strength to all players and support creatures inside, increasing their melee damage. No shared team is required. | Larger, longer support circle. | Longer Strength buff. |
| Bounty | An impact blossom gathers enemies with a short pull, then releases a focused burst at up to three enemies. | +0.5 block acquisition radius. | +3 burst damage per target. |
| Chaos | Summon koi that circle the impact area or struck target. They damage enemies and reflect incoming shots. | Larger koi area and shorter orbit time. | More koi, larger orbits, and longer duration. |

## Tremorstrike

<img src="../common/src/main/resources/assets/simplybows/textures/item/earth_bow/earth_bow.png" width="64" alt="Tremorstrike">

| Build | Ability | String | Frame |
| :--- | :--- | :--- | :--- |
| Normal | Erupt a field of dripstone spikes that damages enemies and knocks them upward. | Larger spike field. | More spike damage and knockback. |
| Pain | Send a fissure of spikes along the shot direction. It damages enemies, briefly slows them, and knocks them upward. | Wider spike field; fissure length stays fixed. | More spike damage and knockback. |
| Grace | Raise a solid dripstone wall that blocks everyone, including the caster and allies, and destroys crossing projectiles. Nearby players and support creatures receive support buffs without requiring a shared team. | Larger protective wall. | Longer protective wall. |
| Bounty | A filled eight-point star expands outward and contracts inward three times along the ground. Each enemy can be hit once per cycle. | +0.5 block radius. | +1 damage per wave. |
| Chaos | Five jagged stone pairs slam inward in sequence, advancing in the firing direction. Escape each pair before it closes. | +0.5 block radius. | +2 crush damage. |

## Balance formulas

These are implementation formulas for this fork, not promises of damage through armor. Let `F` be Frame level and `S` String level. Configuration values and external attribute bonuses remain separate inputs.

| Effect | Current tuning |
| :--- | :--- |
| Everbloom Pain pulse | Configured hostile/undead damage × shared Frame damage multiplier × 0.15; external ability bonus × 0.5. Direct magic pulses avoid the old Wither overlap. |
| Everbloom Bounty tree bolt | `(1 + 0.425 × F) × 0.125`; undead bonus `(0.3 + 0.1 × F) × 0.125`; external ability bonus × 0.5. |
| Everbloom Bounty tree spacing | Ring radius adds `3 / sqrt(3)` blocks: triangle neighbors are 3 blocks farther apart. |
| Bubbleveil Bounty pulse | Configured base damage × `(1 + 0.06 × F) × 0.075`; external ability bonus × 0.3. |
| Petalwind normal storm | Normal storm contribution and external ability bonus are halved. |
| Petalwind Pain | Storm damage scale 0.14 (40% of the previous base); external bonus scale 0.4; Frame multiplier `1 + 0.1 × F`. |
| Petalwind Bounty | One homing blade per selected target: `12 + 3 × F`, plus external ability bonus. Up to three different targets; no overlapping damage fields. |
| Petalwind Grace | Storm damage contribution is zero; grants Strength support. |
| Bubbleveil Chaos range | `(baseSteps + S × stepsPerString) × stepDistance + 4` blocks; endpoint clips to the exact distance. |
| Bubbleveil Chaos knockback | Both horizontal and vertical force multiply by `1 − 0.04 × clamp(F, 0, 5)`: Frame 5 uses 0.8 of the previous force. |
| Winterfang arrow Frame multiplier | `1 + 0.18 × F`; Bounty uses a fixed `3.0` multiplier, Grace deals zero direct damage. |

Petalwind Chaos String changes area and orbit speed, rather than extending lifetime. Its Frame changes duration, fish count, and orbit size. Winterfang Pain now uses a one-time splash, not a lasting damage zone. Bounty traps the struck target rather than producing repeated frost bursts.

## Choosing a build

For repeated area damage, compare Everbloom Pain/Bounty, Petalwind Pain/Bounty, and Bubbleveil Bounty with the expected number of targets and time in the field. For a single activation, include every pulse or swarm hit that actually lands. Winterfang offers homing and control; Tremorstrike trades sustained uptime for eruptions and displacement. Grace variants primarily provide team utility.

There is no verified universal DPS ordering in this repository. Combat stats are estimates and cooldowns are shared: test the configured build against your intended target, with armor and the same external attributes.

### New rune formations

`S` and `F` below are the equipped String and Frame levels, capped at 5 individually; the existing shared upgrade-slot limit still applies. Damage is in health points, before armor and external ability bonuses. Power, Punch and Flame apply to the delivery projectile, never to formation damage.

| Build | Ability damage and timing | Shared cooldown |
|:---|:---|:---|
| Tremorstrike Chaos | `8 + 2F` at most once per enemy; radius `3 + 0.5S`. Five pairs close at 1.4s, 1.8s, 2.2s, 2.6s and 3.0s after impact. Damage follows each closing strip, with no inward pull. Successfully crushed targets are rooted for 5s: movement is blocked, but attacks and items remain available. Roots clear on death, unloading, restart or a long teleport. The formation lasts 4s before its final fade. | 7.5s from launch |
| Tremorstrike Bounty | Three 1.5s cycles. The filled star expands for 0.8s and contracts for 0.8s; each cycle deals `3 + F` once per enemy inside. Radius `4 + 0.5S`. Returning spikes do not add a second hit. | 13.5s from impact |
| Buzzkill Pain | One homing delivery bee; successful hit attaches seven visual bees. Stings every 0.6s for `3 + 0.6S` seconds, each dealing `2.5 + 0.5F`. Five to ten stings. Does not add the old volley poison. | 18s from launch |
| Bubbleveil Pain | One water-core delivery shot creates a stationary 3D cloud with particle edges even on block impact. Radius `5 + 0.5S`. Each volley drops three crystals above enemies in the area, or over the field when empty. Their landing positions are fixed when released; moving enemies can dodge them. Crystals split the existing volley damage into thirds, so overlapping splashes cannot triple the per-volley budget. Splash radius is 2 blocks; successful hits give Slowness I for 2s. Area raincloud lasts `4 + 0.4S` seconds. Every second: `4 + 0.4F` volley damage. At expiry: `6 + F` final burst. Splash damage bypasses armor, but still respects Resistance and other protections. Four to six pulses. | `12 + 1.2S` seconds from launch |
| Petalwind Bounty | One anchored blossom seeks different enemies within `4 + 0.5S` blocks. From 0.8s to 2s it pulls nearby enemies toward the blossom. At 2s it releases up to three blades at distinct targets, for `12 + 3F` damage on arrival. Three targets maximum, then the crown fades. | 7.5s from impact |

These effects use a persistent formation entity plus short-lived seeking water or petal entities, with client-rendered geometry. The swarm begins after a successful direct hit. The area raincloud and impact blossom work on ground impacts. Water and petal projectiles track a selected enemy for at most 1.5s, vanish on terrain collision or a teleport over 4 blocks between ticks, and cannot pass through solid cover. Long teleports over 16 blocks break attached formations. Dead or newly protected targets stop receiving damage immediately. All formations grow over 0.3s and have a separate 0.6s finish animation after their last damage event or an early cancellation. This cosmetic finish cannot add hits and follows a nearby surviving target. Swarm bees fly outward; the particle cloud dissipates while ripples fade and the final volley descends. Unloaded targets have up to a 1s grace period before finishing; active time still advances. Owner absence starts the finish. Duration, upgrade levels, owner/target UUIDs and star-wave hit history save with the entity, so reload does not replay completed pulses.

Tremorstrike Grace remains a curved wall with an open side, rather than an enclosed cage. Its solid segments block the caster, allies, enemies and projectiles in either direction. Crossing checks consider the movement between ticks. It never places or deletes terrain blocks.

### Winterfang control rules

Pain assigns visible hostile targets in front of the shooter within the configured homing radius. Players, monsters, and bosses are eligible subject to friendly-fire rules. Each target is reserved once across the volley; unassigned extra arrows fly without acquiring an already reserved target. Each enemy can receive at most one splash from that volley, excluding the enemy directly struck by that particular arrow. Default splash radius is 1.75 blocks, clamped to 0.5–3 blocks; old configs may retain larger values. Frost means Slowness III for 5 seconds. Status-immune bosses receive a server-side ordinary-movement penalty instead; long teleports are retained.

Bounty's non-homing arrow traps one struck enemy for `60 + 10 × String` ticks (3 to 5.5 seconds), with a translucent normal-ice crystal cluster, uneven shoulders, and long outward-projecting side spikes scaled to the target. It changes no terrain. The frozen target cannot move, attack, mine, interact, or use items. Server-enforced ticking and action restrictions also apply to bosses and players; incoming damage immunity timers continue counting down. Re-hitting an already frozen target does not extend its current prison. Creative/spectator and friendly-fire protections still apply.

Grace homes toward nearby friendly players, otherwise flies straight, and does zero damage, including spectral/tipped arrow effects and external projectile damage bonuses. Partial draws do not fire or consume ammunition. The sanctuary lasts 7 seconds, has radius `3.25 + 0.35 × String`, and refreshes buffs for `5 + Frame` seconds. It has no enemy damage or debuffs. All nearby players, animals, villagers and golems receive buffs regardless of PvP/team settings. During cooldown a fully drawn shot is still harmless, but creates no additional sanctuary.

Pain ability cooldown is 12 seconds under the current launch rule; Bounty uses a fixed 18-second shared cooldown; Grace uses twice the shared calculation for its 7-second sanctuary (38 seconds). During cooldown Pain uses the normal frost-arrow fan without the Frost splash. Bounty remains a single straight damage arrow without a prison. Frost I applies Slowness I for 10 seconds from impact; frozen time counts toward those 10 seconds. The prison deals `1 + 0.5 × Frame` damage every 20 ticks while active, starting one second after impact. Armor and external ability bonuses affect actual health loss. The ice grows over 0.3 seconds and shrinks/shatters over 0.4 seconds after release. All Bounty shots have three times the normal base arrow damage (double the previous Bounty shot), with no Frame damage scaling. Neither the prison nor the status-immune boss movement fallback is persisted through server restart.

Chaos reserves its shared cooldown once after launch; the first delivery arrow carries the cast token. Impact consumes that token without checking the same already-started cooldown again. This fixes the player cast being rejected. Tremorstrike Pain now renders 100 fissure spikes instead of 50, with staggered infill; its length, width and damage calculation stay the same.

Tremorstrike Pain gives each eligible enemy one fissure hit even during ordinary damage immunity, then pushes them away along the firing direction with horizontal impulse 0.8 and upward impulse 0.65. Overlapping fissure samples cannot hit or launch the same enemy repeatedly. Its half-width is at least 3.6 blocks and follows larger configured/upgraded field radii. Bounty keeps its three outward/inward cycles and one hit per enemy per cycle; its filled star now uses broad stepped stone shoulders, tapered caps and outward-leaning teeth.

Tremorstrike Bounty's spike heights are halved, with taller central cells tapering toward the edge. Successful hits set vertical velocity to 0.55 and send player velocity updates, giving approximately a two-block launch under normal gravity. Ceilings and other movement effects can change the actual height. Chaos pairs rise out of the ground before closing and shattering.

Bubbleveil Pain's cloud is two blocks higher (around 6.5 blocks above the impact) and thicker vertically. The three falling water crystals use terrain collision to create a splash; they do not directly damage a homing target in midair. Petalwind Bounty's gathering pull distinguishes it from the default sustained storm: it helps concentrate nearby enemies for a short burst.

Everbloom Bounty tree bolts now travel toward their selected enemy and apply damage only on arrival. Terrain and solid rune walls stop them; missing, dead or teleporting targets end the bolt.

Winterfang Bounty pulses and sways subtly while occupied. Release sends eight outward-pointing ice shards; each shard stops on its first enemy and applies Frost I (Slowness I) for five seconds. Capture uses ice/crunch cues and release uses a slash cue. Chaos assembles from tumbling ice fragments and bursts into flying shards with glass break audio, ice particles and a finishing water splash.

Buzzkill abilities use short pollination/sting cues instead of long flight-loop samples. Bounty retains individual bee emergence and hit sounds; its hive grows into view and shrinks after release. Tremorstrike Grace forms with stone impact audio and breaks into scattered fragments with dust and break audio.

Everbloom Bounty uses magic damage for its small repeated bolts, avoiding heavy armor reduction. Failed/stopped bolts fade at their last position instead of following the target and appearing to hit. Winterfang Grace, Bubbleveil Grace and Petalwind Grace now use animated particle circles rather than solid 3D support structures.
