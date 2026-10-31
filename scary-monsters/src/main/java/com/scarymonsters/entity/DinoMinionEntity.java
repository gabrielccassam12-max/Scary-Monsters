package com.scarymonsters.entity;

import com.scarymonsters.ModRegistry;
import com.scarymonsters.ScaryMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * A dinosaur that fights for the Scary Monsters user. Two types share this class:
 * the Raptor (fast) and the Rex (huge). It follows its owner, attacks whatever the owner fights
 * or any nearby monster, never hurts the owner, and poofs away when its time is up.
 */
public class DinoMinionEntity extends PathAwareEntity {
    private static final TrackedData<Optional<UUID>> OWNER =
            DataTracker.registerData(DinoMinionEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private int lifetime = ScaryMod.MINION_LIFETIME;

    public DinoMinionEntity(EntityType<? extends DinoMinionEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder raptorAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.42)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28);
    }

    public static DefaultAttributeContainer.Builder rexAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 16)
                .add(EntityAttributes.GENERIC_ARMOR, 6)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    public boolean isRex() {
        return getType() == ModRegistry.REX;
    }

    // ------------------------------------------------------------ owner
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER, Optional.empty());
    }

    public UUID getOwnerUuid() {
        return dataTracker.get(OWNER).orElse(null);
    }

    public void setOwner(UUID uuid) {
        dataTracker.set(OWNER, Optional.ofNullable(uuid));
    }

    public PlayerEntity getOwnerPlayer() {
        UUID id = getOwnerUuid();
        return id == null ? null : getWorld().getPlayerByUuid(id);
    }

    public boolean isOwnedBy(Entity other) {
        UUID id = getOwnerUuid();
        return id != null && other != null && id.equals(other.getUuid());
    }

    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    // ------------------------------------------------------------ AI
    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.4, true));
        goalSelector.add(3, new FollowOwnerGoal(this));
        goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, MobEntity.class, 10, true, false,
                e -> e instanceof Monster && !(e instanceof DinoMinionEntity)));
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof DinoMinionEntity other && other.getOwnerUuid() != null
                && other.getOwnerUuid().equals(getOwnerUuid())) return false;
        if (isOwnedBy(target)) return false;
        return super.canTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        PlayerEntity owner = getOwnerPlayer();
        if (--lifetime <= 0 || owner == null || !owner.isAlive()) {
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.POOF, getX(), getBodyY(0.5), getZ(), 25, 0.5, 0.6, 0.5, 0.05);
            }
            discard();
            return;
        }
        if (age % 10 == 0) {
            LivingEntity t = owner.getAttacking();
            if (t == null || !t.isAlive()) t = owner.getAttacker();
            if (t != null && t.isAlive() && t != this && t != owner && !(t instanceof DinoMinionEntity)) {
                setTarget(t);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() != null && isOwnedBy(source.getAttacker())) return false;
        return super.damage(source, amount);
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    // ------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return isRex() ? 0.6f : 1.3f;
    }

    // ------------------------------------------------------------ follow goal
    private static class FollowOwnerGoal extends Goal {
        private final DinoMinionEntity mob;
        private int cooldown;

        FollowOwnerGoal(DinoMinionEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            PlayerEntity o = mob.getOwnerPlayer();
            return o != null && mob.getTarget() == null && mob.squaredDistanceTo(o) > 36;
        }

        @Override
        public boolean shouldContinue() {
            PlayerEntity o = mob.getOwnerPlayer();
            return o != null && mob.getTarget() == null && mob.squaredDistanceTo(o) > 9;
        }

        @Override
        public void start() {
            cooldown = 0;
        }

        @Override
        public void tick() {
            PlayerEntity o = mob.getOwnerPlayer();
            if (o == null) return;
            if (mob.squaredDistanceTo(o) > 900) { // too far: catch up instantly
                mob.refreshPositionAndAngles(o.getX(), o.getY(), o.getZ(), mob.getYaw(), mob.getPitch());
                mob.getNavigation().stop();
            } else if (--cooldown <= 0) {
                cooldown = 10;
                mob.getNavigation().startMovingTo(o, 1.5);
            }
        }
    }
}
