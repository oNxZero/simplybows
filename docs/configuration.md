# Configuration reference

[Documentation home](../README.md)

Every validated field in `SimplyBowsConfig.java` is listed below. Defaults and bounds are transcribed from constructors; numeric constructor order is default, maximum, minimum. Paths below identify Java config fields. UI ConfigGroup headings are presentation groups: use the generated TOML as the authority for serialization.

Config file: `config/simplybows/config.toml`. Back up changes and restart to verify behavior. Server settings govern gameplay; client tooltip presentation uses the client configuration. Do not assume a manually edited client value changes server damage.

## Units and effective values

Durations use ticks unless stated otherwise: 20 ticks = 1 second. Distances usually use blocks. Damage uses health points: 2 = 1 heart. Effect amplifiers are zero-based: 0 = level I, 1 = level II. Launch damage coefficients are not necessarily final hit damage.

A field's allowed range is not a promise of the final effective value: managers also apply fixed multipliers, minimums, maximums, and rune-specific formulas. Some source comments are historical. In particular, `petalwind.graceBuffDuration` does not alone describe the current Strength puddle tuning. Winterfang legacy Grace slow-stack and Bounty bloom keys remain loadable but unused after the rune redesign. See [balance formulas](bows.md#balance-formulas) and the managers before tuning these keys.

All loot values use the same per-thousand scale: 0 disables, 1 = 0.1%, 20 = 2%, 1000 = 100%. Fractional values also divide by 1000: 0.5 = 0.05%. Old raw-probability configs must be converted: multiply the old probability by 1000 to preserve its chance.

Shared upgrade formulas: size = `1 + String × sizeMultiplierPerString`; damage = `1 + Frame × damageMultiplierPerFrame` on abilities using those helpers. Winterfang arrow damage instead uses `1 + 0.18 × Frame`. Not every rune uses the shared formulas.

## winterfang

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `winterfang.baseQuantity` | `1` | `1` | `20` | Number of arrows fired before Enchanted String level bonuses. |
| `winterfang.arrowSpeed` | `1.0` | `0.1` | `5.0` | Initial flight speed multiplier for Winterfang arrows. |
| `winterfang.arrowDivergence` | `1.0` | `0.0` | `5.0` | Random spread applied to each fired arrow. |
| `winterfang.baseDamage` | `0.95` | `0.1` | `20.0` | Base damage dealt by Winterfang arrows before upgrades/runes. |
| `winterfang.homingRadius` | `25.0` | `1.0` | `50.0` | The radius that arrows will check for a suitable target. |
| `winterfang.homingAccel` | `0.55` | `0.01` | `2.0` | How strongly arrows steer toward a target each tick. |
| `winterfang.homingStartTicks` | `6` | `0` | `100` | Ticks after spawn before homing behavior begins. |
| `winterfang.initialSpreadYaw` | `0.28` | `0.0` | `3.0` | Horizontal fan spread applied at launch. |
| `winterfang.initialSpreadPitch` | `0.06` | `0.0` | `1.0` | Vertical fan spread applied at launch. |
| `winterfang.startSpeed` | `0.45` | `0.01` | `2.0` | Starting speed used by homing speed ramp logic. |
| `winterfang.maxSpeed` | `1.0` | `0.1` | `3.0` | Maximum speed homing arrows can ramp up to. |
| `winterfang.speedRampTicks` | `18` | `1` | `200` | Ticks taken to ramp from start speed to max speed. |
| `winterfang.painFrostRadius` | `1.75` | `1.0` | `12.0` | Pain impact splash radius in blocks. Effective radius is clamped to 0.5–3 blocks. |
| `winterfang.painFrostRadiusPerString` | `0.35` | `0.0` | `3.0` | Legacy key: unused. Pain String now adds one distinct-target homing arrow per level. |
| `winterfang.painFrostDamageMultiplier` | `0.175` | `0.0` | `10.0` | Pain impact splash damage multiplier. Frost applies Slowness III for five seconds. |
| `winterfang.graceSlownessDuration` | `200` | `1` | `600` | Legacy key: unused. Grace is a harmless support sanctuary. |
| `winterfang.graceMaxSlownessStacks` | `4` | `0` | `10` | Legacy key: unused. Grace no longer applies enemy debuffs. |
| `winterfang.bountyFrostRadius` | `3.75` | `1.0` | `14.0` | Legacy key: unused. Bounty freezes the struck target for 3 seconds plus 0.5 seconds per String. |
| `winterfang.bountyFrostRadiusPerString` | `0.4` | `0.0` | `3.0` | Legacy key: unused. Bounty String now adds 0.5 seconds of freeze duration per level. |
| `winterfang.bountyFrostDamageMultiplier` | `0.14` | `0.05` | `5.0` | Legacy key: unused. Bounty no longer produces frost damage pulses. |
| `winterfang.bountyFrostPulseCount` | `3` | `1` | `8` | Legacy key: unused. Bounty no longer emits frost pulses. |
| `winterfang.chaosWallDurationTicks` | `160` | `20` | `1200` | How long the Chaos ice wall remains active before melting. |
| `winterfang.chaosWallCooldownTicks` | `240` | `20` | `2400` | Cooldown between Chaos wall casts. |
| `winterfang.chaosWallWidth` | `5` | `1` | `15` | Horizontal width of the Chaos ice wall. |
| `winterfang.chaosWallWidthPerString` | `1` | `0` | `5` | Additional Chaos wall width gained per Enchanted String level. |
| `winterfang.chaosWallHeight` | `3` | `1` | `10` | Vertical height of the Chaos ice wall. |
| `winterfang.chaosWallDurationPerFrameTicks` | `20` | `0` | `400` | Additional Chaos wall duration gained per Reinforced Frame level. |
| `winterfang.chaosWallArrowDivergence` | `1` | `0` | `20` | Spread used for Chaos arrows before the wall is created on impact. |

