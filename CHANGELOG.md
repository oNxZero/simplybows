# Changelog

Bubbleveil Pain rain: vary falling crystal positions across volleys and spread targeted drops around enemies instead of stacking at their center. Add a ground splash ring to show the existing two-block area damage and Slowness I impact.

Cooldown exploit fixes: Bubbleveil and Buzzkill now honor the vanilla-arrow fallback for partial draws, preventing free Pain clouds and unintended short-draw custom effects. Bubbleveil's normal columns also check and reserve the shared ability cooldown. Shared cooldowns use the Overworld clock across dimensions.

Grace support targeting: Tremorstrike's nearby buffs and Petalwind's initial/jumping target selection now include all support players and creatures regardless of scoreboard team. Petalwind's area Strength buff already included these targets.

Bubbleveil Grace support fix: nearby players receive Resistance II and Regeneration I inside the ward even without a shared scoreboard team.

Winterfang Bounty visual: replace the cube shell and corner decorations with a continuous stepped ice mound, an uneven crown, and branching side spikes. Freeze behavior is unchanged.

## 0.1.4: current fork updates

### Repeat-hit damage and particle support revision

- Use armor-bypassing magic damage for Everbloom tree bolts and prevent failed bolt fades from following enemies.
- Assemble Winterfang Chaos from scattered fragments and explode the wall into shards at expiry.
- Allow Winterfang Grace to support all nearby players and animals; replace its structure with a swirling snow/light circle.
- Replace Bubbleveil Grace's fountain and Petalwind Grace's canopy with floor particle circles.
- Increase Bubbleveil Pain's base volley from 2 to 4 and final volley from 4 to 6; crystal splash damage bypasses armor.
- Retain Buzzkill Bounty's existing stacking poison on exploding bees and poison on full-draw direct hits.

### Spooky flowers, support formations and release effects

- Mix Everbloom Pain flowers; make Bounty tree bolts travel and deal damage on arrival.
- Add Winterfang prison sway/pulse, capture audio and outward release shards applying Frost I for five seconds. Add ice formation and water-melt cues to Chaos.
- Make Winterfang Grace seek friendly players and form a 3D ice sanctuary; replace Bubbleveil Grace's axolotl with a restorative fountain ward and Petalwind Grace's flat effect with a blossom canopy.
- Replace Buzzkill flight-loop samples with short cues and animate the hive entering/leaving; add Tremorstrike Grace stone formation and fragment break effects.
- Reduce Petalwind Pain to 40% base damage and +10% damage per Frame; update its combat and upgrade tooltips.

### Hill eruptions, movement roots and crystal rain

- Halve Tremorstrike Bounty spike heights, preserve the taller center/low edges, and tune launches to approximately two blocks under normal gravity with player velocity synchronization.
- Animate Chaos pairs rising from underground; successful crush hits root movement for five seconds without preventing attacks or item use. Enforce movement on the server and player move packets.
- Thicken Bubbleveil Pain's cloud and raise it two blocks. Drop water crystals straight down; terrain impacts deal splash damage within two blocks and apply Slowness I for two seconds. Split volley damage between crystals to preserve the overlap budget.
- Give Petalwind Bounty a gathering role: a short inward pull concentrates enemies before the blossom releases its focused burst.

### Layer audio and formation animation

- Sound every growing/returning Tremorstrike Bounty layer. Add a formation cue for each Chaos pair and shorten the crush sound gate so adjacent pairs remain audible.
- Chaos teeth break into independently spinning stone chunks instead of shrinking intact. Pairs rise individually, close, shatter and fade.
- Bubbleveil Pain gains a continuous overlapping 3D cloud body with a dark belly, bright crown, particle edges and smooth growth/fade; retain area targeting and homing droplets.
- Petalwind Bounty's three visible glass petals leave the crown one after another and travel outward before seeking an enemy.
- Audit all bow/rune sound palettes; replace the bee launch hurt sound with buzzing and add the travelling water wave's ending splash.

### Audible eruptions, crowd control and hunting formations

