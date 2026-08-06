package net.sweenus.simplybows.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.util.Identifier;

public class SimplyBowsConfig extends Config {

    public static SimplyBowsConfig INSTANCE = ConfigApiJava.registerAndLoadConfig(SimplyBowsConfig::new);

    public SimplyBowsConfig() {
        super(new Identifier("simplybows", "config"));
    }

    public IceBowSection iceBow = new IceBowSection();
    public VineBowSection vineBow = new VineBowSection();
    public BubbleBowSection bubbleBow = new BubbleBowSection();
    public BeeBowSection beeBow = new BeeBowSection();
    public BlossomBowSection blossomBow = new BlossomBowSection();
    public EarthBowSection earthBow = new EarthBowSection();
    public EchoBowSection echoBow = new EchoBowSection();
    public CosmicBowSection cosmicBow = new CosmicBowSection();
    public LootSection loot = new LootSection();
    public UpgradeSection upgrades = new UpgradeSection();
    public GeneralSection general = new GeneralSection();

    // ── Ice Bow ──────────────────────────────────────────────

    public static class IceBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedInt baseQuantity = new ValidatedInt(3, 20, 1);
        public ValidatedFloat arrowSpeed = new ValidatedFloat(1.2F, 5.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(1.0F, 5.0F, 0.0F);
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);
        public ValidatedDouble homingRadius = new ValidatedDouble(10.0, 50.0, 1.0);
        public ValidatedDouble homingAccel = new ValidatedDouble(0.3, 2.0, 0.01);
        public ValidatedInt homingStartTicks = new ValidatedInt(15, 100, 0);
        public ValidatedFloat initialSpreadYaw = new ValidatedFloat(0.90F, 3.0F, 0.0F);
        public ValidatedFloat initialSpreadPitch = new ValidatedFloat(0.14F, 1.0F, 0.0F);
        public ValidatedDouble startSpeed = new ValidatedDouble(0.2, 2.0, 0.01);
        public ValidatedDouble maxSpeed = new ValidatedDouble(0.7, 3.0, 0.1);
        @ConfigGroup.Pop
        public ValidatedInt speedRampTicks = new ValidatedInt(40, 200, 1);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedDouble painTargetHorizontalRange = new ValidatedDouble(48.0, 128.0, 1.0);
        public ValidatedDouble painTargetVerticalRange = new ValidatedDouble(16.0, 64.0, 1.0);
        @ConfigGroup.Pop
        public ValidatedDouble painDamageMultiplier = new ValidatedDouble(1.5, 10.0, 0.1);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedInt graceSlownessDuration = new ValidatedInt(80, 600, 1);
        @ConfigGroup.Pop
        public ValidatedInt graceMaxSlownessStacks = new ValidatedInt(4, 10, 0);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        public ValidatedDouble bountyDamageMultiplier = new ValidatedDouble(0.75, 5.0, 0.1);
        @ConfigGroup.Pop
        public ValidatedInt bountyExtraArrowMultiplier = new ValidatedInt(2, 10, 1);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedInt chaosWallDurationTicks = new ValidatedInt(160, 1200, 20);
        public ValidatedInt chaosWallCooldownTicks = new ValidatedInt(240, 2400, 20);
        public ValidatedInt chaosWallWidth = new ValidatedInt(5, 15, 1);
        public ValidatedInt chaosWallWidthPerString = new ValidatedInt(1, 5, 0);
        public ValidatedInt chaosWallHeight = new ValidatedInt(3, 10, 1);
        public ValidatedInt chaosWallDurationPerFrameTicks = new ValidatedInt(20, 400, 0);
        @ConfigGroup.Pop
        public ValidatedInt chaosWallArrowDivergence = new ValidatedInt(1, 20, 0);
    }

    // ── Vine Bow ─────────────────────────────────────────────

    public static class VineBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.55F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(1.15F, 5.0F, 0.0F);
        public ValidatedDouble baseDamage = new ValidatedDouble(1.5, 20.0, 0.1);
        public ValidatedDouble extraDragXZ = new ValidatedDouble(0.94, 1.0, 0.5);
        @ConfigGroup.Pop
        public ValidatedDouble extraDragY = new ValidatedDouble(0.90, 1.0, 0.5);

        // Flower Field
        public ConfigGroup fieldGroup = new ConfigGroup("flowerField");
        public ValidatedInt fieldDurationTicks = new ValidatedInt(200, 1200, 20);
        public ValidatedDouble fieldRadius = new ValidatedDouble(5.0, 30.0, 1.0);
        public ValidatedFloat friendlyHeal = new ValidatedFloat(1.0F, 20.0F, 0.0F);
        public ValidatedFloat hostileDamage = new ValidatedFloat(2.0F, 20.0F, 0.0F);
        public ValidatedFloat undeadBonusDamage = new ValidatedFloat(0.6F, 20.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedInt auraIntervalTicks = new ValidatedInt(20, 200, 1);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        @ConfigGroup.Pop
        public ValidatedInt painAuraInterval = new ValidatedInt(10, 100, 1);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        @ConfigGroup.Pop
        public ValidatedDouble bountyLootChance = new ValidatedDouble(0.25, 1.0, 0.0);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedDouble chaosBaseRadius = new ValidatedDouble(5.5, 30.0, 1.0);
        public ValidatedDouble chaosRadiusPerString = new ValidatedDouble(1.25, 6.0, 0.0);
        public ValidatedInt chaosBaseDurationTicks = new ValidatedInt(220, 2400, 20);
        public ValidatedInt chaosCooldownTicks = new ValidatedInt(480, 2400, 20);
        public ValidatedInt chaosDurationPerFrameTicks = new ValidatedInt(80, 1200, 0);
        public ValidatedInt chaosDrainIntervalTicks = new ValidatedInt(60, 400, 1);
        public ValidatedInt chaosRootDurationTicks = new ValidatedInt(60, 400, 1);
        public ValidatedDouble chaosNodeTriggerRadius = new ValidatedDouble(0.75, 3.0, 0.1);
        public ValidatedFloat chaosBaseDrainDamage = new ValidatedFloat(2.0F, 50.0F, 0.1F);
        public ValidatedFloat chaosDrainDamagePerEnergy = new ValidatedFloat(0.3F, 10.0F, 0.0F);
        public ValidatedFloat chaosMaxDrainDamage = new ValidatedFloat(12.0F, 200.0F, 0.1F);
        public ValidatedInt chaosEnergyDurationExtendTicks = new ValidatedInt(25, 600, 0);
        public ValidatedInt chaosMaxDurationTicks = new ValidatedInt(700, 4800, 20);
        public ValidatedFloat chaosCoreScalePerEnergy = new ValidatedFloat(0.05F, 1.0F, 0.0F);
        public ValidatedFloat chaosCoreMaxScaleBonus = new ValidatedFloat(1.6F, 5.0F, 0.0F);
        public ValidatedInt chaosTendrilCount = new ValidatedInt(7, 24, 1);
        public ValidatedInt chaosNodesPerTendrilMin = new ValidatedInt(3, 20, 1);
        public ValidatedInt chaosNodesPerTendrilMax = new ValidatedInt(7, 30, 1);
        public ValidatedDouble chaosBurstRadius = new ValidatedDouble(6.0, 30.0, 1.0);
        public ValidatedInt chaosBurstBaseBuffDuration = new ValidatedInt(120, 2400, 1);
        public ValidatedInt chaosBurstBuffDurationPerEnergy = new ValidatedInt(8, 300, 0);
        public ValidatedInt chaosBurstEnergyPerAmplifier = new ValidatedInt(5, 100, 1);
        @ConfigGroup.Pop
        public ValidatedInt chaosBurstMaxAmplifier = new ValidatedInt(2, 5, 0);
    }

    // ── Bubble Bow ───────────────────────────────────────────

    public static class BubbleBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.95F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.8F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedDouble baseDamage = new ValidatedDouble(1.75, 20.0, 0.1);

        // Pain mode
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedFloat painDivergence = new ValidatedFloat(0.12F, 5.0F, 0.0F);
        public ValidatedFloat painSpeedMultiplierLand = new ValidatedFloat(0.22F, 3.0F, 0.01F);
        @ConfigGroup.Pop
        public ValidatedFloat painSpeedMultiplierWater = new ValidatedFloat(0.62F, 3.0F, 0.01F);

        // Bubble Column
        public ConfigGroup columnGroup = new ConfigGroup("bubbleColumn");
        public ValidatedInt columnDurationTicks = new ValidatedInt(120, 1200, 10);
        public ValidatedInt columnDurationBonusPerString = new ValidatedInt(40, 200, 0);
        public ValidatedDouble columnBaseRadius = new ValidatedDouble(1.2, 10.0, 0.1);
        public ValidatedDouble columnBaseHeight = new ValidatedDouble(2.6, 20.0, 0.5);
        public ValidatedDouble columnRadiusPerFrame = new ValidatedDouble(0.90, 5.0, 0.0);
        @ConfigGroup.Pop
        public ValidatedDouble columnHeightPerFrame = new ValidatedDouble(0.45, 5.0, 0.0);

        // Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedInt gracePulseIntervalTicks = new ValidatedInt(10, 100, 1);
        public ValidatedInt graceResistanceDuration = new ValidatedInt(40, 600, 1);
        @ConfigGroup.Pop
        public ValidatedInt graceSlownessDuration = new ValidatedInt(35, 600, 1);

        // Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        public ValidatedInt bountyDamageIntervalTicks = new ValidatedInt(10, 100, 1);
        @ConfigGroup.Pop
        public ValidatedFloat bountyBaseDamage = new ValidatedFloat(3.75F, 30.0F, 0.1F);

        // Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedDouble chaosWaveWidthBlocks = new ValidatedDouble(3.0, 7.0, 1.0);
        public ValidatedDouble chaosWaveSegmentThickness = new ValidatedDouble(1.25, 3.0, 0.25);
        public ValidatedDouble chaosWaveStepDistance = new ValidatedDouble(0.8, 2.0, 0.1);
        public ValidatedInt chaosWaveStepIntervalTicks = new ValidatedInt(1, 10, 1);
        public ValidatedDouble chaosWaveForwardStartOffset = new ValidatedDouble(1.2, 5.0, 0.1);
        public ValidatedInt chaosBaseLengthSteps = new ValidatedInt(7, 30, 1);
        public ValidatedInt chaosLengthStepsPerString = new ValidatedInt(2, 10, 0);
        public ValidatedFloat chaosBaseDamage = new ValidatedFloat(3.5F, 40.0F, 0.1F);
        public ValidatedFloat chaosDamagePerFrame = new ValidatedFloat(1.35F, 10.0F, 0.1F);
        public ValidatedDouble chaosBaseKnockback = new ValidatedDouble(0.52, 3.0, 0.0);
        public ValidatedDouble chaosKnockbackPerFrame = new ValidatedDouble(0.08, 1.0, 0.0);
        @ConfigGroup.Pop
        public ValidatedDouble chaosKnockUp = new ValidatedDouble(0.14, 2.0, 0.0);
    }

    // ── Bee Bow ──────────────────────────────────────────────

    public static class BeeBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.48F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.7F, 5.0F, 0.0F);
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);
        public ValidatedInt basePoisonDuration = new ValidatedInt(60, 600, 1);
        @ConfigGroup.Pop
        public ValidatedInt stringPoisonDurationBonus = new ValidatedInt(20, 200, 0);

        // Rune: Pain (homing)
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedDouble painHomingRadius = new ValidatedDouble(10.0, 50.0, 1.0);
        public ValidatedInt painHomingStartTicks = new ValidatedInt(8, 60, 0);
        public ValidatedDouble painHomingAccel = new ValidatedDouble(0.18, 2.0, 0.01);
        @ConfigGroup.Pop
        public ValidatedDouble painMaxSpeed = new ValidatedDouble(1.05, 5.0, 0.1);

        // Rune: Grace (shield)
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedDouble graceApplyRadius = new ValidatedDouble(2.0, 10.0, 0.5);
        public ValidatedInt graceMaxBeesPerTarget = new ValidatedInt(5, 20, 1);
        public ValidatedInt graceBaseDuration = new ValidatedInt(180, 1200, 20);
        @ConfigGroup.Pop
        public ValidatedInt graceStringDurationBonus = new ValidatedInt(35, 200, 0);

        // Rune: Bounty (hive)
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        public ValidatedInt bountyHiveDuration = new ValidatedInt(60, 600, 10);
        public ValidatedInt bountyHiveDurationBonusPerString = new ValidatedInt(20, 200, 0);
        public ValidatedInt bountyFireInterval = new ValidatedInt(5, 60, 1);
        public ValidatedInt bountyBaseShots = new ValidatedInt(7, 50, 1);
        public ValidatedInt bountyFrameBonusShots = new ValidatedInt(1, 10, 0);
        @ConfigGroup.Pop
        public ValidatedDouble bountyTargetRadius = new ValidatedDouble(14.0, 50.0, 1.0);

        // Rune: Chaos (honey storm)
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedInt chaosBaseDurationTicks = new ValidatedInt(260, 2400, 20);
        public ValidatedInt chaosDurationPerStringTicks = new ValidatedInt(80, 800, 0);
        public ValidatedInt chaosCooldownTicks = new ValidatedInt(320, 2400, 20);
        public ValidatedDouble chaosBaseRadius = new ValidatedDouble(4.5, 24.0, 0.5);
        public ValidatedDouble chaosRadiusPerString = new ValidatedDouble(1.0, 8.0, 0.0);
        public ValidatedInt chaosAuraIntervalTicks = new ValidatedInt(20, 200, 1);
        public ValidatedInt chaosRegenDurationTicks = new ValidatedInt(60, 400, 1);
        public ValidatedInt chaosRegenAmplifier = new ValidatedInt(0, 4, 0);
        public ValidatedInt chaosSlownessDurationTicks = new ValidatedInt(60, 400, 1);
        public ValidatedInt chaosSlownessAmplifier = new ValidatedInt(1, 5, 0);
        public ValidatedInt chaosBaseDiveIntervalTicks = new ValidatedInt(50, 400, 1);
        public ValidatedInt chaosDiveIntervalReductionPerFrameTicks = new ValidatedInt(6, 100, 0);
        public ValidatedInt chaosMinDiveIntervalTicks = new ValidatedInt(12, 100, 1);
        public ValidatedDouble chaosDiveImpactRadius = new ValidatedDouble(2.25, 10.0, 0.25);
        @ConfigGroup.Pop
        public ValidatedFloat chaosDiveDamage = new ValidatedFloat(16.0F, 50.0F, 0.1F);
    }

    // ── Blossom Bow ──────────────────────────────────────────

    public static class BlossomBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.78F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.85F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedDouble baseDamage = new ValidatedDouble(1.5, 20.0, 0.1);

        // Storm
        public ConfigGroup stormGroup = new ConfigGroup("storm");
        public ValidatedInt stormDurationTicks = new ValidatedInt(200, 1200, 20);
        public ValidatedInt stormDurationBonusPerString = new ValidatedInt(40, 400, 0);
        public ValidatedInt damageIntervalTicks = new ValidatedInt(10, 100, 1);
        public ValidatedDouble jumpRange = new ValidatedDouble(8.0, 30.0, 1.0);
        @ConfigGroup.Pop
        public ValidatedFloat stormDamage = new ValidatedFloat(1.5F, 20.0F, 0.1F);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedDouble painAreaRadius = new ValidatedDouble(6.5, 30.0, 1.0);
        @ConfigGroup.Pop
        public ValidatedDouble painAreaRadiusPerString = new ValidatedDouble(0.8, 5.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedDouble graceAuraDamageRadius = new ValidatedDouble(3.25, 20.0, 0.5);
        public ValidatedDouble graceAuraRadiusPerString = new ValidatedDouble(0.45, 5.0, 0.0);
        @ConfigGroup.Pop
        public ValidatedInt graceBuffDuration = new ValidatedInt(60, 600, 1);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        public ValidatedInt bountyBaseMaxTraps = new ValidatedInt(3, 20, 1);
        public ValidatedInt bountyMaxTrapsPerString = new ValidatedInt(1, 10, 0);
        public ValidatedFloat bountyTriggerDamageMultiplier = new ValidatedFloat(3.2F, 20.0F, 0.1F);
        public ValidatedDouble bountyTriggerBaseRadius = new ValidatedDouble(1.75, 10.0, 0.5);
        @ConfigGroup.Pop
        public ValidatedDouble bountyTriggerRadiusPerString = new ValidatedDouble(0.25, 5.0, 0.0);

        // Rune: Chaos (Koi)
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedInt chaosDurationTicks = new ValidatedInt(300, 1200, 60);
        public ValidatedInt chaosDurationPerFrameTicks = new ValidatedInt(60, 400, 0);
        public ValidatedDouble chaosRadius = new ValidatedDouble(5.0, 20.0, 1.0);
        public ValidatedDouble chaosRadiusPerString = new ValidatedDouble(0.5, 3.0, 0.0);
        public ValidatedFloat chaosKoiSwimRadius = new ValidatedFloat(2.5F, 8.0F, 0.5F);
        public ValidatedFloat chaosKoiSwimRadiusPerFrame = new ValidatedFloat(0.45F, 3.0F, 0.0F);
        public ValidatedInt chaosBaseFishCount = new ValidatedInt(2, 10, 1);
        public ValidatedInt chaosFishPerFrame = new ValidatedInt(1, 4, 0);
        public ValidatedFloat chaosBaseOrbitPeriodTicks = new ValidatedFloat(90.0F, 400.0F, 1.0F);
        public ValidatedFloat chaosOrbitPeriodReductionPerStringTicks = new ValidatedFloat(12.0F, 80.0F, 0.0F);
        public ValidatedFloat chaosMinOrbitPeriodTicks = new ValidatedFloat(28.0F, 200.0F, 1.0F);
        public ValidatedFloat chaosContactDamage = new ValidatedFloat(3.0F, 20.0F, 0.1F);
        public ValidatedDouble chaosContactKnockbackHorizontal = new ValidatedDouble(1.0, 4.0, 0.0);
        public ValidatedDouble chaosContactKnockbackVertical = new ValidatedDouble(0.22, 1.5, 0.0);
        public ValidatedInt chaosContactCooldownTicks = new ValidatedInt(8, 100, 1);
        public ValidatedDouble chaosTouchRadius = new ValidatedDouble(0.85, 3.0, 0.05);
        public ValidatedDouble chaosProjectileReflectSpeedMultiplier = new ValidatedDouble(1.1, 4.0, 0.05);
        @ConfigGroup.Pop
        public ValidatedInt chaosProjectileReflectCooldownTicks = new ValidatedInt(6, 60, 1);
    }

    // ── Earth Bow ────────────────────────────────────────────

    public static class EarthBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.72F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.9F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);

        // Spike Field
        public ConfigGroup spikeGroup = new ConfigGroup("spikeField");
        public ValidatedDouble fieldRadius = new ValidatedDouble(3.6, 20.0, 1.0);
        public ValidatedFloat spikeDamage = new ValidatedFloat(3.0F, 30.0F, 0.1F);
        public ValidatedDouble baseUpwardKnockback = new ValidatedDouble(0.4, 3.0, 0.0);
        public ValidatedDouble frameUpwardKnockbackPerLevel = new ValidatedDouble(0.10, 1.0, 0.0);
        @ConfigGroup.Pop
        public ValidatedDouble stringRadiusBonusPerLevel = new ValidatedDouble(0.45, 3.0, 0.0);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedDouble painWaveMaxDistance = new ValidatedDouble(4.0, 20.0, 1.0);
        public ValidatedDouble painWaveStepDistance = new ValidatedDouble(0.8, 5.0, 0.1);
        public ValidatedFloat painWaveDamageMultiplier = new ValidatedFloat(1.0F, 10.0F, 0.1F);
        @ConfigGroup.Pop
        public ValidatedDouble painStringWaveDistanceBonusPerLevel = new ValidatedDouble(1.2, 5.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedInt graceResistanceDuration = new ValidatedInt(200, 1200, 1);
        @ConfigGroup.Pop
        public ValidatedInt graceSlowFallingDuration = new ValidatedInt(160, 1200, 1);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        public ValidatedFloat bountyCenterDamageBaseMultiplier = new ValidatedFloat(1.15F, 10.0F, 0.1F);
        public ValidatedFloat bountyCenterDamageProximityMultiplier = new ValidatedFloat(2.0F, 10.0F, 0.1F);
        public ValidatedInt bountyCenterBaseHeightSegments = new ValidatedInt(14, 50, 1);
        @ConfigGroup.Pop
        public ValidatedInt bountyCenterExtraHeightPerFrame = new ValidatedInt(2, 10, 0);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedInt chaosSunderDurationTicks = new ValidatedInt(220, 1200, 20);
        public ValidatedInt chaosSunderDurationPerStringTicks = new ValidatedInt(32, 400, 0);
        public ValidatedDouble chaosSunderAcquisitionRange = new ValidatedDouble(12.0, 24.0, 0.5);
        @ConfigGroup.Pop
        public ValidatedDouble chaosSunderAcquisitionRangePerFrame = new ValidatedDouble(1.0, 8.0, 0.0);
    }

    // ── Echo Bow ─────────────────────────────────────────────

    public static class EchoBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.9F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.8F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);

        // Shoulder Bow
        public ConfigGroup shoulderGroup = new ConfigGroup("shoulderBow");
        @ConfigGroup.Pop
        public ValidatedDouble lookTargetDistance = new ValidatedDouble(48.0, 128.0, 8.0);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedFloat painExplosionBaseHpDamage = new ValidatedFloat(0.16F, 1.0F, 0.01F);
        public ValidatedFloat painExplosionFrameHpDamage = new ValidatedFloat(0.06F, 0.5F, 0.0F);
        public ValidatedFloat painExplosionMaxDamageRatio = new ValidatedFloat(0.55F, 1.0F, 0.1F);
        public ValidatedFloat painExplosionMinDamage = new ValidatedFloat(2.0F, 20.0F, 0.1F);
        public ValidatedDouble painExplosionBaseRadius = new ValidatedDouble(2.35, 10.0, 0.5);
        @ConfigGroup.Pop
        public ValidatedDouble painExplosionStringRadiusBonus = new ValidatedDouble(0.55, 3.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedInt graceBaseShots = new ValidatedInt(20, 100, 1);
        public ValidatedInt graceShotsPerString = new ValidatedInt(5, 20, 0);
        public ValidatedFloat graceBaseSplashRadius = new ValidatedFloat(2.25F, 10.0F, 0.5F);
        @ConfigGroup.Pop
        public ValidatedFloat graceSplashRadiusPerFrame = new ValidatedFloat(0.5F, 5.0F, 0.0F);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedInt chaosBlackHoleDurationTicks = new ValidatedInt(320, 1200, 20);
        public ValidatedInt chaosBlackHoleDurationPerStringTicks = new ValidatedInt(40, 400, 0);
        public ValidatedInt chaosBlackHoleCooldownTicks = new ValidatedInt(480, 2400, 20);
        public ValidatedDouble chaosBlackHoleRadius = new ValidatedDouble(5.0, 12.0, 0.5);
        public ValidatedDouble chaosBlackHolePullStrength = new ValidatedDouble(0.09, 1.0, 0.01);
        public ValidatedDouble chaosBlackHolePullStrengthPerFrame = new ValidatedDouble(0.025, 1.0, 0.0);
        public ValidatedInt chaosBlackHoleDamageIntervalTicks = new ValidatedInt(10, 100, 1);
        @ConfigGroup.Pop
        public ValidatedFloat chaosBlackHoleDamagePerTick = new ValidatedFloat(2.0F, 20.0F, 0.1F);
    }

    // ── Cosmic Bow ─────────────────────────────────────────────

    public static class CosmicBowSection extends ConfigSection {
        // Arrow
        public ConfigGroup arrowGroup = new ConfigGroup("arrow");
        public ValidatedFloat arrowSpeedMultiplier = new ValidatedFloat(0.9F, 3.0F, 0.1F);
        public ValidatedFloat arrowDivergence = new ValidatedFloat(0.8F, 5.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedDouble baseDamage = new ValidatedDouble(2.0, 20.0, 0.1);

        // Constellation Trail
        public ConfigGroup trailGroup = new ConfigGroup("constellationTrail");
        public ValidatedInt trailDurationTicks = new ValidatedInt(25, 300, 10);
        public ValidatedInt trailLineDurationTicks = new ValidatedInt(12, 300, 5);
        public ValidatedFloat trailMaxConnectionDist = new ValidatedFloat(2.5F, 6.0F, 0.5F);
        @ConfigGroup.Pop
        public ValidatedFloat trailConnectionProbability = new ValidatedFloat(0.35F, 1.0F, 0.05F);

        // Orbit
        public ConfigGroup orbitGroup = new ConfigGroup("orbit");
        public ValidatedInt orbitDurationTicks = new ValidatedInt(180, 600, 20);
        public ValidatedInt orbitDurationBonusPerString = new ValidatedInt(20, 200, 0);
        public ValidatedDouble orbitAttackRadius = new ValidatedDouble(10.0, 32.0, 1.0);
        public ValidatedFloat orbitAttackDamage = new ValidatedFloat(4.0F, 40.0F, 0.1F);
        public ValidatedInt orbitAttackIntervalMinTicks = new ValidatedInt(18, 200, 1);
        public ValidatedInt orbitMaxJumps = new ValidatedInt(5, 20, 1);
        @ConfigGroup.Pop
        public ValidatedInt orbitAttackIntervalMaxTicks = new ValidatedInt(36, 400, 1);

        // Rune: Pain
        public ConfigGroup painGroup = new ConfigGroup("pain");
        public ValidatedInt painOrbitCooldownTicks = new ValidatedInt(1600, 12000, 20);
        public ValidatedInt painMaxTetherTargets = new ValidatedInt(6, 32, 0);
        @ConfigGroup.Pop
        public ValidatedDouble painTetherPullStrength = new ValidatedDouble(0.08, 1.0, 0.0);

        // Rune: Grace
        public ConfigGroup graceGroup = new ConfigGroup("grace");
        public ValidatedInt graceFieldDurationTicks = new ValidatedInt(160, 600, 20);
        public ValidatedInt graceFieldCooldownTicks = new ValidatedInt(400, 12000, 20);
        public ValidatedDouble graceFieldRadius = new ValidatedDouble(4.5, 12.0, 1.0);
        public ValidatedInt graceFieldBuffDurationTicks = new ValidatedInt(30, 200, 10);
        public ValidatedFloat graceFieldCocoonChance = new ValidatedFloat(0.20F, 1.0F, 0.0F);
        public ValidatedFloat graceFieldCocoonNightChanceBonus = new ValidatedFloat(0.20F, 1.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedInt graceFieldBuffAmplifier = new ValidatedInt(0, 4, 0);

        // Rune: Bounty
        public ConfigGroup bountyGroup = new ConfigGroup("bounty");
        public ValidatedInt bountyCooldownTicks = new ValidatedInt(40, 12000, 0);
        public ValidatedFloat bountyArrowSpeedMultiplier = new ValidatedFloat(0.62F, 1.0F, 0.1F);
        public ValidatedInt bountyMaxChargeTicks = new ValidatedInt(60, 400, 20);
        public ValidatedInt bountyImplodeTicks = new ValidatedInt(42, 120, 10);
        public ValidatedDouble bountyMinRadius = new ValidatedDouble(3.0, 16.0, 1.0);
        public ValidatedDouble bountyMaxRadius = new ValidatedDouble(7.0, 24.0, 1.0);
        public ValidatedFloat bountyMinDamage = new ValidatedFloat(5.0F, 80.0F, 0.0F);
        public ValidatedFloat bountyMaxDamage = new ValidatedFloat(13.0F, 120.0F, 0.0F);
        @ConfigGroup.Pop
        public ValidatedDouble bountyPullStrength = new ValidatedDouble(0.18, 1.5, 0.0);

        // Rune: Chaos
        public ConfigGroup chaosGroup = new ConfigGroup("chaos");
        public ValidatedInt chaosSunDurationTicks = new ValidatedInt(200, 1200, 20);
        public ValidatedInt chaosSunMaxDurationTicks = new ValidatedInt(1200, 2400, 20);
        public ValidatedInt chaosSunCooldownTicks = new ValidatedInt(1200, 12000, 20);
        public ValidatedDouble chaosSunRadius = new ValidatedDouble(8.0, 32.0, 1.0);
        public ValidatedInt chaosSunMaxCapturedProjectiles = new ValidatedInt(40, 400, 1);
        public ValidatedInt chaosSunFireIntervalTicks = new ValidatedInt(60, 300, 20);
        public ValidatedInt chaosSunDurationBonusPerShotTicks = new ValidatedInt(40, 400, 20);
        public ValidatedInt chaosCelestialSwiftnessDurationTicks = new ValidatedInt(140, 600, 20);
        public ValidatedInt chaosCelestialSwiftnessMaxStacks = new ValidatedInt(15, 15, 1);
        @ConfigGroup.Pop
        public ValidatedFloat chaosCelestialBowPullBonusPerStack = new ValidatedFloat(0.16F, 1.0F, 0.0F);
    }

    // ── Loot ─────────────────────────────────────────────────

    public static class LootSection extends ConfigSection {
        private static final float CHANCE_SCALE = 1000.0F;

        // Global Drops
        public ConfigGroup globalGroup = new ConfigGroup("globalDrops");
        public ValidatedFloat baseStringChance = new ValidatedFloat(20.0F, CHANCE_SCALE, 0.0F);
        public ValidatedFloat baseFrameChance = new ValidatedFloat(20.0F, CHANCE_SCALE, 0.0F);
        public ValidatedFloat baseRuneChance = new ValidatedFloat(3.0F, CHANCE_SCALE, 0.0F);
        @ConfigGroup.Pop
        public ValidatedFloat baseUniqueBowChance = new ValidatedFloat(2.0F, CHANCE_SCALE, 0.0F);

        // Biome Boosts
        public ConfigGroup biomeGroup = new ConfigGroup("biomeBoosts");
        public ValidatedFloat boostedBowChance = new ValidatedFloat(15.0F, CHANCE_SCALE, 0.0F);
        @ConfigGroup.Pop
        public ValidatedFloat boostedRuneChanceAncientCity = new ValidatedFloat(20.0F, CHANCE_SCALE, 0.0F);
    }

    // ── Upgrades ─────────────────────────────────────────────

    public static class UpgradeSection extends ConfigSection {
        public ValidatedInt maxLevelPerType = new ValidatedInt(5, 20, 1);
        public ValidatedInt maxTotalSlots = new ValidatedInt(5, 20, 1);
        public ValidatedDouble sizeMultiplierPerString = new ValidatedDouble(0.2, 1.0, 0.0);
        public ValidatedDouble damageMultiplierPerFrame = new ValidatedDouble(0.55, 2.0, 0.0);

        // Draw speeds
        public ConfigGroup drawSpeedGroup = new ConfigGroup("drawSpeeds");
        public ValidatedFloat drawSpeedVine = new ValidatedFloat(30.0F, 100.0F, 1.0F);
        public ValidatedFloat drawSpeedIce = new ValidatedFloat(40.0F, 100.0F, 1.0F);
        public ValidatedFloat drawSpeedBubble = new ValidatedFloat(20.0F, 100.0F, 1.0F);
        public ValidatedFloat drawSpeedBee = new ValidatedFloat(20.0F, 100.0F, 1.0F);
        public ValidatedFloat drawSpeedBlossom = new ValidatedFloat(20.0F, 100.0F, 1.0F);
        public ValidatedFloat drawSpeedEarth = new ValidatedFloat(40.0F, 100.0F, 1.0F);
        public ValidatedFloat drawSpeedEcho = new ValidatedFloat(30.0F, 100.0F, 1.0F);
        @ConfigGroup.Pop
        public ValidatedFloat drawSpeedCosmic = new ValidatedFloat(30.0F, 100.0F, 1.0F);
    }

    // ── General ──────────────────────────────────────────────

    public static class GeneralSection extends ConfigSection {
        public ValidatedBoolean debugMode = new ValidatedBoolean(false);
        public ValidatedBoolean modernTooltipsEnabled = new ValidatedBoolean(true);
        public ValidatedDouble rangedWeaponApiDamageMultiplier = new ValidatedDouble(1.0, 100.0, 0.0);

        // Non-player (mob) usage of unique bows
        public ValidatedBoolean enableNonPlayerBowUse = new ValidatedBoolean(true);
        public ValidatedInt nonPlayerBowCheckInterval = new ValidatedInt(60, 6000, 1);
        public ValidatedInt nonPlayerBowChance = new ValidatedInt(50, 100, 0);
        public ValidatedDouble nonPlayerBowAbilityDamageModifier = new ValidatedDouble(0.5, 100.0, 0.0);
        public ValidatedDouble nonPlayerBowProjectileDamageModifier = new ValidatedDouble(0.5, 100.0, 0.0);
        public ValidatedDouble nonPlayerBowDamageToPlayersModifier = new ValidatedDouble(0.5, 100.0, 0.0);
    }
}