## everbloom

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `everbloom.arrowSpeedMultiplier` | `0.75` | `0.1` | `3.0` | Launch speed multiplier for Everbloom arrows. |
| `everbloom.arrowDivergence` | `1.15` | `0.0` | `5.0` | Random spread applied to Everbloom arrows. |
| `everbloom.baseDamage` | `2.5` | `0.1` | `20.0` | Base arrow damage before field/rune effects. |
| `everbloom.extraDragXZ` | `0.94` | `0.5` | `1.0` | Extra horizontal drag applied while the arrow flies. |
| `everbloom.extraDragY` | `0.90` | `0.5` | `1.0` | Extra vertical drag applied while the arrow flies. |
| `everbloom.fieldDurationTicks` | `200` | `20` | `1200` | Lifetime of the Everbloom flower field. |
| `everbloom.fieldRadius` | `5.0` | `1.0` | `30.0` | Radius of the Everbloom flower field aura. |
| `everbloom.friendlyHeal` | `2.0` | `0.0` | `20.0` | Health restored to players and animals on each flower-field pulse. Multiplied by Reinforced Frame. |
| `everbloom.hostileDamage` | `0.16` | `0.0` | `20.0` | Damage dealt to monsters on each flower-field pulse. Players and animals are not damaged. Multiplied by Reinforced Frame. Undead monsters also take the undead bonus. |
| `everbloom.undeadBonusDamage` | `0.48` | `0.0` | `20.0` | Additional damage dealt to undead targets. |
| `everbloom.auraIntervalTicks` | `20` | `1` | `200` | Ticks between flower field aura pulses. |
| `everbloom.painAuraInterval` | `10` | `1` | `100` | Aura pulse interval while Pain rune is active. |
| `everbloom.bountyAuraInterval` | `14` | `1` | `100` | Aura pulse interval while Bounty DPS field is active (30 = 1.5s). |
| `everbloom.bountyDamageMultiplier` | `4.5` | `0.1` | `12.0` | Hostile damage multiplier for the Bounty DPS field vs base field damage. |
| `everbloom.chaosBaseRadius` | `5.5` | `1.0` | `30.0` | Base radius used to spread Chaos glow lichen tendrils. |
| `everbloom.chaosRadiusPerString` | `1.25` | `0.0` | `6.0` | Additional lichen spread radius per Enchanted String level. |
| `everbloom.chaosBaseDurationTicks` | `220` | `20` | `2400` | Base lifetime of the Chaos blossom field before energy extensions. |
| `everbloom.chaosCooldownTicks` | `480` | `20` | `2400` | Cooldown after a Chaos blossom expires before a new Chaos blossom can be created. |
| `everbloom.chaosDurationPerFrameTicks` | `80` | `0` | `1200` | Additional base Chaos lifetime per Reinforced Frame level. |
| `everbloom.chaosDrainIntervalTicks` | `60` | `1` | `400` | Ticks between repeated energy-drain pulses on the same target. |
| `everbloom.chaosRootDurationTicks` | `60` | `1` | `400` | How long drained hostiles are rooted in place. |
| `everbloom.chaosNodeTriggerRadius` | `0.75` | `0.1` | `3.0` | Distance from a lichen node required to trigger a drain pulse. |
| `everbloom.chaosBaseDrainDamage` | `2.0` | `0.1` | `50.0` | Base damage dealt when lichen drains a hostile. |
| `everbloom.chaosDrainDamagePerEnergy` | `0.3` | `0.0` | `10.0` | Additional drain damage gained per energy stack absorbed by the blossom. |
| `everbloom.chaosMaxDrainDamage` | `12.0` | `0.1` | `200.0` | Maximum damage cap for a single Chaos drain pulse. |
| `everbloom.chaosEnergyDurationExtendTicks` | `25` | `0` | `600` | Extra lifetime added each time a hostile is drained. |
| `everbloom.chaosMaxDurationTicks` | `700` | `20` | `4800` | Hard cap for total Chaos field lifetime after energy extensions. |
| `everbloom.chaosCoreScalePerEnergy` | `0.05` | `0.0` | `1.0` | Visual growth amount added to the spore blossom per energy stack. |
| `everbloom.chaosCoreMaxScaleBonus` | `1.6` | `0.0` | `5.0` | Maximum additional scale the Chaos blossom can gain from energy. |
| `everbloom.chaosTendrilCount` | `7` | `1` | `24` | Number of glow lichen tendrils generated from the blossom core. |
| `everbloom.chaosNodesPerTendrilMin` | `3` | `1` | `20` | Minimum glow lichen nodes generated along each tendril. |
| `everbloom.chaosNodesPerTendrilMax` | `7` | `1` | `30` | Maximum glow lichen nodes generated along each tendril. |
| `everbloom.chaosBurstRadius` | `6.0` | `1.0` | `30.0` | Radius of the visual burst when the Chaos blossom expires. Speed uses the active field radius. |
| `everbloom.chaosBurstBaseBuffDuration` | `120` | `1` | `2400` | Legacy key: unused. The active Chaos field refreshes Speed I for 60 ticks. |
| `everbloom.chaosBurstBuffDurationPerEnergy` | `8` | `0` | `300` | Legacy key: unused. Energy does not increase Speed I duration. |
| `everbloom.chaosBurstEnergyPerAmplifier` | `5` | `1` | `100` | Legacy key: unused. The active Chaos field grants only Speed I. |
| `everbloom.chaosBurstMaxAmplifier` | `2` | `0` | `5` | Legacy key: unused. The active Chaos field grants only Speed I. |