- Give Tremorstrike Bounty loud dripstone rise/retraction sounds on all three cycles; increase Chaos crush volume and make colliding teeth break outward with dust and fragments.
- Tremorstrike Pain hits each enemy once across its full fissure, bypasses ordinary immunity frames, and applies stronger backward/upward crowd control. Preserve friendly protection and send player velocity updates.
- Bubbleveil Pain creates a stationary area raincloud on entity or block impact. The cloud uses particles; actual stepped 3D water droplets home toward up to three enemies per volley. String expands the area and lifetime.
- Petalwind Bounty grows one impact blossom with an opening, spinning petal crown and three sequential homing blade launches at distinct enemies.
- New seeking effects save target, damage and elapsed time. Terrain, target teleports, owner loss and a 30-tick flight limit terminate them; endings fade without additional damage.

### Formation polish and readable upgrade labels

- Rebuild Tremorstrike Bounty as a filled fractured star with broad stepped spikes, tapered caps and outward lean; keep its three out/in cycles and damage budget.
- Tremorstrike Chaos now raises five pairs of jagged teeth and closes them in sequence along the firing direction. Damage follows the closing strips, at most once per enemy; remove the inward pull so enemies can escape.
- Tremorstrike Pain adds a modest backward/upward crowd-control shove to successful fissure hits, including player velocity updates.
- Replace Bubbleveil Pain's water coils with a layered raincloud, falling rain, ground ripples and a final downpour. Keep existing single-target damage, duration and cooldown scaling.
- Compact the Alt String/Frame bonuses; detailed upgrade and combat pages retain the explanations.

### Effect audio

- Add positional formation, ambient, impact and ending cues to the new rune formations, frost sanctuary, ice prison pulses, water columns, blossom storms, tree retraction and bee shield/swarm endings.
- Match Tremorstrike field creation to stone and dripstone; use water bubble pops for Bubbleveil's projectile shield and moving wave.
- Keep new recurring cues quiet and limit overlapping sounds locally. Use finite vanilla sounds with bounded per-world bookkeeping.
- Both loaders compile; final volume and timing still require listening in Minecraft.

### Winterfang rune redesign

Pain fires distinct-target homing arrows, one plus String level, with Slowness III for five seconds and one small impact splash per victim per volley. Bounty fires a non-homing single arrow that traps the struck target in a spiky packed-ice shell for exactly three seconds, with server-enforced movement and action restrictions. Grace fires non-homing, harmless sanctuary arrows without enemy damage or debuffs, including during cooldown; partial Grace draws do not fire. Frame extends Grace buff duration instead of arrow damage. Update upgrade previews, stats, config comments, save tags, and documentation.

### Conservative Bubbleveil Pain buff

Increase the base volley damage coefficient from 0.28 to 0.35 (+25%). String adds 5% of base volley damage per level, capped at 25%, while still dividing total damage across the shots. Update combat estimates and upgrade descriptions. Keep cooldown, ammunition costs, and Frame scaling; do not add Slowness. Add scaling-cap and five-slot budget checks. In-game PvP balance is not yet measured.

### Loot probability fix

All loot chances now use one per-thousand scale, including values at or below 1. `1` means 0.1%, `0.5` means 0.05%, and `1000` means 100%. The former raw-probability compatibility branch is removed. Existing defaults retain their chances; legacy raw-probability configurations must multiply their old values by 1000 to preserve the intended rate. Added boundary and fractional-chance regression coverage.

### Gameplay and saves

Anvil upgrades require bow left and component middle, consume one component, and support simultaneous renaming. Removed the right-click upgrade shortcut. Winterfang counts finite ammo across stacks, handles supported Infinity use, and requires one arrow for ready rune shots. All seven special projectile classes preserve their upgrade and ability state, including normal and spectral Winterfang homing arrows.

Retuned Everbloom Pain/Bounty, Petalwind normal/Bounty, and Bubbleveil Bounty damage. Increased Everbloom tree spacing. Bubbleveil Chaos travels four blocks farther and has reduced high-Frame knockback. Everbloom Pain uses direct magic pulses to avoid Wither overlap.

### Tooltips and documentation

Restored compact String/Frame bonuses, combined contextual upgrade previews, clearer combat stats, single-decimal formatting, named page headings, and colored plus navigation with the rebound key. Removed duplicate headings and attribute lore. Bow names use their individual colors; rune names no longer say “etching”. Split renderer hooks by loader and check them against Simply Tooltips 0.1.5.

