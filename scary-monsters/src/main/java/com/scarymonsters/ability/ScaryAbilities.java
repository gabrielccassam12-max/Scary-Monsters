package com.scarymonsters.ability;

import com.scarymonsters.AnimPayload;
import com.scarymonsters.ModRegistry;
import com.scarymonsters.ScaryMod;
import com.scarymonsters.entity.DinoMinionEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-side implementation of every Scary Monsters attack. */
public final class ScaryAbilities {
    private ScaryAbilities() {}

    public static void execute(ServerPlayerEntity p, ScaryAttack attack) {
        ServerWorld w = (ServerWorld) p.getWorld();
        sendAnim(p, attack.anim);
        switch (attack) {
            case CLAW_RAKE -> clawRake(p, w);
            case POUNCE -> pounce(p, w);
            case TAIL_WHIP -> tailWhip(p, w);
            case BITE -> bite(p, w);
            case RAPTOR_RUSH -> raptorRush(p, w);
            case DINO_ROAR -> dinoRoar(p, w);
            case DINOSAURIZE -> dinosaurize(p, w);
            case CAMOUFLAGE -> camouflage(p, w);
            case TYRANT_BITE -> tyrantBite(p, w);
            case STOMP -> stomp(p, w);
            case PACK_CALL -> packCall(p, w);
            case FOSSILIZE -> fossilize(p, w);
            case METEOR_STORM -> meteorStorm(p, w);
            case EXTINCTION_AURA -> extinctionAura(p, w);
            case RAMPAGE -> rampage(p, w);
            case DINO_ARMY -> dinoArmy(p, w);
        }
    }

    // =====================================================================================
    // FORM 1 - Raptor: fast melee
    // =====================================================================================