## bubbleveil

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `bubbleveil.arrowSpeedMultiplier` | `1.2` | `0.1` | `3.0` | Launch speed multiplier for Bubbleveil arrows. |
| `bubbleveil.arrowDivergence` | `0.8` | `0.0` | `5.0` | Random spread applied to Bubbleveil arrows. |
| `bubbleveil.baseDamage` | `2.75` | `0.1` | `20.0` | Base Bubbleveil arrow damage. |
| `bubbleveil.painDivergence` | `0.12` | `0.0` | `5.0` | Spread used for Bubble Pain axolotl shots. |
| `bubbleveil.painShotSpeedLand` | `0.9` | `0.01` | `3.0` | Pain axolotl shot speed on land. Full draw multiplies this by 3. |
| `bubbleveil.painShotSpeedWater` | `1.05` | `0.01` | `3.0` | Pain axolotl shot speed underwater. Full draw multiplies this by 3. |
| `bubbleveil.columnDurationTicks` | `120` | `10` | `1200` | Base lifetime of the Bubble Column. |
| `bubbleveil.columnDurationBonusPerString` | `40` | `0` | `200` | Extra column duration per Enchanted String level. |
| `bubbleveil.columnBaseRadius` | `1.2` | `0.1` | `10.0` | Base Bubble Column radius before upgrades. |
| `bubbleveil.columnBaseHeight` | `2.6` | `0.5` | `20.0` | Base Bubble Column height before upgrades. |
| `bubbleveil.columnRadiusPerFrame` | `0.90` | `0.0` | `5.0` | Additional column radius per Reinforced Frame level. |
| `bubbleveil.columnHeightPerFrame` | `0.45` | `0.0` | `5.0` | Additional column height per Reinforced Frame level. |
| `bubbleveil.gracePulseIntervalTicks` | `10` | `1` | `100` | Ticks between Grace support pulses. |
| `bubbleveil.graceResistanceDuration` | `200` | `1` | `600` | Resistance duration applied to allies in Grace mode. |
| `bubbleveil.graceSlownessDuration` | `35` | `1` | `600` | Slowness duration applied to hostiles in Grace mode. |
| `bubbleveil.bountyDamageIntervalTicks` | `10` | `1` | `100` | Ticks between Bounty swarm damage pulses. |
| `bubbleveil.bountyBaseDamage` | `0.22` | `0.05` | `30.0` | Base damage for Bubble Bounty swarm ticks. |
| `bubbleveil.chaosWaveWidthBlocks` | `3.0` | `1.0` | `7.0` | Total width of the Chaos water wave hit area. |
| `bubbleveil.chaosWaveSegmentThickness` | `1.25` | `0.25` | `3.0` | Front-to-back thickness of each advancing wave damage segment. |
| `bubbleveil.chaosWaveStepDistance` | `0.8` | `0.1` | `2.0` | Forward distance traveled per Chaos wave step. |
| `bubbleveil.chaosWaveStepIntervalTicks` | `1` | `1` | `10` | Tick interval between each advancing wave step. |
| `bubbleveil.chaosWaveForwardStartOffset` | `1.2` | `0.1` | `5.0` | How far in front of the shooter the Chaos wave begins. |
| `bubbleveil.chaosBaseLengthSteps` | `7` | `1` | `30` | Base number of wave steps before String bonuses. |
| `bubbleveil.chaosLengthStepsPerString` | `2` | `0` | `10` | Additional wave steps added per Enchanted String level. |
| `bubbleveil.chaosBaseDamage` | `3.5` | `0.1` | `40.0` | Base damage dealt by the Chaos wave. |
| `bubbleveil.chaosDamagePerFrame` | `1.35` | `0.1` | `10.0` | Additional Chaos wave damage per Reinforced Frame level. |
| `bubbleveil.chaosBaseKnockback` | `1.55` | `0.0` | `3.0` | Base forward knockback applied by Chaos wave hits. |
| `bubbleveil.chaosKnockbackPerFrame` | `0.22` | `0.0` | `1.0` | Additional Chaos wave knockback per Reinforced Frame level. |
| `bubbleveil.chaosKnockUp` | `0.32` | `0.0` | `2.0` | Vertical launch applied when the Chaos wave hits a target. |