Replaced the README with a concise overview and linked guides covering installation, all bow/rune builds, upgrade costs, all 212 validated settings, loot, commands, development, persistence, and manual tests.

[Earlier revision notes](docs/historical-changelog.md)

Winterfang Bounty balance: +50% base arrow damage, String adds 0.3 seconds of freeze per level, and Frame reduces shared ability cooldown by 0.3 seconds per level instead of adding damage. Updated lore, previews and combat stats.

Winterfang Bounty follow-up: fixed 18-second shared cooldown, +0.5s freeze per String, Frost I for 10s from impact, 1 prison damage each second plus 0.5 per Frame, and doubled current shot damage. Added eased ice growth and release/shatter animation.

Winterfang Bounty visual: normal translucent ice, irregular crystal shoulders, shorter crown, and long spikes extending sideways rather than a pyramid of upward spikes. Existing growth and release animation retained.

Loot defaults: base rune chance 3% (30 per thousand), base unique bow chance 5% (50), and an additional independent Ancient City rune roll at 3% (30). Existing saved configs retain their values.

Projectile enchantment compatibility: save bow snapshots on all eight custom arrow types, run vanilla spawn enchantments for Flame, and add custom arrows to the vanilla arrow entity tag so Power and Punch apply on direct hits. Protect zero-damage and ally support hits from ignition. Winterfang Grace and ability-spawned bees remain unaffected; ability damage keeps its own scaling.

Bow overview tooltips show actual equipped enchantments as named level bars in the upgrade layout, with bow colors and red curses. Removed duplicate plain enchantment lore. Enchantment changes are included in the tooltip content key; command-created bars are capped at ten squares with the true high level displayed in the label.

Infinity pickup fix: custom arrows now use Creative-only pickup when ammunition is free, covering upgraded fans and rune shots. Non-player free shots disallow pickup; consumed tipped/spectral arrows retain normal recovery. Pickup permission is saved by vanilla arrow NBT.

Homing Enderman fix: Winterfang normal/spectral homing arrows and Buzzkill homing bees discard after attempting to hit an Enderman, preventing repeated projectile-dodge teleport chases. Vanilla gets its first hit/dodge attempt before cleanup.

Airborne homing projectiles also expire after 10 seconds of flight, so targets that repeatedly dodge before collision cannot leave an endless pursuit. Grounded arrows retain their existing pickup/despawn behavior.

Tooltip correction: render bow enchantment pips as filled square markers instead of the Tooltips default thin pips. Homing cleanup now handles any attempted entity impact and tracked-target position jumps over four blocks in one tick, including modded teleporters; the existing flight timeout remains.

### Rune formation redesign

- Tremorstrike Grace blocks everyone, including caster/allies, and checks swept projectile crossings.
- Tremorstrike Chaos replaces the rotating spike arc with rising, inward-closing stone jaws.
- Tremorstrike Bounty keeps three eruptions, now travelling from center outward along eight ground-following star rays. Each target takes at most one hit per wave.
- Buzzkill Pain replaces the spread volley with one homing delivery bee and an attached animated stinging swarm.
- Bubbleveil Pain replaces the axolotl volley with a rotating water-core shot, attached water vortex, timed pulses, Slowness and final burst.
- Petalwind Bounty replaces tracking storms with opening/closing 3D blossoms on up to three different enemies.
- New formations save their elapsed time, owner, target, upgrades and per-wave hit history. Updated rune descriptions, upgrade bonuses and combat pages use the same rules as gameplay.
- Built for Fabric and NeoForge; rendering, multiplayer collision and balance still need in-game verification.

### Rune effect visual refinements

- Fixed Tremorstrike Chaos rejecting its own impact because the launch had already reserved the cooldown.
- Tremorstrike Bounty now fills an eight-point star with spikes and cycles outward/inward three times; each cycle retains one hit per enemy.
- Doubled Tremorstrike Pain fissure visuals to 100 staggered spikes without doubling damage.
- Bubbleveil Pain uses animated flowing-water textures, downward-moving water beads, falling drops and splashes instead of stained-glass coils.
- Formation effects grow in and receive a separate 0.6s finish animation, including target death, owner loss and teleport cancellation. No damage occurs during the finish; final pulses are no longer faded out beforehand. Finish state persists across reload.
