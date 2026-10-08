package net.sweenus.simplybows.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import net.minecraft.util.Identifier;

public class SimplyBowsConfig extends Config {

    public static SimplyBowsConfig INSTANCE = ConfigApiJava.registerAndLoadConfig(SimplyBowsConfig::new);

    public SimplyBowsConfig() {
        super(Identifier.of("simplybows", "config"));
    }

    @Comment("In-game name: Winterfang. Enchanted String adds arrows and widens the Chaos frost wall. Reinforced Frame increases arrow damage and lengthens the Chaos wall. 20 ticks = 1 second.")
    public IceBowSection winterfang = new IceBowSection();
    @Comment("In-game name: Everbloom. The flower field heals every player and every animal inside it, and damages only monsters. Enchanted String increases field radius. Reinforced Frame multiplies heal and monster damage. Pain disables healing but still only damages monsters. Grace heals and cleanses instead of damaging. 20 ticks = 1 second.")
    public VineBowSection everbloom = new VineBowSection();
    @Comment("In-game name: Bubbleveil. Enchanted String lengthens the bubble column and the Chaos wave. Reinforced Frame widens and heightens the column, and increases Chaos wave damage and knockback. 20 ticks = 1 second.")
    public BubbleBowSection bubbleveil = new BubbleBowSection();
    @Comment("In-game name: Buzzkill. Enchanted String lengthens poison, Grace bees, the Bounty hive, and the Chaos honey storm, and widens the storm. Reinforced Frame makes Chaos dives happen more often. Bee damage also scales with the global Frame damage multiplier. 20 ticks = 1 second.")
    public BeeBowSection buzzkill = new BeeBowSection();
    @Comment("In-game name: Petalwind. Enchanted String lengthens the storm, widens Pain and Grace areas, widens the Chaos area, and makes koi orbit faster. Bounty always splits into 3 locked storms. Reinforced Frame lengthens Chaos, enlarges koi orbits, and summons more koi. Storm damage also scales with the global Frame damage multiplier. 20 ticks = 1 second.")
    public BlossomBowSection petalwind = new BlossomBowSection();
    @Comment("In-game name: Tremorstrike. Enchanted String increases spike radius, Pain wave distance, and Chaos sunder duration. Reinforced Frame increases knock-up, Bounty spike height, and Chaos acquisition range. Spike damage also scales with the global Frame damage multiplier. 20 ticks = 1 second.")
    public EarthBowSection tremorstrike = new EarthBowSection();
    public LootSection loot = new LootSection();
    public UpgradeSection upgrades = new UpgradeSection();
    public GeneralSection general = new GeneralSection();

    public static class IceBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        @Comment("Number of arrows fired before Enchanted String level bonuses.")
        public ValidatedInt baseQuantity = new ValidatedInt(1, 20, 1);
        @Comment("Initial flight speed multiplier for Winterfang arrows.")
        public ValidatedFloat arrowSpeed = new ValidatedFloat(1.0F, 5.0F, 0.1F);
        @Comment("Random spread applied to each fired arrow.")
        public ValidatedFloat arrowDivergence = new ValidatedFloat(1.0F, 5.0F, 0.0F);
        @Comment("Base damage dealt by Winterfang arrows before upgrades/runes.")
        public ValidatedDouble baseDamage = new ValidatedDouble(0.95, 20.0, 0.1);
        @Comment("The radius that arrows will check for a suitable target.")
        public ValidatedDouble homingRadius = new ValidatedDouble(25.0, 50.0, 1.0);
        @Comment("How strongly arrows steer toward a target each tick.")
        public ValidatedDouble homingAccel = new ValidatedDouble(0.55, 2.0, 0.01);
        @Comment("Ticks after spawn before homing behavior begins.")
        public ValidatedInt homingStartTicks = new ValidatedInt(6, 100, 0);
        @Comment("Horizontal fan spread applied at launch.")
        public ValidatedFloat initialSpreadYaw = new ValidatedFloat(0.28F, 3.0F, 0.0F);
        @Comment("Vertical fan spread applied at launch.")
        public ValidatedFloat initialSpreadPitch = new ValidatedFloat(0.06F, 1.0F, 0.0F);
        @Comment("Starting speed used by homing speed ramp logic.")
        public ValidatedDouble startSpeed = new ValidatedDouble(0.45, 2.0, 0.01);
        @Comment("Maximum speed homing arrows can ramp up to.")
        public ValidatedDouble maxSpeed = new ValidatedDouble(1.0, 3.0, 0.1);
        @ConfigGroup.Pop
        @Comment("Ticks taken to ramp from start speed to max speed.")
        public ValidatedInt speedRampTicks = new ValidatedInt(18, 200, 1);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @Comment("Frost bloom radius on Pain impact.")
        public ValidatedDouble painFrostRadius = new ValidatedDouble(3.25, 12.0, 1.0);
        @Comment("Extra frost bloom radius per Enchanted String level.")
        public ValidatedDouble painFrostRadiusPerString = new ValidatedDouble(0.35, 3.0, 0.0);
        @ConfigGroup.Pop
        @Comment("Unused legacy — Pain frost bloom is Slowness-only now.")
        public ValidatedDouble painFrostDamageMultiplier = new ValidatedDouble(0.175, 10.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        @Comment("Duration of Slowness stacks applied by Grace hits.")
        public ValidatedInt graceSlownessDuration = new ValidatedInt(200, 600, 1);
        @ConfigGroup.Pop
        @Comment("Maximum Slowness stack count from Grace arrows.")
        public ValidatedInt graceMaxSlownessStacks = new ValidatedInt(4, 10, 0);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @Comment("Frost bloom zone radius on Bounty impact.")
        public ValidatedDouble bountyFrostRadius = new ValidatedDouble(3.75, 14.0, 1.0);
        @Comment("Extra Bounty frost radius per Enchanted String level.")
        public ValidatedDouble bountyFrostRadiusPerString = new ValidatedDouble(0.4, 3.0, 0.0);
        @Comment("Damage per Bounty frost pulse vs base arrow damage.")
        public ValidatedDouble bountyFrostDamageMultiplier = new ValidatedDouble(0.14, 5.0, 0.05);
        @ConfigGroup.Pop
        @Comment("How many frost pulses a Bounty bloom fires.")
        public ValidatedInt bountyFrostPulseCount = new ValidatedInt(3, 8, 1);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        @Comment("How long the Chaos ice wall remains active before melting.")
        public ValidatedInt chaosWallDurationTicks = new ValidatedInt(160, 1200, 20);
        @Comment("Cooldown between Chaos wall casts.")
        public ValidatedInt chaosWallCooldownTicks = new ValidatedInt(240, 2400, 20);
        @Comment("Horizontal width of the Chaos ice wall.")
        public ValidatedInt chaosWallWidth = new ValidatedInt(5, 15, 1);
        @Comment("Additional Chaos wall width gained per Enchanted String level.")
        public ValidatedInt chaosWallWidthPerString = new ValidatedInt(1, 5, 0);
        @Comment("Vertical height of the Chaos ice wall.")
        public ValidatedInt chaosWallHeight = new ValidatedInt(3, 10, 1);
        @Comment("Additional Chaos wall duration gained per Reinforced Frame level.")
        public ValidatedInt chaosWallDurationPerFrameTicks = new ValidatedInt(20, 400, 0);
        @ConfigGroup.Pop
        @Comment("Spread used for Chaos arrows before the wall is created on impact.")
        public ValidatedInt chaosWallArrowDivergence = new ValidatedInt(1, 20, 0);
    }