## buzzkill

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `buzzkill.arrowSpeedMultiplier` | `0.88` | `0.1` | `3.0` | Launch speed multiplier for Buzzkill arrows. |
| `buzzkill.arrowDivergence` | `0.7` | `0.0` | `5.0` | Random spread applied to Buzzkill arrows. |
| `buzzkill.baseDamage` | `2.0` | `0.1` | `20.0` | Base Buzzkill arrow/bee damage before modifiers. |
| `buzzkill.basePoisonDuration` | `60` | `1` | `600` | Base poison duration applied by bee hits. |
| `buzzkill.stringPoisonDurationBonus` | `20` | `0` | `200` | Extra poison duration per Enchanted String level. |
| `buzzkill.painHomingRadius` | `10.0` | `1.0` | `50.0` | Target acquisition radius for Pain homing bees. |
| `buzzkill.painHomingStartTicks` | `8` | `0` | `60` | Ticks before Pain bees begin homing. |
| `buzzkill.painHomingAccel` | `0.18` | `0.01` | `2.0` | How strongly Pain bees steer each tick. |
| `buzzkill.painMaxSpeed` | `0.85` | `0.1` | `5.0` | Maximum flight speed for Pain bees. |
| `buzzkill.graceApplyRadius` | `2.0` | `0.5` | `10.0` | Radius around impact used to find allies for Grace shields. |
| `buzzkill.graceMaxBeesPerTarget` | `5` | `1` | `20` | Maximum Grace shield bees that can orbit one ally. |
| `buzzkill.graceBaseDuration` | `180` | `20` | `1200` | Base duration of each Grace shield bee. |
| `buzzkill.graceStringDurationBonus` | `35` | `0` | `200` | Extra Grace shield duration per Enchanted String level. |
| `buzzkill.graceCooldownTicks` | `80` | `20` | `600` | Ticks before another Grace bee can be added to an ally. 80 is four seconds. 20 ticks = 1 second. |
| `buzzkill.bountyHiveDuration` | `40` | `10` | `600` | Base lifetime of Bounty beehives. |
| `buzzkill.bountyHiveDurationBonusPerString` | `4` | `0` | `200` | Extra hive duration per Enchanted String level. |
| `buzzkill.bountyFireInterval` | `14` | `1` | `60` | Base interval between beehive-fired bee shots. |
| `buzzkill.bountyBaseShots` | `5` | `1` | `50` | Base bees spawned instantly by Bounty (String adds more). |
| `buzzkill.bountyFrameBonusShots` | `0` | `0` | `10` | Legacy key : Frame now boosts per-bee damage, not shot count. |
| `buzzkill.bountyTargetRadius` | `7.0` | `1.0` | `50.0` | Radius a Bounty hive searches for hostile targets. |
| `buzzkill.chaosBaseDurationTicks` | `100` | `20` | `280` | How long the Bee Chaos honey storm remains active before subsiding. 100 = 5 seconds. |
| `buzzkill.chaosDurationPerStringTicks` | `10` | `0` | `40` | Additional honey storm duration gained per Enchanted String level. Capped in code. |
| `buzzkill.chaosCooldownTicks` | `320` | `20` | `2400` | Cooldown applied after the honey storm ends before another can be created. |
| `buzzkill.chaosBaseRadius` | `4.5` | `0.5` | `24.0` | Base area radius of the Bee Chaos honey storm. |
| `buzzkill.chaosRadiusPerString` | `0.35` | `0.0` | `1.0` | Additional storm radius gained per Enchanted String level. |
| `buzzkill.chaosAuraIntervalTicks` | `20` | `1` | `200` | Ticks between honey storm aura pulses (slowness on hostiles). |
| `buzzkill.chaosRegenDurationTicks` | `0` | `0` | `400` | Unused legacy key (Chaos no longer grants Regeneration). |
| `buzzkill.chaosRegenAmplifier` | `0` | `0` | `4` | Unused legacy key (Chaos no longer grants Regeneration). |
| `buzzkill.chaosSlownessDurationTicks` | `60` | `1` | `400` | Slowness effect duration applied to hostiles each aura pulse. |
| `buzzkill.chaosSlownessAmplifier` | `1` | `0` | `5` | Slowness amplifier applied to hostiles in the honey storm. |
| `buzzkill.chaosBaseDiveIntervalTicks` | `50` | `1` | `400` | Base tick interval between Chaos dive-bomb bee strikes. |
| `buzzkill.chaosDiveIntervalReductionPerFrameTicks` | `6` | `0` | `100` | How many ticks are removed from the dive-bomb interval per Reinforced Frame level. |
| `buzzkill.chaosMinDiveIntervalTicks` | `12` | `1` | `100` | Lower clamp for the Chaos dive-bomb interval after Frame scaling. |
| `buzzkill.chaosDiveImpactRadius` | `2.25` | `0.25` | `10.0` | Explosion radius of each dive-bomb bee impact. |
| `buzzkill.chaosDiveDamage` | `6.0` | `0.1` | `50.0` | Damage dealt by each dive-bomb bee impact. |

