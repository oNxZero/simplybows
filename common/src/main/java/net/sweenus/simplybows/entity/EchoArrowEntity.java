package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.world.EchoChaosBlackHoleManager;
import net.sweenus.simplybows.world.EchoShoulderBowManager;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EchoArrowEntity extends ArrowEntity {

    private static float painExplosionBaseMaxHealthDamage() { return SimplyBowsConfig.INSTANCE.echoBow.painExplosionBaseHpDamage.get(); }
    private static float painExplosionFrameMaxHealthDamage() { return SimplyBowsConfig.INSTANCE.echoBow.painExplosionFrameHpDamage.get(); }
    private static float painExplosionMaxDamageRatio() { return SimplyBowsConfig.INSTANCE.echoBow.painExplosionMaxDamageRatio.get(); }
    private static float painExplosionMinDamage() { return SimplyBowsConfig.INSTANCE.echoBow.painExplosionMinDamage.get(); }
    private static double painExplosionBaseRadius() { return SimplyBowsConfig.INSTANCE.echoBow.painExplosionBaseRadius.get(); }
    private static double painExplosionStringRadiusBonus() { return SimplyBowsConfig.INSTANCE.echoBow.painExplosionStringRadiusBonus.get(); }
    private static float defaultGraceSplashRadius() { return SimplyBowsConfig.INSTANCE.echoBow.graceBaseSplashRadius.get(); }
    private final BowUpgradeData upgrades;
    private List<StatusEffectInstance> gracePotionEffects = List.of();
    private boolean graceSupportArrow;
    private float graceSplashRadius = defaultGraceSplashRadius();
    private boolean chaosBlackHoleOnImpact;

    public EchoArrowEntity(EntityType<? extends EchoArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public EchoArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.ECHO_ARROW.get(), world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.upgrades = BowUpgradeData.from(weaponStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
            if (!this.gracePotionEffects.isEmpty()) {
                serverWorld.spawnParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    public void setGracePotionPayload(List<StatusEffectInstance> effects, boolean supportMode, float splashRadius) {
        if (effects == null || effects.isEmpty()) {
            this.gracePotionEffects = List.of();
            this.graceSupportArrow = false;
            this.graceSplashRadius = defaultGraceSplashRadius();
            return;
        }
        java.util.ArrayList<StatusEffectInstance> copied = new java.util.ArrayList<>();
        for (StatusEffectInstance effect : effects) {
            if (effect != null) {
                copied.add(new StatusEffectInstance(effect));
            }
        }
        this.gracePotionEffects = copied;
        this.graceSupportArrow = supportMode;
        this.graceSplashRadius = Math.max(1.0F, splashRadius);
    }

    public void setChaosBlackHoleOnImpact(boolean chaosBlackHoleOnImpact) {
        this.chaosBlackHoleOnImpact = chaosBlackHoleOnImpact;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (!this.gracePotionEffects.isEmpty() && this.graceSupportArrow) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                applyGracePotionSplash(serverWorld, entityHitResult.getPos());
            }
            this.discard();
            return;
        }

        LivingEntity hitLiving = entityHitResult.getEntity() instanceof LivingEntity living ? living : null;
        boolean wasAliveBeforeHit = hitLiving != null && hitLiving.isAlive();

        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (hitLiving != null) {
                if (this.getOwner() instanceof LivingEntity livingOwner && this.upgrades.runeEtching() == RuneEtching.PAIN) {
                    EchoShoulderBowManager.setFocusedTarget(livingOwner, hitLiving);
                }
                if (this.upgrades.runeEtching() == RuneEtching.PAIN && wasAliveBeforeHit && !hitLiving.isAlive()) {
                    triggerPainArcaneChain(serverWorld, hitLiving);
                }
            }
            triggerChaosBlackHole(serverWorld, entityHitResult.getPos());
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, entityHitResult.getPos().x, entityHitResult.getPos().y + 0.1, entityHitResult.getPos().z, 10, 0.15, 0.15, 0.15, 0.0);
            serverWorld.playSound(null, entityHitResult.getPos().x, entityHitResult.getPos().y, entityHitResult.getPos().z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5F, 1.25F);
            if (!this.gracePotionEffects.isEmpty()) {
                applyGracePotionSplash(serverWorld, entityHitResult.getPos());
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, blockHitResult.getPos().x, blockHitResult.getPos().y + 0.1, blockHitResult.getPos().z, 8, 0.12, 0.12, 0.12, 0.0);
            if (!this.gracePotionEffects.isEmpty()) {
                applyGracePotionSplash(serverWorld, blockHitResult.getPos());
            }
            triggerChaosBlackHole(serverWorld, blockHitResult.getPos());
        }
    }

    private void triggerChaosBlackHole(ServerWorld world, Vec3d pos) {
        if (!this.chaosBlackHoleOnImpact || world == null || pos == null) {
            return;
        }
        EchoChaosBlackHoleManager.spawnAtImpact(
                world,
                pos,
                this.getOwner() != null ? this.getOwner().getUuid() : null,
                this.upgrades.stringLevel(),
                this.upgrades.frameLevel()
        );
        this.chaosBlackHoleOnImpact = false;
    }

    @Override
    public ItemStack asItemStack() {
        return new ItemStack(Items.ARROW);
    }

    private void triggerPainArcaneChain(ServerWorld world, LivingEntity initialVictim) {
        if (initialVictim == null) {
            return;
        }

        ArrayDeque<LivingEntity> queue = new ArrayDeque<>();
        Set<java.util.UUID> explodedVictims = new HashSet<>();
        queue.add(initialVictim);

        while (!queue.isEmpty()) {
            LivingEntity sourceVictim = queue.removeFirst();
            if (sourceVictim == null || !explodedVictims.add(sourceVictim.getUuid())) {
                continue;
            }

            double radius = painExplosionBaseRadius() + this.upgrades.stringLevel() * painExplosionStringRadiusBonus();
            float healthRatio = painExplosionBaseMaxHealthDamage() + this.upgrades.frameLevel() * painExplosionFrameMaxHealthDamage();
            float clampedRatio = Math.min(painExplosionMaxDamageRatio(), healthRatio);
            float damage = Math.max(painExplosionMinDamage(), sourceVictim.getMaxHealth() * clampedRatio);
            Vec3d center = sourceVictim.getPos().add(0.0, sourceVictim.getStandingEyeHeight() * 0.5, 0.0);

            spawnArcaneExplosionEffects(world, center, radius);

            Box damageBox = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
            for (LivingEntity nearby : world.getEntitiesByClass(LivingEntity.class, damageBox, entity ->
                    entity.isAlive() && entity != sourceVictim && sourceVictim.squaredDistanceTo(entity) <= radius * radius)) {
                boolean wasAlive = nearby.isAlive();
                if (!CombatTargeting.applyDamage(world, this.getOwner(), nearby, damage, true, false)) {
                    continue;
                }
                if (wasAlive && !nearby.isAlive()) {
                    queue.addLast(nearby);
                }
            }
        }
    }

    private static void spawnArcaneExplosionEffects(ServerWorld world, Vec3d center, double radius) {
        int enchantCount = (int) Math.max(18, 26 + radius * 7.0);
        int witchCount = (int) Math.max(10, 12 + radius * 4.0);
        int dragonCount = (int) Math.max(12, 14 + radius * 5.0);
        spawnArcaneDomeShell(world, center, radius);
        spawnArcaneGroundRing(world, center, radius);

        world.spawnParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, enchantCount, radius * 0.45, radius * 0.28, radius * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.WITCH, center.x, center.y, center.z, witchCount, radius * 0.38, radius * 0.24, radius * 0.38, 0.02);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, dragonCount, radius * 0.35, radius * 0.2, radius * 0.35, 0.01);
        world.spawnParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 22, radius * 0.4, radius * 0.3, radius * 0.4, 0.2);

        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.7F, 0.65F + world.random.nextFloat() * 0.1F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.45F, 1.2F + world.random.nextFloat() * 0.15F, world.random.nextLong());
    }

    private static void spawnArcaneDomeShell(ServerWorld world, Vec3d center, double radius) {
        int layers = 8;
        int pointsPerRing = 24;
        for (int layer = 0; layer <= layers; layer++) {
            double t = (double) layer / (double) layers;
            double theta = t * (Math.PI * 0.5); // hemisphere
            double y = center.y + Math.sin(theta) * radius;
            double ringRadius = Math.cos(theta) * radius;
            for (int i = 0; i < pointsPerRing; i++) {
                double angle = (Math.PI * 2.0 * i) / pointsPerRing;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
                if ((i & 1) == 0) {
                    world.spawnParticles(ParticleTypes.WITCH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static void spawnArcaneGroundRing(ServerWorld world, Vec3d center, double radius) {
        int points = 36;
        double y = center.y - radius * 0.15;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.0, 0.0, 0.0, 0.02);
            if ((i % 3) == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.02, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void applyGracePotionSplash(ServerWorld world, Vec3d impactPos) {
        if (world == null || impactPos == null || this.gracePotionEffects.isEmpty()) {
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        double radius = Math.max(1.0, this.graceSplashRadius);
        Box box = Box.of(impactPos, radius * 2.0, radius * 2.0, radius * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, entity ->
                entity.isAlive() && impactPos.squaredDistanceTo(entity.getPos()) <= radius * radius)) {
            if (owner != null) {
                boolean friendly = CombatTargeting.isFriendlyTo(target, owner);
                if (this.graceSupportArrow) {
                    if (!friendly) {
                        continue;
                    }
                } else if (!CombatTargeting.checkFriendlyFire(target, owner)) {
                    continue;
                }
            }

            double dist = Math.sqrt(impactPos.squaredDistanceTo(target.getPos()));
            double proximity = MathHelper.clamp(1.0 - (dist / radius), 0.0, 1.0);
            if (proximity <= 0.0) {
                continue;
            }
            for (StatusEffectInstance effect : this.gracePotionEffects) {
                int scaledDuration = Math.max(1, (int) (effect.getDuration() * (0.25 + 0.75 * proximity)));
                target.addStatusEffect(new StatusEffectInstance(
                        effect.getEffectType(),
                        scaledDuration,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.shouldShowParticles(),
                        effect.shouldShowIcon()
                ), this.getOwner());
            }
        }

        world.spawnParticles(ParticleTypes.WITCH, impactPos.x, impactPos.y + 0.1, impactPos.z, 16, radius * 0.25, 0.18, radius * 0.25, 0.01);
        world.spawnParticles(ParticleTypes.INSTANT_EFFECT, impactPos.x, impactPos.y + 0.1, impactPos.z, 12, radius * 0.2, 0.15, radius * 0.2, 0.0);
        world.spawnParticles(ParticleTypes.INSTANT_EFFECT, impactPos.x, impactPos.y + 0.1, impactPos.z, 8, radius * 0.18, 0.12, radius * 0.18, 0.0);
        world.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 0.7F, 1.05F + world.random.nextFloat() * 0.1F);
    }

    private static ItemStack sanitizeArrowStack(ItemStack arrowStack) {
        if (arrowStack == null || arrowStack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }
        ItemStack copy = arrowStack.copy();
        copy.setCount(1);
        return copy;
    }
}
