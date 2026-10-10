package net.sweenus.simplybows.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.util.*;
import java.util.*;

/** A single persistent cast; block geometry is rendered client-side, never placed in the world. */
public class RuneEffectEntity extends Entity {
    private static final TrackedData<Integer> KIND = DataTracker.registerData(RuneEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ELAPSED = DataTracker.registerData(RuneEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STRINGS = DataTracker.registerData(RuneEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(RuneEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT = DataTracker.registerData(RuneEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(RuneEffectEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public int duration() { return dataTracker.get(LIFETIME)>0 ? dataTracker.get(LIFETIME) : RuneEffectRules.duration(kind(),strings()); }
    public static void spawnSupport(ServerWorld world,int kind,Vec3d pos,LivingEntity owner,double radius,int lifetime) {
        RuneEffectEntity visual=new RuneEffectEntity(world,kind,pos,owner,null,0,0);
        visual.dataTracker.set(WIDTH,(float)radius); visual.dataTracker.set(LIFETIME,lifetime); world.spawnEntity(visual);
    }
    public static void spawnProjectile(ServerWorld world,int kind,Vec3d pos,LivingEntity owner,LivingEntity target,float damage,Vec3d velocity) {
        RuneEffectEntity projectile=new RuneEffectEntity(world,kind,pos,owner,target,0,0);
        projectile.payloadDamage=damage; projectile.setVelocity(velocity);
        if(target!=null) projectile.lastTargetPosition=target.getPos(); world.spawnEntity(projectile);
    }
    private UUID ownerId, targetId;
    private int frames;
    private float payloadDamage;
    private Vec3d lastTargetPosition;
    private int missingTargetTicks;
    private static final TrackedData<Integer> FINISH_AT = DataTracker.registerData(RuneEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private final Map<Long, Double> groundOffsets = new HashMap<>();
    public double groundOffset(double localX, double localZ) {
        BlockPos origin = BlockPos.ofFloored(getX()+localX, getY()+1, getZ()+localZ);
        return groundOffsets.computeIfAbsent(origin.asLong(), key -> {
            for (int down=0;down<=6;down++) {
                BlockPos pos = origin.down(down);
                var shape = getWorld().getBlockState(pos).getCollisionShape(getWorld(),pos);
                if (!shape.isEmpty()) return pos.getY()+shape.getMax(Direction.Axis.Y)-getY();
            }
            return 0.0;
        });
    }
    public int finishAt() { return dataTracker.get(FINISH_AT); }
    private void finish() {
        if (finishAt() < 0) {
            dataTracker.set(FINISH_AT, elapsed()+RuneEffectRules.FINISH_TICKS);
            if (getWorld() instanceof ServerWorld world) net.sweenus.simplybows.world.BowEffectSounds.end(world,getPos(),soundTheme());
        }
    }
    private net.sweenus.simplybows.world.BowEffectSounds.Theme soundTheme() {
        return switch (kind()) {
            case RuneEffectRules.SWARM -> net.sweenus.simplybows.world.BowEffectSounds.Theme.BEE;
            case RuneEffectRules.VORTEX, RuneEffectRules.WATER_DROP -> net.sweenus.simplybows.world.BowEffectSounds.Theme.WATER;
            case RuneEffectRules.LOTUS, RuneEffectRules.PETAL_BLADE, 9 -> net.sweenus.simplybows.world.BowEffectSounds.Theme.BLOSSOM;
            case 8 -> net.sweenus.simplybows.world.BowEffectSounds.Theme.SUPPORT_BLOSSOM;
            case 7 -> net.sweenus.simplybows.world.BowEffectSounds.Theme.SUPPORT_FROST;
            case 10 -> net.sweenus.simplybows.world.BowEffectSounds.Theme.FROST;
            default -> net.sweenus.simplybows.world.BowEffectSounds.Theme.EARTH;
        };
    }
    private final Set<UUID> waveHits = new HashSet<>();
    public RuneEffectEntity(EntityType<? extends RuneEffectEntity> type, World world) {
        super(type, world); noClip = true; setNoGravity(true);
    }
    public RuneEffectEntity(ServerWorld world, int kind, Vec3d position, LivingEntity owner, LivingEntity target, int strings, int frames) {
        this(EntityRegistry.RUNE_EFFECT.get(), world);
        setPosition(position);
        Vec3d aim = position.subtract(owner.getPos());
        setYaw((float)Math.toDegrees(Math.atan2(-aim.x,aim.z)));
        dataTracker.set(KIND, kind); dataTracker.set(STRINGS, RuneEffectRules.level(strings));
        this.frames = RuneEffectRules.level(frames); ownerId = owner.getUuid();
        if (target != null) { targetId = target.getUuid(); dataTracker.set(WIDTH, Math.max(0.9F, target.getWidth()));
            dataTracker.set(HEIGHT, Math.max(1.0F, target.getHeight())); }
    }
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LIFETIME,0); builder.add(FINISH_AT, -1); builder.add(KIND, 0); builder.add(ELAPSED, 0); builder.add(STRINGS, 0); builder.add(WIDTH, 1F); builder.add(HEIGHT, 2F);
    }
    public int kind() { return dataTracker.get(KIND); }
    public int elapsed() { return dataTracker.get(ELAPSED); }
    public int strings() { return dataTracker.get(STRINGS); }
    public float targetWidth() { return dataTracker.get(WIDTH); }
    public float targetHeight() { return dataTracker.get(HEIGHT); }
    public boolean isAttackable() { return false; }
    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) {
            if (kind() == RuneEffectRules.VORTEX && age%2 == 0) {
                float fade=RuneEffectRules.animationScale(elapsed(),finishAt());
                double radius=RuneEffectRules.radius(kind(),strings());
                if (fade>0) for(int puff=0;puff<10;puff++) {
                    double angle=random.nextDouble()*Math.PI*2;
                    double distance=Math.sqrt(random.nextDouble())*radius*fade;
                    getWorld().addParticle(ParticleTypes.CLOUD,getX()+Math.cos(angle)*distance,
                            getY()+6.3+random.nextDouble()*1.8,getZ()+Math.sin(angle)*distance,0,.006,0);
                    if(puff%3==0) getWorld().addParticle(ParticleTypes.FALLING_WATER,getX()+Math.cos(angle)*distance,
                            getY()+6.3,getZ()+Math.sin(angle)*distance,0,-.03,0);
                }
            }
            if ((kind()==7 || kind()==8) && age%3==0) {
                double fade=RuneEffectRules.animationScale(elapsed(),finishAt());
                for(int i=0;i<12;i++) {
                    double angle=age*.025+i*Math.PI/6;
                    double ring=targetWidth() * (i%2==0 ? .9 : .55) * fade;
                    getWorld().addParticle(kind()==7 ? ParticleTypes.SNOWFLAKE : ParticleTypes.CHERRY_LEAVES,
                        getX()+Math.cos(angle)*ring,getY()+.15+(.1*Math.sin(angle*3+age*.08)),getZ()+Math.sin(angle)*ring,
                        -Math.sin(angle)*.015,.01,Math.cos(angle)*.015);
                    if(i%3==0) getWorld().addParticle(kind()==7 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER,
                        getX()+Math.cos(angle)*ring*.6,getY()+.3,getZ()+Math.sin(angle)*ring*.6,0,.035,0);
                }
            }
            if(kind()==9 && age%2==0) getWorld().addParticle(ParticleTypes.CHERRY_LEAVES,getX(),getY(),getZ(),0,0,0);
            if(kind()==10) getWorld().addParticle(ParticleTypes.SNOWFLAKE,getX(),getY(),getZ(),0,0,0);
            if(kind()==RuneEffectRules.LOTUS && age%3==0 && finishAt()<0) {
                double a=age*.4;
                getWorld().addParticle(ParticleTypes.CHERRY_LEAVES,getX()+Math.cos(a)*1.4,getY()+1.8,getZ()+Math.sin(a)*1.4,0,.04,0);
            }
            return;
        }
        int t = elapsed()+1;
        dataTracker.set(ELAPSED,t);
        if (finishAt() >= 0) {
            if (kind()==RuneEffectRules.SWARM && targetId != null && world.getEntity(targetId) instanceof LivingEntity trailingTarget
                    && trailingTarget.isAlive() && trailingTarget.getPos().squaredDistanceTo(getPos()) <= 256) setPosition(trailingTarget.getPos());
            if (t >= finishAt()) discard();
            return;
        }
        Entity ownerEntity = ownerId == null ? null : world.getEntity(ownerId);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) { finish(); return; }
        LivingEntity target = targetId != null && world.getEntity(targetId) instanceof LivingEntity living ? living : null;
        if (kind() == RuneEffectRules.WATER_DROP || kind() == RuneEffectRules.PETAL_BLADE || kind()==9 || kind()==10) {
            tickSeekingProjectile(world,owner,target,t); return;
        }
        if (targetId != null) {
            if (target == null) { if (++missingTargetTicks >= 20 || t >= RuneEffectRules.duration(kind(),strings())) finish(); return; }
            if (!target.isAlive() || !CombatTargeting.checkFriendlyFire(target,owner)) { finish(); return; }
            missingTargetTicks=0;
            if (target.getPos().squaredDistanceTo(getPos()) > 256) { finish(); return; }
            setPosition(target.getPos());
        }
        int duration = duration();
        float damage = RuneEffectRules.damage(kind(), frames);
        if (t == 1 && kind() != RuneEffectRules.STAR) net.sweenus.simplybows.world.BowEffectSounds.start(world,getPos(),soundTheme());
        if ((t < duration && t%40 == 0) || (kind() == RuneEffectRules.STONE && t == 18))
            net.sweenus.simplybows.world.BowEffectSounds.ambient(world,getPos(),soundTheme());
        if (kind() == RuneEffectRules.SWARM && RuneEffectRules.pulseTick(kind(), t, strings())) hit(world, owner, target, damage);
        if (kind() == RuneEffectRules.VORTEX && RuneEffectRules.pulseTick(kind(),t,strings())) {
            launchVolley(world,owner,RuneEffectRules.WATER_DROP,damage,false,3);
        }
        if (kind()==RuneEffectRules.VORTEX && t==duration)
            launchVolley(world,owner,RuneEffectRules.WATER_DROP,RuneEffectRules.finisher(frames),false,3);
        if(kind()==RuneEffectRules.LOTUS && t>=16 && t<=40) {
            double radius=RuneEffectRules.radius(kind(),strings());
            for(LivingEntity enemy:world.getEntitiesByClass(LivingEntity.class,getBoundingBox().expand(radius,3,radius),
                    e -> CombatTargeting.isOffensiveTargetCandidate(e,owner) && CombatTargeting.checkFriendlyFire(e,owner))) {
                Vec3d inward=getPos().subtract(enemy.getPos());
                if(inward.horizontalLength()>radius) continue;
                if(t<40 && inward.horizontalLength()>.8) {
                    Vec3d pull=new Vec3d(inward.x,0,inward.z).normalize().multiply(.09);
                    enemy.addVelocity(pull); enemy.velocityDirty=true;
                    if(enemy instanceof net.minecraft.server.network.ServerPlayerEntity player) NetworkCompat.sendVelocityUpdate(player);
                }
            }
            if(t==40) launchVolley(world,owner,RuneEffectRules.PETAL_BLADE,damage,true,3);
        }
        if (kind() == RuneEffectRules.STONE) {
            double radius = RuneEffectRules.radius(kind(), strings());
            for (int pair=0; pair<RuneEffectRules.STONE_PAIRS; pair++) {
                if(t==RuneEffectRules.stoneStrikeTick(pair)-18)
                    net.sweenus.simplybows.world.BowEffectSounds.eruption(world,getPos().add(Vec3d.fromPolar(0,getYaw()).multiply(RuneEffectRules.stonePairZ(pair,radius))),false);
                if (t != RuneEffectRules.stoneStrikeTick(pair)) continue;
                double z = RuneEffectRules.stonePairZ(pair,radius);
                Vec3d forward = Vec3d.fromPolar(0,getYaw());
                Vec3d strike = getPos().add(forward.multiply(z));
                net.sweenus.simplybows.world.BowEffectSounds.crush(world,strike);
                world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK,
                        net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()), strike.x,strike.y+1,strike.z,36,.85,.9,.45,.09);
                world.spawnParticles(ParticleTypes.POOF,strike.x,strike.y+.8,strike.z,8,.6,.7,.3,.025);
                for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class,
                        net.minecraft.util.math.Box.of(strike.add(0,1,0),radius*2+2,3.5,radius*2+2),
                        e -> (CombatTargeting.isOffensiveTargetCandidate(e,owner) || e instanceof net.minecraft.entity.mob.Monster)
                                && CombatTargeting.checkFriendlyFire(e,owner))) {
                    Vec3d relative=victim.getPos().subtract(strike);
                    double across=relative.x*forward.z-relative.z*forward.x;
                    double along=relative.x*forward.x+relative.z*forward.z;
                    double padding=victim.getWidth()*.5;
                    if (Math.abs(across)>radius+padding || Math.abs(along)>radius*.225+padding) continue;
                    // The formation has one damage budget per enemy, even if they cross several pairs.
                    if (waveHits.add(victim.getUuid()) && hit(world,owner,victim,damage)) net.sweenus.simplybows.world.StoneRootManager.root(world,victim);
                }
            }
        }
        if (kind() == RuneEffectRules.STAR && t <= duration) {
            int waveAge=RuneEffectRules.starWaveAge(t);
            if (waveAge == 0) waveHits.clear();
            if(waveAge%3==2) net.sweenus.simplybows.world.BowEffectSounds.stoneLayer(world,getPos(),waveAge>=15,(waveAge%15)/3);

            double radius=RuneEffectRules.radius(kind(),strings());
            double progress=RuneEffectRules.starProgress(waveAge+1);
            for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(radius+1,6,radius+1),
                    e -> (CombatTargeting.isOffensiveTargetCandidate(e,owner) || e instanceof net.minecraft.entity.mob.Monster) && CombatTargeting.checkFriendlyFire(e,owner))) {
                Vec3d rel=victim.getPos().subtract(getPos());
                double groundY=getY()+groundOffset(rel.x,rel.z);
                if (victim.getBoundingBox().maxY < groundY || victim.getY() > groundY+1.8) continue;
                double padding=victim.getWidth()/2;
                if (progress > 0 && RuneEffectRules.insideStar(rel.x,rel.z,radius*progress,padding) && waveHits.add(victim.getUuid())) {
                    if(hit(world,owner,victim,damage)) {
                        victim.setVelocity(victim.getVelocity().x,.55,victim.getVelocity().z); victim.setOnGround(false); victim.velocityDirty=true;
                        if(victim instanceof net.minecraft.server.network.ServerPlayerEntity player) NetworkCompat.sendVelocityUpdate(player);
                    }
                }
            }
        }
        if (t == duration && kind() == RuneEffectRules.VORTEX) {
            world.spawnParticles(kind() == RuneEffectRules.LOTUS ? ParticleTypes.CHERRY_LEAVES : kind() == RuneEffectRules.VORTEX ? ParticleTypes.SPLASH : ParticleTypes.CRIT,
                    getX(), getY() + 1, getZ(), 24, 0.7, 0.7, 0.7, 0.08);
            if (kind() == RuneEffectRules.STONE) net.sweenus.simplybows.world.BowEffectSounds.crush(world,getPos());
            else if (kind() == RuneEffectRules.VORTEX) net.sweenus.simplybows.world.BowEffectSounds.splash(world,getPos());
            else net.sweenus.simplybows.world.BowEffectSounds.hit(world,getPos(),soundTheme());
        }
        if (t >= duration) finish();
    }
    private void launchVolley(ServerWorld world, LivingEntity owner, int projectileKind, float damage, boolean distinct, int maximum) {
        double radius=RuneEffectRules.radius(kind(),strings());
        if(projectileKind==RuneEffectRules.WATER_DROP) {
            List<LivingEntity> rainTargets=world.getEntitiesByClass(LivingEntity.class,Box.of(getPos().add(0,2,0),radius*2,8,radius*2),
                    e -> e.isAlive() && CombatTargeting.isOffensiveTargetCandidate(e,owner) && CombatTargeting.checkFriendlyFire(e,owner)
                            && e.getPos().subtract(getPos()).horizontalLength()<=radius);
            for(int drop=0;drop<maximum;drop++) {
                double angle=elapsed()*.29+drop*Math.PI*2/maximum+(random.nextDouble()-.5)*.5;
                double spread=radius*(.3+random.nextDouble()*.5);
                Vec3d start=getPos().add(Math.cos(angle)*spread,6.5,Math.sin(angle)*spread);
                if(!rainTargets.isEmpty()) {
                    LivingEntity selected=rainTargets.get(drop%rainTargets.size());
                    // Spread crystals around victims while keeping them inside the splash radius.
                    double offset=.8+random.nextDouble()*.7;
                    Vec3d landing=selected.getPos().subtract(getPos()).multiply(1,0,1)
                            .add(Math.cos(angle)*offset,0,Math.sin(angle)*offset);
                    if(landing.horizontalLength()>radius) landing=landing.normalize().multiply(radius);
                    start=getPos().add(landing.x,6.5,landing.z);
                }
                RuneEffectEntity crystal=new RuneEffectEntity(world,projectileKind,start,owner,null,strings(),frames);
                crystal.payloadDamage=damage/maximum; world.spawnEntity(crystal);
            }
            return;
        }
        List<LivingEntity> targets=new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                Box.of(getPos().add(0,2,0),radius*2,8,radius*2),
                e -> e.isAlive() && (CombatTargeting.isOffensiveTargetCandidate(e,owner) || e instanceof net.minecraft.entity.mob.Monster)
                        && CombatTargeting.checkFriendlyFire(e,owner) && (!distinct || !waveHits.contains(e.getUuid()))
                        && e.getPos().subtract(getPos()).horizontalLength()<=radius));
        targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(getPos())));
        if(targets.isEmpty() && projectileKind==RuneEffectRules.PETAL_BLADE) {
            double angle=((elapsed()-24)/12)*Math.PI*2/3+elapsed()*.17;
            Vec3d start=getPos().add(Math.cos(angle)*1.25,1.7,Math.sin(angle)*1.25);
            RuneEffectEntity petal=new RuneEffectEntity(world,projectileKind,start,owner,null,strings(),frames);
            petal.setVelocity(Math.cos(angle)*.4,.08,Math.sin(angle)*.4);
            world.spawnEntity(petal);
            net.sweenus.simplybows.world.BowEffectSounds.hit(world,start,net.sweenus.simplybows.world.BowEffectSounds.Theme.BLOSSOM);
        }
        for(int i=0;i<Math.min(maximum,targets.size());i++) {
            LivingEntity victim=targets.get(i);
            if(distinct) waveHits.add(victim.getUuid());
            double angle=projectileKind==RuneEffectRules.PETAL_BLADE ? ((elapsed()-24)/12)*Math.PI*2/3+elapsed()*.17+i*Math.PI*2/3 : elapsed()*.23+i*2.399963;
            Vec3d start=projectileKind==RuneEffectRules.WATER_DROP
                    ? getPos().add(Math.cos(angle)*radius*.5,4.5,Math.sin(angle)*radius*.5)
                    : getPos().add(Math.cos(angle)*1.25,1.7,Math.sin(angle)*1.25);
            RuneEffectEntity projectile=new RuneEffectEntity(world,projectileKind,start,owner,victim,strings(),frames);
            projectile.payloadDamage=damage; projectile.lastTargetPosition=victim.getPos();
            if(projectileKind==RuneEffectRules.PETAL_BLADE) projectile.setVelocity(Math.cos(angle)*.4,.08,Math.sin(angle)*.4);
            world.spawnEntity(projectile);
            if(projectileKind==RuneEffectRules.PETAL_BLADE) {
                net.sweenus.simplybows.world.BowEffectSounds.hit(world,start,net.sweenus.simplybows.world.BowEffectSounds.Theme.BLOSSOM);
                world.spawnParticles(ParticleTypes.CHERRY_LEAVES,start.x,start.y,start.z,8,.3,.25,.3,.04);
            }
        }
    }
    private void tickSeekingProjectile(ServerWorld world, LivingEntity owner, LivingEntity target, int t) {
        if(kind()==10) {
            Vec3d next=getPos().add(getVelocity());
            if (net.sweenus.simplybows.world.EarthSpikeFieldManager.blockProjectileImpact(world,this,next) || net.sweenus.simplybows.world.IceChaosWallManager.blockFormationProjectile(world,this,next)) return;
            var terrain=world.raycast(new net.minecraft.world.RaycastContext(getPos(),next,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,this));
            if(t>20 || terrain.getType()!=net.minecraft.util.hit.HitResult.Type.MISS) { finish(); return; }
            for(LivingEntity victim:world.getEntitiesByClass(LivingEntity.class,new Box(getPos(),next).expand(.3),
                    e -> e.isAlive() && CombatTargeting.isOffensiveTargetCandidate(e,owner) && CombatTargeting.checkFriendlyFire(e,owner))) {
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,100,0),owner);
                world.spawnParticles(ParticleTypes.SNOWFLAKE,victim.getX(),victim.getBodyY(.5),victim.getZ(),12,.3,.4,.3,.02);
                finish(); return;
            }
            setPosition(next); return;
        }
        if(kind()==RuneEffectRules.WATER_DROP) {
            Vec3d next=getPos().add(0,-Math.min(1,.2+t*.06),0);
            if (net.sweenus.simplybows.world.EarthSpikeFieldManager.blockProjectileImpact(world,this,next) || net.sweenus.simplybows.world.IceChaosWallManager.blockFormationProjectile(world,this,next)) return;
            var collision=world.raycast(new net.minecraft.world.RaycastContext(getPos(),next,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,this));
            if(collision.getType()!=net.minecraft.util.hit.HitResult.Type.MISS) {
                Vec3d splash=collision.getPos(); setPosition(splash);
                for(LivingEntity enemy:world.getEntitiesByClass(LivingEntity.class,Box.of(splash.add(0,.7,0),4,3,4),
                        e -> CombatTargeting.isOffensiveTargetCandidate(e,owner) && CombatTargeting.checkFriendlyFire(e,owner)))
                    if(enemy.getPos().subtract(splash).horizontalLength()<=2 && hit(world,owner,enemy,payloadDamage))
                        enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,40,0),owner);
                world.spawnParticles(ParticleTypes.SPLASH,splash.x,splash.y+.1,splash.z,24,.9,.15,.9,.08);
                for(int ripple=0;ripple<24;ripple++) {
                    double angle=ripple*Math.PI/12;
                    world.spawnParticles(ParticleTypes.SPLASH,splash.x+Math.cos(angle)*1.6,splash.y+.12,
                            splash.z+Math.sin(angle)*1.6,1,.04,.03,.04,.025);
                }
                net.sweenus.simplybows.world.BowEffectSounds.splash(world,splash);
                finish();
            } else if(t>=30) finish(); else setPosition(next);
            return;
        }
        if(targetId==null && kind()==RuneEffectRules.PETAL_BLADE) {
            Vec3d next=getPos().add(getVelocity());
            if (net.sweenus.simplybows.world.EarthSpikeFieldManager.blockProjectileImpact(world,this,next) || net.sweenus.simplybows.world.IceChaosWallManager.blockFormationProjectile(world,this,next)) return;
            var collision=world.raycast(new net.minecraft.world.RaycastContext(getPos(),next,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,this));
            if(t>12 || collision.getType()!=net.minecraft.util.hit.HitResult.Type.MISS) finish();
            else setPosition(next);
            return;
        }
        if (target==null || !target.isAlive() || !CombatTargeting.checkFriendlyFire(target,owner) || t>30
                || (lastTargetPosition!=null && lastTargetPosition.squaredDistanceTo(target.getPos())>16)) { finish(); return; }
        lastTargetPosition=target.getPos();
        Vec3d aim=new Vec3d(target.getX(),target.getBodyY(.5),target.getZ());
        Vec3d toward=aim.subtract(getPos());
        Vec3d step=(kind()==RuneEffectRules.PETAL_BLADE && t<=6 && getVelocity().lengthSquared()>.01)
                ? getVelocity() : toward.normalize().multiply(Math.min(.6,toward.length()));
        Vec3d next=getPos().add(step);
            if (net.sweenus.simplybows.world.EarthSpikeFieldManager.blockProjectileImpact(world,this,next) || net.sweenus.simplybows.world.IceChaosWallManager.blockFormationProjectile(world,this,next)) return;
        var blocked=world.raycast(new net.minecraft.world.RaycastContext(getPos(),next,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,this));
        if(blocked.getType()!=net.minecraft.util.hit.HitResult.Type.MISS) { finish(); return; }
        setPosition(next); setVelocity(step);
        if(target.getBoundingBox().expand(.25).contains(next) || toward.length()<.65) {
            boolean damaged=hit(world,owner,target,payloadDamage);
            if(kind()==RuneEffectRules.WATER_DROP) {
                if(damaged) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,25,0));
                world.spawnParticles(ParticleTypes.SPLASH,next.x,next.y,next.z,10,.2,.2,.2,.05);
                net.sweenus.simplybows.world.BowEffectSounds.hit(world,next,net.sweenus.simplybows.world.BowEffectSounds.Theme.WATER);
            } else {
                world.spawnParticles(ParticleTypes.CHERRY_LEAVES,next.x,next.y,next.z,10,.3,.3,.3,.02);
                net.sweenus.simplybows.world.BowEffectSounds.hit(world,next,net.sweenus.simplybows.world.BowEffectSounds.Theme.BLOSSOM);
            }
            finish();
        }
    }
    private boolean hit(ServerWorld world, LivingEntity owner, LivingEntity target, float damage) {
        if (target != null && CombatTargeting.applyAbilityDamage(world, owner, target, damage, true, false,
                kind()==RuneEffectRules.WATER_DROP ? 1F/3F : kind()==9 ? .5F : 1F,kind()==9 || kind()==RuneEffectRules.WATER_DROP)) {
            if (kind() == RuneEffectRules.SWARM || kind() == RuneEffectRules.VORTEX)
                net.sweenus.simplybows.world.BowEffectSounds.hit(world,target.getPos(),soundTheme());
            if (kind() == RuneEffectRules.SWARM) world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(.5), target.getZ(), 3, .15, .2, .15, .02);
            if (kind() == RuneEffectRules.VORTEX) world.spawnParticles(ParticleTypes.BUBBLE, target.getX(), target.getBodyY(.5), target.getZ(), 5, .3, .3, .3, .02);
            return true;
        }
        return false;
    }
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("VisualLifetime",dataTracker.get(LIFETIME)); nbt.putFloat("PayloadDamage",payloadDamage);
        if (lastTargetPosition != null) { nbt.putDouble("LastTargetX",lastTargetPosition.x); nbt.putDouble("LastTargetY",lastTargetPosition.y); nbt.putDouble("LastTargetZ",lastTargetPosition.z); }
        nbt.putInt("FinishAt", finishAt()); nbt.putInt("Kind", kind()); nbt.putInt("Elapsed", elapsed()); nbt.putInt("Strings", strings()); nbt.putInt("Frames", frames);
        if (ownerId != null) nbt.putUuid("Owner", ownerId); if (targetId != null) nbt.putUuid("Target", targetId);
        nbt.putFloat("TargetWidth", targetWidth()); nbt.putFloat("TargetHeight", targetHeight());
        nbt.putInt("HitCount", waveHits.size()); int i = 0; for (UUID id : waveHits) nbt.putUuid("Hit" + i++, id);
    }
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(FINISH_AT, nbt.contains("FinishAt") ? nbt.getInt("FinishAt") : -1);
        dataTracker.set(LIFETIME,nbt.getInt("VisualLifetime")); payloadDamage=nbt.getFloat("PayloadDamage");
        if (nbt.contains("LastTargetX")) lastTargetPosition=new Vec3d(nbt.getDouble("LastTargetX"),nbt.getDouble("LastTargetY"),nbt.getDouble("LastTargetZ"));
        dataTracker.set(KIND, MathHelper.clamp(nbt.getInt("Kind"), 0, 10)); dataTracker.set(ELAPSED, nbt.getInt("Elapsed"));
        dataTracker.set(STRINGS, RuneEffectRules.level(nbt.getInt("Strings"))); frames = RuneEffectRules.level(nbt.getInt("Frames"));
        ownerId = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null; targetId = nbt.containsUuid("Target") ? nbt.getUuid("Target") : null;
        dataTracker.set(WIDTH, Math.max(0.9F, nbt.getFloat("TargetWidth"))); dataTracker.set(HEIGHT, Math.max(1F, nbt.getFloat("TargetHeight")));
        waveHits.clear(); for (int i = 0; i < Math.min(4096, nbt.getInt("HitCount")); i++) if (nbt.containsUuid("Hit" + i)) waveHits.add(nbt.getUuid("Hit" + i));
    }
}