## petalwind

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `petalwind.arrowSpeedMultiplier` | `0.78` | `0.1` | `3.0` | Launch speed multiplier for Petalwind arrows. |
| `petalwind.arrowDivergence` | `0.85` | `0.0` | `5.0` | Random spread applied to Petalwind arrows. |
| `petalwind.baseDamage` | `1.5` | `0.1` | `20.0` | Base Petalwind arrow damage before storm effects. |
| `petalwind.stormDurationTicks` | `70` | `20` | `1200` | Base lifetime of a Petalwind storm. |
| `petalwind.stormDurationBonusPerString` | `10` | `0` | `400` | Extra storm duration per Enchanted String level. |
| `petalwind.damageIntervalTicks` | `10` | `1` | `100` | Ticks between Petalwind storm damage pulses. |
| `petalwind.jumpRange` | `8.0` | `1.0` | `30.0` | Maximum range for a storm to jump to a new target. |
| `petalwind.stormDamage` | `1.5` | `0.1` | `20.0` | Base damage per Petalwind storm pulse before multipliers. |
| `petalwind.painAreaRadius` | `6.5` | `1.0` | `30.0` | Base area radius for Pain storm damage. |
| `petalwind.painAreaRadiusPerString` | `0.8` | `0.0` | `5.0` | Additional Pain area radius per Enchanted String level. |
| `petalwind.graceAuraDamageRadius` | `3.25` | `0.5` | `20.0` | Base aura radius for Grace support storms. |
| `petalwind.graceAuraRadiusPerString` | `0.45` | `0.0` | `5.0` | Additional Grace aura radius per Enchanted String level. |
| `petalwind.graceBuffDuration` | `60` | `1` | `600` | Duration of Grace buffs applied to allies. |
| `petalwind.bountyMaxStorms` | `3` | `1` | `3` | Max simultaneous Bounty petal storms. Default/max 3 : keep at 3 for balance (Fzzy needs min < max). |
| `petalwind.bountyTriggerDamageMultiplier` | `3.2` | `0.1` | `20.0` | Legacy trap-trigger damage multiplier (unused by locked storms). |
| `petalwind.bountyTriggerBaseRadius` | `1.75` | `0.5` | `10.0` | Base trigger radius for Bounty traps. |
| `petalwind.bountyTriggerRadiusPerString` | `0.25` | `0.0` | `5.0` | Additional trigger radius per Enchanted String level. |
| `petalwind.chaosDurationTicks` | `160` | `40` | `1200` | Base lifetime of the Petalwind Chaos koi swarm. |
| `petalwind.chaosDurationPerFrameTicks` | `30` | `0` | `400` | Additional Chaos swarm duration per Reinforced Frame level. |
| `petalwind.chaosRadius` | `5.0` | `1.0` | `20.0` | Outer area radius used for Chaos ambient waves and petals. |
| `petalwind.chaosRadiusPerString` | `0.5` | `0.0` | `3.0` | Additional Chaos area radius per Enchanted String level. |
| `petalwind.chaosKoiSwimRadius` | `2.5` | `0.5` | `8.0` | Radius each koi orbits around the Chaos center. |
| `petalwind.chaosKoiSwimRadiusPerFrame` | `0.45` | `0.0` | `3.0` | Additional koi orbit radius gained per Reinforced Frame level. |
| `petalwind.chaosBaseFishCount` | `2` | `1` | `10` | Base number of orbiting koi spawned by Chaos. |
| `petalwind.chaosFishPerFrame` | `1` | `0` | `4` | Additional orbiting koi spawned per Reinforced Frame level. |
| `petalwind.chaosBaseOrbitPeriodTicks` | `90.0` | `1.0` | `400.0` | Base ticks needed for koi to complete one full orbit. |
| `petalwind.chaosOrbitPeriodReductionPerStringTicks` | `12.0` | `0.0` | `80.0` | Ticks removed from orbit period per Enchanted String level. |
| `petalwind.chaosMinOrbitPeriodTicks` | `28.0` | `1.0` | `200.0` | Lower clamp for orbit period after String scaling. |
| `petalwind.chaosContactDamage` | `3.0` | `0.1` | `20.0` | Damage dealt when a koi touches a hostile target. |
| `petalwind.chaosContactKnockbackHorizontal` | `1.0` | `0.0` | `4.0` | Horizontal knockback force applied when a koi hits a target. |
| `petalwind.chaosContactKnockbackVertical` | `0.22` | `0.0` | `1.5` | Vertical lift applied when a koi hits a target. |
| `petalwind.chaosContactCooldownTicks` | `8` | `1` | `100` | Per-target cooldown between repeated koi contact hits. |
| `petalwind.chaosTouchRadius` | `0.85` | `0.05` | `3.0` | Proximity radius used for koi contact hits and projectile reflection. |
| `petalwind.chaosProjectileReflectSpeedMultiplier` | `1.1` | `0.05` | `4.0` | Velocity multiplier applied to reflected hostile projectiles. |
| `petalwind.chaosProjectileReflectCooldownTicks` | `6` | `1` | `60` | Per-projectile cooldown before it can be reflected again. |