    public static class VineBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        @Comment("Launch speed multiplier for Everbloom arrows.")
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.75F, 3.0F, 0.1F);
        @Comment("Random spread applied to Everbloom arrows.")
        public ValidatedFloat arrowDivergence = new ValidatedFloat(1.15F, 5.0F, 0.0F);
        @Comment("Base arrow damage before field/rune effects.")
        public ValidatedDouble baseDamage = new ValidatedDouble(2.5, 20.0, 0.1);
        @Comment("Extra horizontal drag applied while the arrow flies.")
        public ValidatedDouble extraDragXZ = new ValidatedDouble(0.94, 1.0, 0.5);
        @ConfigGroup.Pop
        @Comment("Extra vertical drag applied while the arrow flies.")
        public ValidatedDouble extraDragY = new ValidatedDouble(0.90, 1.0, 0.5);

        // Flower Field
        public ConfigGroup fieldGroup = new ConfigGroup("flowerField");
        @Comment("Lifetime of the Everbloom flower field.")
        public ValidatedInt fieldDurationTicks = new ValidatedInt(200, 1200, 20);
        @Comment("Radius of the Everbloom flower field aura.")
        public ValidatedDouble fieldRadius = new ValidatedDouble(5.0, 30.0, 1.0);
        @Comment("Health restored to players and animals on each flower-field pulse. Multiplied by Reinforced Frame.")
        public ValidatedFloat friendlyHeal = new ValidatedFloat(2.0F, 20.0F, 0.0F);
        @Comment("Damage dealt to monsters on each flower-field pulse. Players and animals are not damaged. Multiplied by Reinforced Frame. Undead monsters also take the undead bonus.")
        public ValidatedFloat hostileDamage = new ValidatedFloat(0.16F, 20.0F, 0.0F);
        @Comment("Additional damage dealt to undead targets.")
        public ValidatedFloat undeadBonusDamage = new ValidatedFloat(0.48F, 20.0F, 0.0F);
        @ConfigGroup.Pop
        @Comment("Ticks between flower field aura pulses.")
        public ValidatedInt auraIntervalTicks = new ValidatedInt(20, 200, 1);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @ConfigGroup.Pop
        @Comment("Aura pulse interval while Pain rune is active.")
        public ValidatedInt painAuraInterval = new ValidatedInt(10, 100, 1);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @Comment("Aura pulse interval while Bounty DPS field is active (30 = 1.5s).")
        public ValidatedInt bountyAuraInterval = new ValidatedInt(14, 100, 1);
        @ConfigGroup.Pop
        @Comment("Hostile damage multiplier for the Bounty DPS field vs base field damage.")
        public ValidatedFloat bountyDamageMultiplier = new ValidatedFloat(4.5F, 12.0F, 0.1F);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        @Comment("Base radius used to spread Chaos glow lichen tendrils.")
        public ValidatedDouble chaosBaseRadius = new ValidatedDouble(5.5, 30.0, 1.0);
        @Comment("Additional lichen spread radius per Enchanted String level.")
        public ValidatedDouble chaosRadiusPerString = new ValidatedDouble(1.25, 6.0, 0.0);
        @Comment("Base lifetime of the Chaos blossom field before energy extensions.")
        public ValidatedInt chaosBaseDurationTicks = new ValidatedInt(220, 2400, 20);
        @Comment("Cooldown after a Chaos blossom expires before a new Chaos blossom can be created.")
        public ValidatedInt chaosCooldownTicks = new ValidatedInt(480, 2400, 20);
        @Comment("Additional base Chaos lifetime per Reinforced Frame level.")
        public ValidatedInt chaosDurationPerFrameTicks = new ValidatedInt(80, 1200, 0);
        @Comment("Ticks between repeated energy-drain pulses on the same target.")
        public ValidatedInt chaosDrainIntervalTicks = new ValidatedInt(60, 400, 1);
        @Comment("How long drained hostiles are rooted in place.")
        public ValidatedInt chaosRootDurationTicks = new ValidatedInt(60, 400, 1);
        @Comment("Distance from a lichen node required to trigger a drain pulse.")
        public ValidatedDouble chaosNodeTriggerRadius = new ValidatedDouble(0.75, 3.0, 0.1);
        @Comment("Base damage dealt when lichen drains a hostile.")
        public ValidatedFloat chaosBaseDrainDamage = new ValidatedFloat(2.0F, 50.0F, 0.1F);
        @Comment("Additional drain damage gained per energy stack absorbed by the blossom.")
        public ValidatedFloat chaosDrainDamagePerEnergy = new ValidatedFloat(0.3F, 10.0F, 0.0F);
        @Comment("Maximum damage cap for a single Chaos drain pulse.")
        public ValidatedFloat chaosMaxDrainDamage = new ValidatedFloat(12.0F, 200.0F, 0.1F);
        @Comment("Extra lifetime added each time a hostile is drained.")
        public ValidatedInt chaosEnergyDurationExtendTicks = new ValidatedInt(25, 600, 0);
        @Comment("Hard cap for total Chaos field lifetime after energy extensions.")
        public ValidatedInt chaosMaxDurationTicks = new ValidatedInt(700, 4800, 20);
        @Comment("Visual growth amount added to the spore blossom per energy stack.")
        public ValidatedFloat chaosCoreScalePerEnergy = new ValidatedFloat(0.05F, 1.0F, 0.0F);
        @Comment("Maximum additional scale the Chaos blossom can gain from energy.")
        public ValidatedFloat chaosCoreMaxScaleBonus = new ValidatedFloat(1.6F, 5.0F, 0.0F);
        @Comment("Number of glow lichen tendrils generated from the blossom core.")
        public ValidatedInt chaosTendrilCount = new ValidatedInt(7, 24, 1);
        @Comment("Minimum glow lichen nodes generated along each tendril.")
        public ValidatedInt chaosNodesPerTendrilMin = new ValidatedInt(3, 20, 1);
        @Comment("Maximum glow lichen nodes generated along each tendril.")
        public ValidatedInt chaosNodesPerTendrilMax = new ValidatedInt(7, 30, 1);
        @Comment("Radius of the final ally buff burst when the blossom expires.")
        public ValidatedDouble chaosBurstRadius = new ValidatedDouble(6.0, 30.0, 1.0);
        @Comment("Base duration of Strength, Haste, and Speed from Chaos expiry burst.")
        public ValidatedInt chaosBurstBaseBuffDuration = new ValidatedInt(120, 2400, 1);
        @Comment("Additional burst buff duration gained per stored energy stack.")
        public ValidatedInt chaosBurstBuffDurationPerEnergy = new ValidatedInt(8, 300, 0);
        @Comment("Energy required to increase burst buff amplifier by 1.")
        public ValidatedInt chaosBurstEnergyPerAmplifier = new ValidatedInt(5, 100, 1);
        @ConfigGroup.Pop
        @Comment("Maximum amplifier level for Chaos expiry burst buffs.")
        public ValidatedInt chaosBurstMaxAmplifier = new ValidatedInt(2, 5, 0);
    }

    public static class BubbleBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        @Comment("Launch speed multiplier for Bubbleveil arrows.")
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(1.2F, 3.0F, 0.1F);
        @Comment("Random spread applied to Bubbleveil arrows.")
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.8F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        @Comment("Base Bubbleveil arrow damage.")
        public ValidatedDouble baseDamage = new ValidatedDouble(2.75, 20.0, 0.1);

        // Pain mode
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @Comment("Spread used for Bubble Pain axolotl shots.")
        public ValidatedFloat painDivergence = new ValidatedFloat(0.12F, 5.0F, 0.0F);
        @Comment("Pain axolotl shot speed on land. Full draw multiplies this by 3.")
        public ValidatedFloat painShotSpeedLand = new ValidatedFloat(0.9F, 3.0F, 0.01F);
        @ConfigGroup.Pop
        @Comment("Pain axolotl shot speed underwater. Full draw multiplies this by 3.")
        public ValidatedFloat painShotSpeedWater = new ValidatedFloat(1.05F, 3.0F, 0.01F);

        // Bubble Column
        public ConfigGroup columnGroup = new ConfigGroup("bubbleColumn");
        @Comment("Base lifetime of the Bubble Column.")
        public ValidatedInt columnDurationTicks = new ValidatedInt(120, 1200, 10);
        @Comment("Extra column duration per Enchanted String level.")
        public ValidatedInt columnDurationBonusPerString = new ValidatedInt(40, 200, 0);
        @Comment("Base Bubble Column radius before upgrades.")
        public ValidatedDouble columnBaseRadius = new ValidatedDouble(1.2, 10.0, 0.1);
        @Comment("Base Bubble Column height before upgrades.")
        public ValidatedDouble columnBaseHeight = new ValidatedDouble(2.6, 20.0, 0.5);
        @Comment("Additional column radius per Reinforced Frame level.")
        public ValidatedDouble columnRadiusPerFrame = new ValidatedDouble(0.90, 5.0, 0.0);
        @ConfigGroup.Pop
        @Comment("Additional column height per Reinforced Frame level.")
        public ValidatedDouble columnHeightPerFrame = new ValidatedDouble(0.45, 5.0, 0.0);

        // Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        @Comment("Ticks between Grace support pulses.")
        public ValidatedInt gracePulseIntervalTicks = new ValidatedInt(10, 100, 1);
        @Comment("Resistance duration applied to allies in Grace mode.")
        public ValidatedInt graceResistanceDuration = new ValidatedInt(200, 600, 1);
        @ConfigGroup.Pop
        @Comment("Slowness duration applied to hostiles in Grace mode.")
        public ValidatedInt graceSlownessDuration = new ValidatedInt(35, 600, 1);

        // Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @Comment("Ticks between Bounty swarm damage pulses.")
        public ValidatedInt bountyDamageIntervalTicks = new ValidatedInt(10, 100, 1);
        @ConfigGroup.Pop
        @Comment("Base damage for Bubble Bounty swarm ticks.")
        public ValidatedFloat bountyBaseDamage = new ValidatedFloat(0.22F, 30.0F, 0.05F);

        // Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        @Comment("Total width of the Chaos water wave hit area.")
        public ValidatedDouble chaosWaveWidthBlocks = new ValidatedDouble(3.0, 7.0, 1.0);
        @Comment("Front-to-back thickness of each advancing wave damage segment.")
        public ValidatedDouble chaosWaveSegmentThickness = new ValidatedDouble(1.25, 3.0, 0.25);
        @Comment("Forward distance traveled per Chaos wave step.")
        public ValidatedDouble chaosWaveStepDistance = new ValidatedDouble(0.8, 2.0, 0.1);
        @Comment("Tick interval between each advancing wave step.")
        public ValidatedInt chaosWaveStepIntervalTicks = new ValidatedInt(1, 10, 1);
        @Comment("How far in front of the shooter the Chaos wave begins.")
        public ValidatedDouble chaosWaveForwardStartOffset = new ValidatedDouble(1.2, 5.0, 0.1);
        @Comment("Base number of wave steps before String bonuses.")
        public ValidatedInt chaosBaseLengthSteps = new ValidatedInt(7, 30, 1);
        @Comment("Additional wave steps added per Enchanted String level.")
        public ValidatedInt chaosLengthStepsPerString = new ValidatedInt(2, 10, 0);
        @Comment("Base damage dealt by the Chaos wave.")
        public ValidatedFloat chaosBaseDamage = new ValidatedFloat(3.5F, 40.0F, 0.1F);
        @Comment("Additional Chaos wave damage per Reinforced Frame level.")
        public ValidatedFloat chaosDamagePerFrame = new ValidatedFloat(1.35F, 10.0F, 0.1F);
        @Comment("Base forward knockback applied by Chaos wave hits.")
        public ValidatedDouble chaosBaseKnockback = new ValidatedDouble(1.55, 3.0, 0.0);
        @Comment("Additional Chaos wave knockback per Reinforced Frame level.")
        public ValidatedDouble chaosKnockbackPerFrame = new ValidatedDouble(0.22, 1.0, 0.0);
        @ConfigGroup.Pop
        @Comment("Vertical launch applied when the Chaos wave hits a target.")
        public ValidatedDouble chaosKnockUp = new ValidatedDouble(0.32, 2.0, 0.0);
    }

    public static class BeeBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        @Comment("Launch speed multiplier for Buzzkill arrows.")
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.88F, 3.0F, 0.1F);
        @Comment("Random spread applied to Buzzkill arrows.")
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.7F, 5.0F, 0.0F);
        @Comment("Base Buzzkill arrow/bee damage before modifiers.")
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);
        @Comment("Base poison duration applied by bee hits.")
        public ValidatedInt basePoisonDuration = new ValidatedInt(60, 600, 1);
        @Comment("Extra poison duration per Enchanted String level.")
        @ConfigGroup.Pop
        public ValidatedInt stringPoisonDurationBonus = new ValidatedInt(20, 200, 0);

        // Rune: Pain (homing)
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @Comment("Target acquisition radius for Pain homing bees.")
        public ValidatedDouble painHomingRadius = new ValidatedDouble(10.0, 50.0, 1.0);
        @Comment("Ticks before Pain bees begin homing.")
        public ValidatedInt painHomingStartTicks = new ValidatedInt(8, 60, 0);
        @Comment("How strongly Pain bees steer each tick.")
        public ValidatedDouble painHomingAccel = new ValidatedDouble(0.18, 2.0, 0.01);
        @ConfigGroup.Pop
        @Comment("Maximum flight speed for Pain bees.")
        public ValidatedDouble painMaxSpeed = new ValidatedDouble(0.85, 5.0, 0.1);

        // Rune: Grace (shield)
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        @Comment("Radius around impact used to find allies for Grace shields.")
        public ValidatedDouble graceApplyRadius = new ValidatedDouble(2.0, 10.0, 0.5);
        @Comment("Maximum Grace shield bees that can orbit one ally.")
        public ValidatedInt graceMaxBeesPerTarget = new ValidatedInt(5, 20, 1);
        @Comment("Base duration of each Grace shield bee.")
        public ValidatedInt graceBaseDuration = new ValidatedInt(180, 1200, 20);
        @Comment("Extra Grace shield duration per Enchanted String level.")
        public ValidatedInt graceStringDurationBonus = new ValidatedInt(35, 200, 0);
        @Comment("Ticks before another Grace bee can be added to an ally. 80 is four seconds. 20 ticks = 1 second.")
        @ConfigGroup.Pop
        public ValidatedInt graceCooldownTicks = new ValidatedInt(80, 600, 20);

        // Rune: Bounty (hive)
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @Comment("Base lifetime of Bounty beehives.")
        public ValidatedInt bountyHiveDuration = new ValidatedInt(40, 600, 10);
        @Comment("Extra hive duration per Enchanted String level.")
        public ValidatedInt bountyHiveDurationBonusPerString = new ValidatedInt(4, 200, 0);
        @Comment("Base interval between beehive-fired bee shots.")
        public ValidatedInt bountyFireInterval = new ValidatedInt(14, 60, 1);
        @Comment("Base bees spawned instantly by Bounty (String adds more).")
        public ValidatedInt bountyBaseShots = new ValidatedInt(5, 50, 1);
        @Comment("Legacy key — Frame now boosts per-bee damage, not shot count.")
        public ValidatedInt bountyFrameBonusShots = new ValidatedInt(0, 10, 0);
        @ConfigGroup.Pop
        @Comment("Radius a Bounty hive searches for hostile targets.")
        public ValidatedDouble bountyTargetRadius = new ValidatedDouble(7.0, 50.0, 1.0);

        // Rune: Chaos (honey storm)
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        @Comment("How long the Bee Chaos honey storm remains active before subsiding. 100 = 5 seconds.")
        public ValidatedInt chaosBaseDurationTicks = new ValidatedInt(100, 280, 20);
        @Comment("Additional honey storm duration gained per Enchanted String level. Capped in code.")
        public ValidatedInt chaosDurationPerStringTicks = new ValidatedInt(10, 40, 0);
        @Comment("Cooldown applied after the honey storm ends before another can be created.")
        public ValidatedInt chaosCooldownTicks = new ValidatedInt(320, 2400, 20);
        @Comment("Base area radius of the Bee Chaos honey storm.")
        public ValidatedDouble chaosBaseRadius = new ValidatedDouble(4.5, 24.0, 0.5);
        @Comment("Additional storm radius gained per Enchanted String level.")
        public ValidatedDouble chaosRadiusPerString = new ValidatedDouble(0.35, 1.0, 0.0);
        @Comment("Ticks between honey storm aura pulses (slowness on hostiles).")
        public ValidatedInt chaosAuraIntervalTicks = new ValidatedInt(20, 200, 1);
        @Comment("Unused legacy key (Chaos no longer grants Regeneration).")
        public ValidatedInt chaosRegenDurationTicks = new ValidatedInt(0, 400, 0);
        @Comment("Unused legacy key (Chaos no longer grants Regeneration).")
        public ValidatedInt chaosRegenAmplifier = new ValidatedInt(0, 4, 0);
        @Comment("Slowness effect duration applied to hostiles each aura pulse.")
        public ValidatedInt chaosSlownessDurationTicks = new ValidatedInt(60, 400, 1);
        @Comment("Slowness amplifier applied to hostiles in the honey storm.")
        public ValidatedInt chaosSlownessAmplifier = new ValidatedInt(1, 5, 0);
        @Comment("Base tick interval between Chaos dive-bomb bee strikes.")
        public ValidatedInt chaosBaseDiveIntervalTicks = new ValidatedInt(50, 400, 1);
        @Comment("How many ticks are removed from the dive-bomb interval per Reinforced Frame level.")
        public ValidatedInt chaosDiveIntervalReductionPerFrameTicks = new ValidatedInt(6, 100, 0);
        @Comment("Lower clamp for the Chaos dive-bomb interval after Frame scaling.")
        public ValidatedInt chaosMinDiveIntervalTicks = new ValidatedInt(12, 100, 1);
        @Comment("Explosion radius of each dive-bomb bee impact.")
        public ValidatedDouble chaosDiveImpactRadius = new ValidatedDouble(2.25, 10.0, 0.25);
        @ConfigGroup.Pop
        @Comment("Damage dealt by each dive-bomb bee impact.")
        public ValidatedFloat chaosDiveDamage = new ValidatedFloat(6.0F, 50.0F, 0.1F);
    }

    public static class BlossomBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        @Comment("Launch speed multiplier for Petalwind arrows.")
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.78F, 3.0F, 0.1F);
        @Comment("Random spread applied to Petalwind arrows.")
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.85F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        @Comment("Base Petalwind arrow damage before storm effects.")
        public ValidatedDouble baseDamage = new ValidatedDouble(1.5, 20.0, 0.1);

        // Storm
        public ConfigGroup stormGroup = new ConfigGroup("storm");
        @Comment("Base lifetime of a Petalwind storm.")
        public ValidatedInt stormDurationTicks = new ValidatedInt(70, 1200, 20);
        @Comment("Extra storm duration per Enchanted String level.")
        public ValidatedInt stormDurationBonusPerString = new ValidatedInt(10, 400, 0);
        @Comment("Ticks between Petalwind storm damage pulses.")
        public ValidatedInt damageIntervalTicks = new ValidatedInt(10, 100, 1);
        @Comment("Maximum range for a storm to jump to a new target.")
        public ValidatedDouble jumpRange = new ValidatedDouble(8.0, 30.0, 1.0);
        @ConfigGroup.Pop
        @Comment("Base damage per Petalwind storm pulse before multipliers.")
        public ValidatedFloat stormDamage = new ValidatedFloat(1.5F, 20.0F, 0.1F);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @Comment("Base area radius for Pain storm damage.")
        public ValidatedDouble painAreaRadius = new ValidatedDouble(6.5, 30.0, 1.0);
        @ConfigGroup.Pop
        @Comment("Additional Pain area radius per Enchanted String level.")
        public ValidatedDouble painAreaRadiusPerString = new ValidatedDouble(0.8, 5.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        @Comment("Base aura radius for Grace support storms.")
        public ValidatedDouble graceAuraDamageRadius = new ValidatedDouble(3.25, 20.0, 0.5);
        @Comment("Additional Grace aura radius per Enchanted String level.")
        public ValidatedDouble graceAuraRadiusPerString = new ValidatedDouble(0.45, 5.0, 0.0);
        @ConfigGroup.Pop
        @Comment("Duration of Grace buffs applied to allies.")
        public ValidatedInt graceBuffDuration = new ValidatedInt(60, 600, 1);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @Comment("Max simultaneous Bounty petal storms. Default/max 3 — keep at 3 for balance (Fzzy needs min < max).")
        public ValidatedInt bountyMaxStorms = new ValidatedInt(3, 3, 1);
        @Comment("Legacy trap-trigger damage multiplier (unused by locked storms).")
        public ValidatedFloat bountyTriggerDamageMultiplier = new ValidatedFloat(3.2F, 20.0F, 0.1F);
        @Comment("Base trigger radius for Bounty traps.")
        public ValidatedDouble bountyTriggerBaseRadius = new ValidatedDouble(1.75, 10.0, 0.5);
        @ConfigGroup.Pop
        @Comment("Additional trigger radius per Enchanted String level.")
        public ValidatedDouble bountyTriggerRadiusPerString = new ValidatedDouble(0.25, 5.0, 0.0);

        // Rune: Chaos (Koi)
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        @Comment("Base lifetime of the Petalwind Chaos koi swarm.")
        public ValidatedInt chaosDurationTicks = new ValidatedInt(160, 1200, 40);
        @Comment("Additional Chaos swarm duration per Reinforced Frame level.")
        public ValidatedInt chaosDurationPerFrameTicks = new ValidatedInt(30, 400, 0);
        @Comment("Outer area radius used for Chaos ambient waves and petals.")
        public ValidatedDouble chaosRadius = new ValidatedDouble(5.0, 20.0, 1.0);
        @Comment("Additional Chaos area radius per Enchanted String level.")
        public ValidatedDouble chaosRadiusPerString = new ValidatedDouble(0.5, 3.0, 0.0);
        @Comment("Radius each koi orbits around the Chaos center.")
        public ValidatedFloat chaosKoiSwimRadius = new ValidatedFloat(2.5F, 8.0F, 0.5F);
        @Comment("Additional koi orbit radius gained per Reinforced Frame level.")
        public ValidatedFloat chaosKoiSwimRadiusPerFrame = new ValidatedFloat(0.45F, 3.0F, 0.0F);
        @Comment("Base number of orbiting koi spawned by Chaos.")
        public ValidatedInt chaosBaseFishCount = new ValidatedInt(2, 10, 1);
        @Comment("Additional orbiting koi spawned per Reinforced Frame level.")
        public ValidatedInt chaosFishPerFrame = new ValidatedInt(1, 4, 0);
        @Comment("Base ticks needed for koi to complete one full orbit.")
        public ValidatedFloat chaosBaseOrbitPeriodTicks = new ValidatedFloat(90.0F, 400.0F, 1.0F);
        @Comment("Ticks removed from orbit period per Enchanted String level.")
        public ValidatedFloat chaosOrbitPeriodReductionPerStringTicks = new ValidatedFloat(12.0F, 80.0F, 0.0F);
        @Comment("Lower clamp for orbit period after String scaling.")
        public ValidatedFloat chaosMinOrbitPeriodTicks = new ValidatedFloat(28.0F, 200.0F, 1.0F);
        @Comment("Damage dealt when a koi touches a hostile target.")
        public ValidatedFloat chaosContactDamage = new ValidatedFloat(3.0F, 20.0F, 0.1F);
        @Comment("Horizontal knockback force applied when a koi hits a target.")
        public ValidatedDouble chaosContactKnockbackHorizontal = new ValidatedDouble(1.0, 4.0, 0.0);
        @Comment("Vertical lift applied when a koi hits a target.")
        public ValidatedDouble chaosContactKnockbackVertical = new ValidatedDouble(0.22, 1.5, 0.0);
        @Comment("Per-target cooldown between repeated koi contact hits.")
        public ValidatedInt chaosContactCooldownTicks = new ValidatedInt(8, 100, 1);
        @Comment("Proximity radius used for koi contact hits and projectile reflection.")
        public ValidatedDouble chaosTouchRadius = new ValidatedDouble(0.85, 3.0, 0.05);
        @Comment("Velocity multiplier applied to reflected hostile projectiles.")
        public ValidatedDouble chaosProjectileReflectSpeedMultiplier = new ValidatedDouble(1.1, 4.0, 0.05);
        @ConfigGroup.Pop
        @Comment("Per-projectile cooldown before it can be reflected again.")
        public ValidatedInt chaosProjectileReflectCooldownTicks = new ValidatedInt(6, 60, 1);
    }

    public static class EarthBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        @Comment("Launch speed multiplier for Earth arrows.")
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.72F, 3.0F, 0.1F);
        @Comment("Random spread applied to Earth arrows.")
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.9F, 5.0F, 0.0F);
        @Comment("Base Earth arrow damage before field effects.")
        @ConfigGroup.Pop
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);

        // Spike Field
        public ConfigGroup spikeGroup = new ConfigGroup("spikeField");
        @Comment("Base Earth spike field radius.")
        public ValidatedDouble fieldRadius = new ValidatedDouble(3.6, 20.0, 1.0);
        @Comment("Base damage dealt by Earth spikes.")
        public ValidatedFloat spikeDamage = new ValidatedFloat(1.25F, 30.0F, 0.1F);
        @Comment("Base vertical knock-up applied by spike hits.")
        public ValidatedDouble baseUpwardKnockback = new ValidatedDouble(0.4, 3.0, 0.0);
        @Comment("Additional knock-up per Reinforced Frame level.")
        public ValidatedDouble frameUpwardKnockbackPerLevel = new ValidatedDouble(0.10, 1.0, 0.0);
        @Comment("Additional spike field radius per Enchanted String level.")
        public ValidatedDouble stringRadiusBonusPerLevel = new ValidatedDouble(0.45, 3.0, 0.0);
        @Comment("Ticks before another spike field can be placed. Grace launches allies only when a new field is placed. 200 is ten seconds. 20 ticks = 1 second.")
        @ConfigGroup.Pop
        public ValidatedInt fieldLockoutTicks = new ValidatedInt(200, 600, 20);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @Comment("Maximum travel distance of Pain rune spike waves.")
        public ValidatedDouble painWaveMaxDistance = new ValidatedDouble(6.0, 20.0, 1.0);
        @Comment("Distance between consecutive Pain wave spike steps.")
        public ValidatedDouble painWaveStepDistance = new ValidatedDouble(0.8, 5.0, 0.1);
        @Comment("Damage multiplier applied to Pain wave spikes.")
        public ValidatedFloat painWaveDamageMultiplier = new ValidatedFloat(0.55F, 10.0F, 0.1F);
        @ConfigGroup.Pop
        @Comment("Extra Pain wave travel distance per Enchanted String level.")
        public ValidatedDouble painStringWaveDistanceBonusPerLevel = new ValidatedDouble(1.2, 5.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        @Comment("Resistance duration granted in Earth Grace mode.")
        public ValidatedInt graceResistanceDuration = new ValidatedInt(200, 1200, 1);
        @ConfigGroup.Pop
        @Comment("Slow Falling duration granted in Earth Grace mode.")
        public ValidatedInt graceSlowFallingDuration = new ValidatedInt(160, 1200, 1);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @Comment("Base damage multiplier for Bounty center spike impacts.")
        public ValidatedFloat bountyCenterDamageBaseMultiplier = new ValidatedFloat(0.7F, 10.0F, 0.1F);
        @Comment("Extra center spike damage scaling based on proximity.")
        public ValidatedFloat bountyCenterDamageProximityMultiplier = new ValidatedFloat(1.0F, 10.0F, 0.1F);
        @Comment("Base visual height segments for the Bounty center spike.")
        public ValidatedInt bountyCenterBaseHeightSegments = new ValidatedInt(14, 50, 1);
        @ConfigGroup.Pop
        @Comment("Additional center spike height per Reinforced Frame level.")
        public ValidatedInt bountyCenterExtraHeightPerFrame = new ValidatedInt(2, 10, 0);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        @Comment("Base active duration of the Earth Chaos sunder field.")
        public ValidatedInt chaosSunderDurationTicks = new ValidatedInt(200, 360, 200);
        @Comment("Additional sunder duration gained per Enchanted String level. Hard-capped in code.")
        public ValidatedInt chaosSunderDurationPerStringTicks = new ValidatedInt(24, 60, 0);
        @Comment("Base range used to acquire the next hostile after a sunder hit.")
        public ValidatedDouble chaosSunderAcquisitionRange = new ValidatedDouble(12.0, 24.0, 0.5);
        @ConfigGroup.Pop
        @Comment("Additional target acquisition range gained per Reinforced Frame level.")
        public ValidatedDouble chaosSunderAcquisitionRangePerFrame = new ValidatedDouble(1.0, 8.0, 0.0);
    }

    // ── Loot ─────────────────────────────────────────────────

    public static class LootSection extends ConfigSection {
        private static final float CHANCE_SCALE = 1000.0F;

        // Global Drops
        public ConfigGroup globalGroup = new ConfigGroup("globalDrops");
        @Comment("Base chest drop chance for Enchanted String upgrades. Example: 20 = 2.0%.")
        public ValidatedFloat baseStringChance = new ValidatedFloat(20.0F, CHANCE_SCALE, 0.0F);
        @Comment("Base chest drop chance for Reinforced Frame upgrades. Example: 20 = 2.0%.")
        public ValidatedFloat baseFrameChance = new ValidatedFloat(20.0F, CHANCE_SCALE, 0.0F);
        @Comment("Base chest drop chance for rune upgrade items. Example: 3 = 0.3%.")
        public ValidatedFloat baseRuneChance = new ValidatedFloat(3.0F, CHANCE_SCALE, 0.0F);
        @Comment("Base chest drop chance for unique bows. Example: 2 = 0.2%.")
        @ConfigGroup.Pop
        public ValidatedFloat baseUniqueBowChance = new ValidatedFloat(2.0F, CHANCE_SCALE, 0.0F);

        // Biome Boosts
        public ConfigGroup biomeGroup = new ConfigGroup("biomeBoosts");
        @Comment("Structure-specific boosted drop chance for selected bows. Example: 15 = 1.5%.")
        public ValidatedFloat boostedBowChance = new ValidatedFloat(15.0F, CHANCE_SCALE, 0.0F);
        @Comment("Boosted rune drop chance in Ancient City chests. Example: 20 = 2.0%.")
        @ConfigGroup.Pop
        public ValidatedFloat boostedRuneChanceAncientCity = new ValidatedFloat(20.0F, CHANCE_SCALE, 0.0F);
    }

    // ── Upgrades ─────────────────────────────────────────────

    public static class UpgradeSection extends ConfigSection {
        @Comment("Maximum Enchanted String level and maximum Reinforced Frame level on one bow.")
        public ValidatedInt maxLevelPerType = new ValidatedInt(5, 20, 1);
        @Comment("String levels plus Frame levels on one bow cannot exceed this. A rune does not use these slots.")
        public ValidatedInt maxTotalSlots = new ValidatedInt(5, 20, 1);
        @Comment("Enchanted String size formula: 1 + string level * this value. 0.2 means +20% size or radius per String level on bows that use the shared size multiplier.")
        public ValidatedDouble sizeMultiplierPerString = new ValidatedDouble(0.2, 1.0, 0.0);
        @Comment("Reinforced Frame damage formula: 1 + frame level * this value. 0.55 means +55% damage per Frame level on bows that use the shared damage multiplier.")
        public ValidatedDouble damageMultiplierPerFrame = new ValidatedDouble(0.55, 2.0, 0.0);

        // Draw speeds. Lower = faster full draw. These are the ticks needed to fully charge that bow.
        public ConfigGroup drawSpeedGroup = new ConfigGroup("drawSpeeds");
        @Comment("Ticks required to fully draw Everbloom. Lower is a faster draw.")
        public ValidatedFloat drawSpeedEverbloom = new ValidatedFloat(30.0F, 100.0F, 1.0F);
        @Comment("Ticks required to fully draw Winterfang. Lower is a faster draw.")
        public ValidatedFloat drawSpeedWinterfang = new ValidatedFloat(60.0F, 100.0F, 1.0F);
        @Comment("Ticks required to fully draw Bubbleveil. Lower is a faster draw.")
        public ValidatedFloat drawSpeedBubbleveil = new ValidatedFloat(20.0F, 100.0F, 1.0F);
        @Comment("Ticks required to fully draw Buzzkill. Lower is a faster draw.")
        public ValidatedFloat drawSpeedBuzzkill = new ValidatedFloat(20.0F, 100.0F, 1.0F);
        @Comment("Ticks required to fully draw Petalwind. Lower is a faster draw.")
        public ValidatedFloat drawSpeedPetalwind = new ValidatedFloat(40.0F, 100.0F, 1.0F);
        @Comment("Ticks required to fully draw Tremorstrike. Lower is a faster draw.")
        @ConfigGroup.Pop
        public ValidatedFloat drawSpeedTremorstrike = new ValidatedFloat(40.0F, 100.0F, 1.0F);
    }

    // ── General ──────────────────────────────────────────────

    public static class GeneralSection extends ConfigSection {
        @Comment("Enables verbose debug logging and developer diagnostics.")
        public ValidatedBoolean debugMode = new ValidatedBoolean(false);
        @Comment("Uses the modern tooltip style for Simply Bows items.")
        public ValidatedBoolean modernTooltipsEnabled = new ValidatedBoolean(true);
        @Comment("Adds bonus bow ability damage when RangedWeaponAPI is present (finalDamage += attributeValue * configMultiplier).")
        public ValidatedDouble rangedWeaponApiDamageMultiplier = new ValidatedDouble(1.0, 100.0, 0.0);

        // Non-player (mob) usage of unique bows
        @Comment("Lets mobs that are holding a unique bow use its ability.")
        public ValidatedBoolean enableNonPlayerBowUse = new ValidatedBoolean(true);
        @Comment("How often, in ticks, a mob rolls for a shot. 20 ticks = 1 second.")
        public ValidatedInt nonPlayerBowCheckInterval = new ValidatedInt(60, 6000, 1);
        @Comment("Percent chance (0-100) that a mob fires when the check interval elapses.")
        public ValidatedInt nonPlayerBowChance = new ValidatedInt(50, 100, 0);
        @Comment("Multiplies ability damage dealt by mobs using unique bows.")
        public ValidatedDouble nonPlayerBowAbilityDamageModifier = new ValidatedDouble(0.5, 100.0, 0.0);
        @Comment("Multiplies projectile damage dealt by mobs using unique bows.")
        public ValidatedDouble nonPlayerBowProjectileDamageModifier = new ValidatedDouble(0.5, 100.0, 0.0);
        @Comment("Extra multiplier applied when a mob's bow damages a player.")
        public ValidatedDouble nonPlayerBowDamageToPlayersModifier = new ValidatedDouble(0.5, 100.0, 0.0);
    }
}