    /** Three claw slashes in a cone in front of you. Causes bleeding. */
    private static void clawRake(ServerPlayerEntity p, ServerWorld w) {
        Vec3d look = flat(p.getRotationVec(1f));
        for (LivingEntity e : near(p, w, p.getPos(), 4.2)) {
            Vec3d to = flat(e.getPos().subtract(p.getPos()));
            if (to.dotProduct(look) < 0.4) continue;
            if (hurt(p, w, e, 7f)) {
                push(p, e, 0.4);
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), p);
            }
        }
        for (int i = -1; i <= 1; i++) {
            Vec3d pos = p.getEyePos().add(look.rotateY(i * 0.55f).multiply(2.0)).add(0, -0.4 - i * 0.1, 0);
            w.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        sfx(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
    }

    /** Big forward leap; on landing you smash the ground and hurt everything around you. */
    private static void pounce(ServerPlayerEntity p, ServerWorld w) {
        Vec3d look = p.getRotationVec(1f);
        launch(p, look.x * 1.5, 0.6, look.z * 1.5);
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ATTACK, 0.8f, 1.4f);
        awaitLanding(p, w, 0);
    }

    private static void awaitLanding(ServerPlayerEntity p, ServerWorld w, int n) {
        Scheduler.later(2, () -> {
            if (!valid(p, w)) return;
            if (n >= 3 && p.isOnGround()) {
                landingSmash(p, w);
            } else if (n < 25) {
                awaitLanding(p, w, n + 1);
            }
        });
    }

    private static void landingSmash(ServerPlayerEntity p, ServerWorld w) {
        for (LivingEntity e : near(p, w, p.getPos(), 3.4)) {
            if (hurt(p, w, e, 8f)) {
                push(p, e, 0.9);
                e.addVelocity(0, 0.4, 0);
                e.velocityModified = true;
            }
        }
        debrisRing(w, p, 3.0, 24);
        sfx(w, p, SoundEvents.BLOCK_ANVIL_LAND, 0.6f, 0.7f);
    }

    /** The tail sweeps all the way around you. */
    private static void tailWhip(ServerPlayerEntity p, ServerWorld w) {
        for (LivingEntity e : near(p, w, p.getPos(), 4.2)) {
            if (hurt(p, w, e, 6f)) push(p, e, 1.3);
        }
        for (int i = 0; i < 16; i++) {
            final int k = i;
            Scheduler.later(1 + i / 2, () -> {
                double a = k * Math.PI * 2 / 16;
                Vec3d c = p.getPos();
                w.spawnParticles(ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * 3.0, c.y + 0.8, c.z + Math.sin(a) * 3.0, 1, 0, 0, 0, 0);
            });
        }
        sfx(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
    }

    /** Single-target bite that heals you. */
    private static void bite(ServerPlayerEntity p, ServerWorld w) {
        LivingEntity t = meleeTarget(p, w, 4.0);
        if (t != null && hurt(p, w, t, 10f)) {
            p.heal(4f);
            w.spawnParticles(ParticleTypes.CRIT, t.getX(), t.getBodyY(0.6), t.getZ(), 18, 0.3, 0.3, 0.3, 0.3);
        }
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ATTACK, 1.0f, 1.1f);
    }

    // =====================================================================================
    // FORM 2 - Predator: control and minions
    // =====================================================================================

    /** A dash that slashes everything on the way (each target at most twice). */
    private static void raptorRush(ServerPlayerEntity p, ServerWorld w) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 3));
        Map<Integer, Integer> hits = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            Scheduler.later(1 + i, () -> {
                if (!valid(p, w)) return;
                Vec3d dir = flat(p.getRotationVec(1f));
                launch(p, dir.x * 1.7, Math.max(0, p.getVelocity().y), dir.z * 1.7);
                for (LivingEntity e : near(p, w, p.getPos().add(0, 0.5, 0), 2.3)) {
                    if (hits.merge(e.getId(), 1, Integer::sum) <= 2 && hurt(p, w, e, 5f)) push(p, e, 0.5);
                }
                Vec3d c = p.getPos();
                w.spawnParticles(ParticleTypes.SWEEP_ATTACK, c.x + dir.x, c.y + 1.0, c.z + dir.z, 1, 0.3, 0.3, 0.3, 0);
                w.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + 0.1, c.z, 5, 0.3, 0.05, 0.3, 0.02);
            });
        }
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 0.5f, 1.8f);
    }

    /** Terrifying roar: everything nearby is slowed, weakened, lit up and the monsters run away. */
    private static void dinoRoar(ServerPlayerEntity p, ServerWorld w) {
        Vec3d c = p.getPos();
        for (LivingEntity e : near(p, w, c, 11)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), p);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0), p);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 140, 0), p);
            push(p, e, 0.8);
            if (e instanceof MobEntity m) flee(m, p, 3);
        }
        for (int r = 0; r < 3; r++) {
            final int k = r;
            Scheduler.later(1 + r * 3, () -> w.spawnParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1.2, c.z, 1, 0, 0, 0, 0));
            Scheduler.later(1 + r * 3, () -> ringParticles(w, ParticleTypes.CLOUD, c, 2.5 + k * 3.0, 28, 0.3));
        }
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.6f, 0.7f);
        sfx(w, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.8f);
    }

    /** SIGNATURE: turns the mob you look at into a raptor that fights for you. */
    private static void dinosaurize(ServerPlayerEntity p, ServerWorld w) {
        LivingEntity t = aimedEntity(p, w, 22);
        if (!(t instanceof MobEntity m) || m instanceof PassiveEntity || m instanceof TameableEntity
                || m instanceof WitherEntity || m instanceof EnderDragonEntity || m instanceof WardenEntity
                || m instanceof DinoMinionEntity || m.getMaxHealth() > 120) {
            p.sendMessage(Text.translatable("message.scarymonsters.cannot"), true);
            return;
        }
        Vec3d pos = m.getPos();
        boolean rex = m.getMaxHealth() >= 60;
        w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + m.getHeight() * 0.5, pos.z, 40, 0.4, 0.6, 0.4, 0.1);
        w.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 20, 0.4, 0.4, 0.4, 0.05);
        m.discard();
        spawnMinion(p, w, rex, pos, ScaryMod.MINION_LIFETIME);
        sfx(w, p, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.7f);
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 0.8f, 1.2f);
    }

    /** Invisible, fast, and every monster forgets you exist for eight seconds. */
    private static void camouflage(ServerPlayerEntity p, ServerWorld w) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 170, 0, true, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 170, 2, true, false, false));
        for (int i = 0; i < 34; i++) {
            Scheduler.later(1 + i * 5, () -> {
                if (!valid(p, w)) return;
                for (MobEntity m : w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(20),
                        e -> e.getTarget() == p)) {
                    m.setTarget(null);
                }
            });
        }
        w.spawnParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        sfx(w, p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
    }

    // =====================================================================================
    // FORM 3 - Tyrant: heavy hits
    // =====================================================================================

    /** Crushing bite: huge damage (more against weakened prey) and a big heal. */
    private static void tyrantBite(ServerPlayerEntity p, ServerWorld w) {
        LivingEntity t = meleeTarget(p, w, 5.0);
        if (t != null) {
            boolean weak = t.getHealth() < t.getMaxHealth() * 0.3f;
            if (hurt(p, w, t, weak ? 34f : 22f)) {
                p.heal(8f);
                push(p, t, 0.8);
                w.spawnParticles(ParticleTypes.CRIT, t.getX(), t.getBodyY(0.6), t.getZ(), 40, 0.4, 0.4, 0.4, 0.4);
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.RED_WOOL.getDefaultState()),
                        t.getX(), t.getBodyY(0.5), t.getZ(), 25, 0.3, 0.4, 0.3, 0.1);
            }
        }
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ATTACK, 1.3f, 0.6f);
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 0.6f, 0.7f);
    }

    /** Earth-shaking stomp: damages, launches and slows everything within seven blocks. */
    private static void stomp(ServerPlayerEntity p, ServerWorld w) {
        Vec3d c = p.getPos();
        for (LivingEntity e : near(p, w, c, 7)) {
            if (hurt(p, w, e, 10f)) {
                e.addVelocity(0, 0.9, 0);
                e.velocityModified = true;
                push(p, e, 0.6);
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2), p);
            }
        }
        for (int r = 0; r < 4; r++) {
            final int k = r;
            Scheduler.later(1 + r * 2, () -> debrisRing(w, p, 1.5 + k * 1.8, 20 + k * 6));
        }
        sfx(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.5f);
        sfx(w, p, SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
    }

    /** Three raptors answer the call. */
    private static void packCall(ServerPlayerEntity p, ServerWorld w) {
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2 / 3 + p.getYaw() * 0.0175;
            spawnMinion(p, w, false, p.getPos().add(Math.cos(a) * 3, 0, Math.sin(a) * 3), ScaryMod.MINION_LIFETIME);
        }
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 1.5f);
    }

    /** The target turns to stone: it cannot move or act for three seconds and cracks under the pressure. */
    private static void fossilize(ServerPlayerEntity p, ServerWorld w) {
        LivingEntity t = aimedEntity(p, w, 24);
        if (t == null) t = meleeTarget(p, w, 10);
        if (t == null) {
            p.sendMessage(Text.translatable("message.scarymonsters.no_target"), true);
            return;
        }
        final LivingEntity target = t;
        BlockStateParticleEffect stone = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STONE.getDefaultState());
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 2), p);
        if (target instanceof MobEntity m) m.setAiDisabled(true);
        for (int i = 0; i < 60; i++) {
            final int k = i;
            Scheduler.later(1 + i, () -> {
                if (!target.isAlive() || target.isRemoved()) return;
                target.setVelocity(0, Math.min(target.getVelocity().y, 0), 0);
                target.velocityModified = true;
                if (k % 3 == 0) {
                    w.spawnParticles(stone, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.35, 0.6, 0.35, 0.02);
                }
                if (k % 15 == 14) {
                    hurt(p, w, target, 4f);
                    sfx(w, p, SoundEvents.BLOCK_STONE_BREAK, 1.0f, 0.6f);
                }
            });
        }
        Scheduler.later(62, () -> {
            if (target instanceof MobEntity m) m.setAiDisabled(false);
        });
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_STUNNED, 1.0f, 0.8f);
    }

    // =====================================================================================
    // FORM 4 - Primordial: extinction-level power
    // =====================================================================================

    /** Seven meteors fall around the spot you look at. Do not stand there. */
    private static void meteorStorm(ServerPlayerEntity p, ServerWorld w) {
        Vec3d c = aimPoint(p, w, 45);
        for (int i = 0; i < 7; i++) {
            final int delay = 14 + i * 9;
            double a = w.random.nextDouble() * Math.PI * 2;
            double r = i == 0 ? 0 : Math.sqrt(w.random.nextDouble()) * 6.5;
            final Vec3d t = new Vec3d(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r);
            Scheduler.later(1 + i * 9, () -> ringParticles(w, ParticleTypes.FLAME, t.add(0, 0.2, 0), 2.8, 24, 0.0));
            for (int s = 0; s < 8; s++) {
                final int step = s;
                Scheduler.later(delay + step, () -> {
                    Vec3d pos = t.add(0, 28 * (1 - (step + 1) / 8.0), 0);
                    w.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 14, 0.45, 0.45, 0.45, 0.02);
                    w.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 1, pos.z, 6, 0.3, 0.6, 0.3, 0.02);
                });
            }
            Scheduler.later(delay + 8, () -> {
                Entity source = p.isAlive() ? p : null;
                World.ExplosionSourceType type = ScaryMod.BREAK_BLOCKS
                        ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE;
                w.createExplosion(source, t.x, t.y, t.z, 2.8f, type);
                w.spawnParticles(ParticleTypes.LAVA, t.x, t.y + 0.5, t.z, 20, 1.2, 0.3, 1.2, 0.2);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(t, t).expand(3.5),
                        e -> e != p && e.isAlive() && !(e instanceof DinoMinionEntity))) {
                    e.setOnFireFor(4);
                }
            });
        }
        sfx(w, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);
    }

    /** Ten seconds of a burning aura around you; you take less damage while it lasts. */
    private static void extinctionAura(ServerPlayerEntity p, ServerWorld w) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 210, 1));
        sfx(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.6f);
        for (int i = 0; i < 40; i++) {
            final int k = i;
            Scheduler.later(1 + i * 5, () -> {
                if (!valid(p, w)) return;
                Vec3d c = p.getPos().add(0, 0.6, 0);
                ringParticles(w, ParticleTypes.FLAME, c, 6.0, 26, 0.02);
                w.spawnParticles(ParticleTypes.ASH, c.x, c.y + 1, c.z, 25, 3.5, 1.5, 3.5, 0.02);
                if (k % 2 == 0) {
                    for (LivingEntity e : near(p, w, p.getPos(), 6.5)) {
                        e.timeUntilRegen = 0;
                        e.damage(w.getDamageSources().indirectMagic(p, p), 3f);
                        e.setOnFireFor(2);
                    }
                }
            });
        }
    }

    /** Eight seconds of pure fury: strong buffs and a claw storm around you every half second. */
    private static void rampage(ServerPlayerEntity p, ServerWorld w) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 170, 3));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 170, 2));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 170, 2));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 170, 2));
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.6f, 0.6f);
        for (int i = 0; i < 16; i++) {
            Scheduler.later(1 + i * 10, () -> {
                if (!valid(p, w)) return;
                sendAnim(p, Anims.CLAW);
                Vec3d c = p.getPos();
                for (LivingEntity e : near(p, w, c, 3.6)) {
                    if (hurt(p, w, e, 6f)) push(p, e, 0.6);
                }
                for (int j = 0; j < 6; j++) {
                    double a = w.random.nextDouble() * Math.PI * 2;
                    w.spawnParticles(ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * 2.2, c.y + 0.5 + w.random.nextDouble(),
                            c.z + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
                }
                sfx(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.9f);
            });
        }
    }

    /** Four raptors and a Rex, for a full minute. */
    private static void dinoArmy(ServerPlayerEntity p, ServerWorld w) {
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + Math.PI / 4;
            spawnMinion(p, w, false, p.getPos().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5), ScaryMod.MINION_LIFETIME * 4 / 3);
        }
        Vec3d behind = flat(p.getRotationVec(1f)).multiply(-5);
        spawnMinion(p, w, true, p.getPos().add(behind), ScaryMod.MINION_LIFETIME * 4 / 3);
        sfx(w, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
        sfx(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.2f, 0.6f);
    }

    // =====================================================================================
    // helpers
    // =====================================================================================

    private static void sendAnim(ServerPlayerEntity p, int anim) {
        AnimPayload payload = new AnimPayload(p.getId(), anim);
        ServerPlayNetworking.send(p, payload);
        for (ServerPlayerEntity other : PlayerLookup.tracking(p)) {
            ServerPlayNetworking.send(other, payload);
        }
    }

    private static boolean valid(ServerPlayerEntity p, ServerWorld w) {
        return p.isAlive() && !p.isRemoved() && p.getWorld() == w;
    }

    private static boolean friendly(ServerPlayerEntity p, Entity e) {
        return e instanceof DinoMinionEntity m && m.isOwnedBy(p);
    }

    private static List<LivingEntity> near(ServerPlayerEntity p, ServerWorld w, Vec3d c, double r) {
        return w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !friendly(p, e) && e.squaredDistanceTo(c) <= r * r);
    }

    private static boolean hurt(ServerPlayerEntity p, ServerWorld w, LivingEntity e, float dmg) {
        e.timeUntilRegen = 0;
        return e.damage(w.getDamageSources().playerAttack(p), dmg);
    }

    private static void push(ServerPlayerEntity p, LivingEntity e, double strength) {
        e.takeKnockback(strength, p.getX() - e.getX(), p.getZ() - e.getZ());
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0, v.z);
        return f.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : f.normalize();
    }

    private static void launch(ServerPlayerEntity p, double x, double y, double z) {
        p.setVelocity(x, y, z);
        p.velocityModified = true;
        p.fallDistance = 0;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    private static void flee(MobEntity m, ServerPlayerEntity p, int times) {
        for (int i = 0; i < times; i++) {
            Scheduler.later(1 + i * 12, () -> {
                if (!m.isAlive()) return;
                m.setTarget(null);
                Vec3d away = flat(m.getPos().subtract(p.getPos()));
                m.getNavigation().startMovingTo(m.getX() + away.x * 12, m.getY(), m.getZ() + away.z * 12, 1.5);
            });
        }
    }

    private static DinoMinionEntity spawnMinion(ServerPlayerEntity p, ServerWorld w, boolean rex, Vec3d pos, int life) {
        DinoMinionEntity m = new DinoMinionEntity(rex ? ModRegistry.REX : ModRegistry.RAPTOR, w);
        m.refreshPositionAndAngles(pos.x, pos.y, pos.z, p.getYaw(), 0);
        m.setOwner(p.getUuid());
        m.setLifetime(life);
        w.spawnEntity(m);
        w.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 15, 0.4, 0.4, 0.4, 0.05);
        return m;
    }

    private static void ringParticles(ServerWorld w, ParticleEffect fx, Vec3d c, double r, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            w.spawnParticles(fx, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, speed);
        }
    }

    private static void debrisRing(ServerWorld w, ServerPlayerEntity p, double r, int n) {
        BlockState below = w.getBlockState(p.getBlockPos().down());
        if (below.isAir()) below = Blocks.STONE.getDefaultState();
        BlockStateParticleEffect fx = new BlockStateParticleEffect(ParticleTypes.BLOCK, below);
        Vec3d c = p.getPos();
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            w.spawnParticles(fx, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 3, 0.15, 0.2, 0.15, 0.15);
        }
    }

    private static void sfx(ServerWorld w, ServerPlayerEntity p, SoundEvent sound, float volume, float pitch) {
        w.playSound(null, p.getBlockPos(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    /** Best melee victim: the entity under the crosshair, or the closest one inside a forward cone. */
    private static LivingEntity meleeTarget(ServerPlayerEntity p, ServerWorld w, double range) {
        LivingEntity aimed = aimedEntity(p, w, range);
        if (aimed != null && !friendly(p, aimed)) return aimed;
        Vec3d look = flat(p.getRotationVec(1f));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : near(p, w, p.getPos(), range)) {
            Vec3d to = flat(e.getPos().subtract(p.getPos()));
            if (to.dotProduct(look) < 0.5) continue;
            double d = e.squaredDistanceTo(p);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static Vec3d aimPoint(ServerPlayerEntity p, ServerWorld w, double range) {
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1f).multiply(range));
        HitResult block = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, p));
        Vec3d blockEnd = block.getType() == HitResult.Type.MISS ? end : block.getPos();
        EntityHitResult ent = ProjectileUtil.raycast(p, eye, blockEnd, new Box(eye, blockEnd).expand(1.0),
                e -> !e.isSpectator() && e.canHit(), range * range);
        return ent != null ? ent.getPos() : blockEnd;
    }

    private static LivingEntity aimedEntity(ServerPlayerEntity p, ServerWorld w, double range) {
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1f).multiply(range));
        HitResult block = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, p));
        Vec3d blockEnd = block.getType() == HitResult.Type.MISS ? end : block.getPos();
        EntityHitResult ent = ProjectileUtil.raycast(p, eye, blockEnd, new Box(eye, blockEnd).expand(1.0),
                e -> !e.isSpectator() && e.canHit(), range * range);
        return ent != null && ent.getEntity() instanceof LivingEntity le ? le : null;
    }
}