## tremorstrike

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `tremorstrike.arrowSpeedMultiplier` | `0.72` | `0.1` | `3.0` | Launch speed multiplier for Earth arrows. |
| `tremorstrike.arrowDivergence` | `0.9` | `0.0` | `5.0` | Random spread applied to Earth arrows. |
| `tremorstrike.baseDamage` | `2.0` | `0.1` | `20.0` | Base Earth arrow damage before field effects. |
| `tremorstrike.fieldRadius` | `3.6` | `1.0` | `20.0` | Base Earth spike field radius. |
| `tremorstrike.spikeDamage` | `1.25` | `0.1` | `30.0` | Base damage dealt by Earth spikes. |
| `tremorstrike.baseUpwardKnockback` | `0.4` | `0.0` | `3.0` | Base vertical knock-up applied by spike hits. |
| `tremorstrike.frameUpwardKnockbackPerLevel` | `0.10` | `0.0` | `1.0` | Additional knock-up per Reinforced Frame level. |
| `tremorstrike.stringRadiusBonusPerLevel` | `0.45` | `0.0` | `3.0` | Additional spike field radius per Enchanted String level. |
| `tremorstrike.fieldLockoutTicks` | `200` | `20` | `600` | Ticks before another spike field can be placed. Grace launches allies only when a new field is placed. 200 is ten seconds. 20 ticks = 1 second. |
| `tremorstrike.painWaveMaxDistance` | `6.0` | `1.0` | `20.0` | Maximum travel distance of Pain rune spike waves. |
| `tremorstrike.painWaveStepDistance` | `0.8` | `0.1` | `5.0` | Distance between consecutive Pain wave spike steps. |
| `tremorstrike.painWaveDamageMultiplier` | `0.55` | `0.1` | `10.0` | Damage multiplier applied to Pain wave spikes. |
| `tremorstrike.painStringWaveDistanceBonusPerLevel` | `1.2` | `0.0` | `5.0` | Extra Pain wave travel distance per Enchanted String level. |
| `tremorstrike.graceResistanceDuration` | `200` | `1` | `1200` | Resistance duration granted in Earth Grace mode. |
| `tremorstrike.graceSlowFallingDuration` | `160` | `1` | `1200` | Slow Falling duration granted in Earth Grace mode. |
| `tremorstrike.bountyCenterDamageBaseMultiplier` | `0.7` | `0.1` | `10.0` | Base damage multiplier for Bounty center spike impacts. |
| `tremorstrike.bountyCenterDamageProximityMultiplier` | `1.0` | `0.1` | `10.0` | Extra center spike damage scaling based on proximity. |
| `tremorstrike.bountyCenterBaseHeightSegments` | `14` | `1` | `50` | Base visual height segments for the Bounty center spike. |
| `tremorstrike.bountyCenterExtraHeightPerFrame` | `2` | `0` | `10` | Additional center spike height per Reinforced Frame level. |
| `tremorstrike.chaosSunderDurationTicks` | `200` | `200` | `360` | Base active duration of the Earth Chaos sunder field. |
| `tremorstrike.chaosSunderDurationPerStringTicks` | `24` | `0` | `60` | Additional sunder duration gained per Enchanted String level. Hard-capped in code. |
| `tremorstrike.chaosSunderAcquisitionRange` | `12.0` | `0.5` | `24.0` | Base range used to acquire the next hostile after a sunder hit. |
| `tremorstrike.chaosSunderAcquisitionRangePerFrame` | `1.0` | `0.0` | `8.0` | Additional target acquisition range gained per Reinforced Frame level. |

## loot

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `loot.baseStringChance` | `20.0` | `0.0` | `1000.0` | Base chest drop chance for Enchanted String upgrades. Example: 20 = 2.0%. |
| `loot.baseFrameChance` | `20.0` | `0.0` | `1000.0` | Base chest drop chance for Reinforced Frame upgrades. Example: 20 = 2.0%. |
| `loot.baseRuneChance` | `30.0` | `0.0` | `1000.0` | Base chest drop chance for rune upgrade items. Example: 30 = 3.0%. |
| `loot.baseUniqueBowChance` | `50.0` | `0.0` | `1000.0` | Chance for one random unique bow in vanilla and modded loot chests. Example: 50 = 5.0%. |
| `loot.boostedBowChance` | `15.0` | `0.0` | `1000.0` | Structure-specific boosted drop chance for selected bows. Example: 15 = 1.5%. |
| `loot.boostedRuneChanceAncientCity` | `30.0` | `0.0` | `1000.0` | Additional independent rune drop chance in Ancient City chests. Example: 30 = 3.0%. |

## upgrades

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `upgrades.maxLevelPerType` | `5` | `1` | `20` | Maximum Enchanted String level and maximum Reinforced Frame level on one bow. |
| `upgrades.maxTotalSlots` | `5` | `1` | `20` | String levels plus Frame levels on one bow cannot exceed this. A rune does not use these slots. |
| `upgrades.sizeMultiplierPerString` | `0.2` | `0.0` | `1.0` | Enchanted String size formula: 1 + string level * this value. 0.2 means +20% size or radius per String level on bows that use the shared size multiplier. |
| `upgrades.damageMultiplierPerFrame` | `0.55` | `0.0` | `2.0` | Reinforced Frame damage formula: 1 + frame level * this value. 0.55 means +55% damage per Frame level on bows that use the shared damage multiplier. |
| `upgrades.drawSpeedEverbloom` | `30.0` | `1.0` | `100.0` | Ticks required to fully draw Everbloom. Lower is a faster draw. |
| `upgrades.drawSpeedWinterfang` | `60.0` | `1.0` | `100.0` | Ticks required to fully draw Winterfang. Lower is a faster draw. |
| `upgrades.drawSpeedBubbleveil` | `20.0` | `1.0` | `100.0` | Ticks required to fully draw Bubbleveil. Lower is a faster draw. |
| `upgrades.drawSpeedBuzzkill` | `20.0` | `1.0` | `100.0` | Ticks required to fully draw Buzzkill. Lower is a faster draw. |
| `upgrades.drawSpeedPetalwind` | `40.0` | `1.0` | `100.0` | Ticks required to fully draw Petalwind. Lower is a faster draw. |
| `upgrades.drawSpeedTremorstrike` | `40.0` | `1.0` | `100.0` | Ticks required to fully draw Tremorstrike. Lower is a faster draw. |

## general

| Field | Default | Minimum | Maximum | Source description |
| :--- | :--- | :--- | :--- | :--- |
| `general.debugMode` | `false` | `false` | `true` | Enables verbose debug logging and developer diagnostics. |
| `general.modernTooltipsEnabled` | `true` | `false` | `true` | Uses the modern tooltip style for Simply Bows items. |
| `general.rangedWeaponApiDamageMultiplier` | `1.0` | `0.0` | `100.0` | Adds bonus bow ability damage when RangedWeaponAPI is present (finalDamage += attributeValue * configMultiplier). |
| `general.enableNonPlayerBowUse` | `true` | `false` | `true` | Lets mobs that are holding a unique bow use its ability. |
| `general.nonPlayerBowCheckInterval` | `60` | `1` | `6000` | How often, in ticks, a mob rolls for a shot. 20 ticks = 1 second. |
| `general.nonPlayerBowChance` | `50` | `0` | `100` | Percent chance (0-100) that a mob fires when the check interval elapses. |
| `general.nonPlayerBowAbilityDamageModifier` | `0.5` | `0.0` | `100.0` | Multiplies ability damage dealt by mobs using unique bows. |
| `general.nonPlayerBowProjectileDamageModifier` | `0.5` | `0.0` | `100.0` | Multiplies projectile damage dealt by mobs using unique bows. |
| `general.nonPlayerBowDamageToPlayersModifier` | `0.5` | `0.0` | `100.0` | Extra multiplier applied when a mob's bow damages a player. |

Coverage: **212 validated settings** across 9 sections. ConfigGroup objects and static constants are not user-editable validated settings.

### Replaced rune settings

The new formation variants use `RuneEffectRules` for their timing and damage. Old Tremorstrike Chaos sunder and Bounty center multipliers, Buzzkill Pain volley settings, Bubbleveil Pain volley scaling, and Petalwind Bounty tracking-storm settings no longer tune the replaced formations. Existing config keys remain readable for backward compatibility. Direct projectile base damage, speeds/divergence, common upgrade slots, friendly-fire rules and external ability damage modifiers still apply. See [bows.md](bows.md#new-rune-formations) for current scaling.
