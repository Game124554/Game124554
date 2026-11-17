package com.thunderscience.changedtsc.boss;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.registry.ModItems;
import com.thunderscience.changedtsc.util.EntityResolver;
import com.thunderscience.changedtsc.util.EntitySpawning;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a Changed latex mob into a boss: boss stats, a boss bar, special attacks, minions and loot.
 * Bosses are marked with entity tags so they survive save/load.
 */
public final class BossManager {
    public static final String BOSS_TAG = "changed_tsc.boss";
    public static final String TYPE_PREFIX = "changed_tsc.boss_type.";
    public static final String MINION_TAG = "changed_tsc.minion";
    public static final String ENRAGED_TAG = "changed_tsc.enraged";
    /** Squid dogs that shield the Squid Dog boss with metal plates. */
    public static final String SHIELD_TAG = "changed_tsc.shield_bearer";

    private static final String SWOOPS = "changed_tsc_swoops";
    private static final String TIRED_UNTIL = "changed_tsc_tired_until";
    private static final String SHIELD_STAGE = "changed_tsc_shield_stage";
    private static final String SELF_HIT_UNTIL = "changed_tsc_self_hit_until";
    private static final String EXPIRE = "changed_tsc_expire";
    private static final String POUNCE_X = "changed_tsc_pounce_x";
    private static final String POUNCE_Y = "changed_tsc_pounce_y";
    private static final String POUNCE_Z = "changed_tsc_pounce_z";

    /** A marked spot on the floor that is hit when the game time reaches {@code due}. */
    private record Strike(Vec3 pos, long due, float damage, float radius, boolean white) {}

    private static final Map<UUID, List<Strike>> STRIKES = new HashMap<>();

    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private BossManager() {}

    public static boolean isBoss(@Nullable Entity e) {
        return e != null && e.getTags().contains(BOSS_TAG);
    }

    public static Optional<BossType> typeOf(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(TYPE_PREFIX)) return BossType.byId(tag.substring(TYPE_PREFIX.length()));
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ spawning

    @Nullable
    public static Mob spawnBoss(ServerLevel level, BlockPos pos, BossType type) {
        if (!TSCConfig.BOSSES_ENABLED.get()) return null;
        String spec = TSCConfig.BOSS_ENTITY.get(type).get();
        EntityType<?> entityType = EntityResolver.resolve(spec).orElse(null);
        if (entityType == null) {
            ChangedTSC.LOGGER.warn("Boss {} could not be spawned: no entity matches '{}'", type.id(), spec);
            return null;
        }
        Entity spawned = EntitySpawning.spawn(level, entityType, pos, MobSpawnType.EVENT, true,
                BOSS_TAG, TYPE_PREFIX + type.id());
        if (!(spawned instanceof Mob mob)) {
            if (spawned != null) spawned.discard();
            ChangedTSC.LOGGER.warn("Boss {} entity '{}' is not a mob", type.id(), spec);
            return null;
        }
        applyStats(mob, type);
        mob.setCustomName(type.displayName().copy().withStyle(ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);

        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.8F);
        for (ServerPlayer p : level.getPlayers(p -> p.blockPosition().closerThan(pos, 48))) {
            p.displayClientMessage(Component.translatable("message.changed_tsc.boss_awakens", type.displayName())
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        return mob;
    }

    private static void applyStats(Mob mob, BossType type) {
        setBase(mob.getAttribute(Attributes.MAX_HEALTH), TSCConfig.BOSS_HEALTH.get(type).get());
        mob.setHealth(mob.getMaxHealth());
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(Math.max(dmg.getBaseValue(), TSCConfig.BOSS_DAMAGE.get(type).get()));
        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(Math.max(armor.getBaseValue(), 8.0));
        setBase(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), 0.8);
        setBase(mob.getAttribute(Attributes.FOLLOW_RANGE), 48.0);
    }

    private static void setBase(@Nullable AttributeInstance attribute, double value) {
        if (attribute != null) attribute.setBaseValue(value);
    }

    // ------------------------------------------------------------------ ticking

    public static void tickBoss(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        BossType type = typeOf(mob).orElse(null);
        if (type == null) return;

        ServerBossEvent bar = BARS.computeIfAbsent(mob.getUUID(), id -> {
            ServerBossEvent b = new ServerBossEvent(type.displayName(), type.color(), BossEvent.BossBarOverlay.NOTCHED_10);
            b.setDarkenScreen(type == BossType.ELDER_ONE);
            return b;
        });
        bar.setProgress(Mth.clamp(mob.getHealth() / mob.getMaxHealth(), 0.0F, 1.0F));
        if (mob.tickCount % 20 == 0) updateBarPlayers(level, mob, bar);
        if (!mob.isAlive()) return;

        boolean enraged = mob.getTags().contains(ENRAGED_TAG);
        if (!enraged && mob.getHealth() < mob.getMaxHealth() * 0.5F) {
            mob.addTag(ENRAGED_TAG);
            enraged = true;
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0));
            level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.7F);
            for (Player p : playersNear(mob, 32)) {
                p.displayClientMessage(Component.translatable("message.changed_tsc.boss_enraged", type.displayName())
                        .withStyle(ChatFormatting.RED), true);
            }
            if (type == BossType.SQUID_DOG) releaseCaptive(level, mob);
        }

        tickStrikes(level, mob);
        switch (type) {
            case SQUID_DOG -> tickSquidDog(level, mob, enraged);
            case TIGER_SHARK -> tickTigerShark(level, mob, enraged);
            case ELDER_ONE -> tickElderOne(level, mob, enraged);
            case WOLF_KING -> tickWolfKing(level, mob, enraged);
        }

        // the bar shows when the boss is protected (yellow) or wide open (green)
        if (mob.tickCount % 10 == 0) {
            float taken = damageTakenMultiplier(mob);
            bar.setColor(taken < 1.0F ? BossEvent.BossBarColor.YELLOW : taken > 1.0F ? BossEvent.BossBarColor.GREEN : type.color());
        }
    }

    // ------------------------------------------------------------------ the four fights

    /**
     * Squid Dog, after the original game: four tentacles chase you around the arena, and the way
     * to win is to make them hit the Squid Dog itself. Stand close to it when a tentacle comes down.
     * As in the Special Edition, other squid dogs come to shield the boss with metal plates; while
     * a shield bearer lives the boss takes very little damage. It also holds a captured Tiger Shark,
     * which it lets go when it becomes enraged.
     */
    private static void tickSquidDog(ServerLevel level, Mob mob, boolean enraged) {
        int t = mob.tickCount;
        CompoundTag data = mob.getPersistentData();

        int every = enraged ? 40 : 60;
        if (t % every == 0) {
            for (Player p : playersNear(mob, 16)) {
                markStrike(level, mob, p.position(), 24, enraged ? 10.0F : 8.0F, 2.2F, false);
                if (enraged) {
                    // a second tentacle comes down right beside the first
                    Vec3 side = p.position().add(level.random.nextInt(7) - 3, 0.0, level.random.nextInt(7) - 3);
                    markStrike(level, mob, side, 34, 8.0F, 2.2F, false);
                }
            }
        }
        if (t % 240 == 120) inkBurst(level, mob);
        if (enraged && t % 160 == 80) tentaclePull(mob);

        float health = mob.getHealth() / mob.getMaxHealth();
        int stage = data.getInt(SHIELD_STAGE);
        if ((stage == 0 && health < 0.7F) || (stage == 1 && health < 0.35F)) {
            data.putInt(SHIELD_STAGE, stage + 1);
            String spec = TSCConfig.BOSS_ENTITY.get(BossType.SQUID_DOG).get();
            for (int i = 0; i < 2; i++) {
                Mob bearer = spawnMinion(level, mob, spec, around(level, mob, 4), 0, true, SHIELD_TAG);
                if (bearer == null) continue;
                bearer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                bearer.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
                bearer.setCustomName(Component.translatable("entity.changed_tsc.shield_bearer"));
                bearer.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            }
            announce(mob, "message.changed_tsc.boss.squid_shield", 32);
        }
    }

    private static void squidSelfHit(ServerLevel level, Mob mob) {
        CompoundTag data = mob.getPersistentData();
        long now = level.getGameTime();
        if (data.getLong(SELF_HIT_UNTIL) > now) return;
        data.putLong(SELF_HIT_UNTIL, now + 100);
        // generic damage has no attacker, so shield bearers do not protect the boss from itself
        mob.hurt(level.damageSources().generic(), mob.getMaxHealth() * 0.05F);
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));
        level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 1.2, mob.getZ(), 30, 0.6, 0.6, 0.6, 0.2);
        level.playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.0F, 0.7F);
        announce(mob, "message.changed_tsc.boss.squid_self_hit", 32);
    }

    private static void releaseCaptive(ServerLevel level, Mob mob) {
        Mob shark = spawnMinion(level, mob, TSCConfig.BOSS_MINIONS.get(BossType.SQUID_DOG).get(), around(level, mob, 3), 0, true);
        if (shark == null) return;
        shark.setCustomName(BossType.TIGER_SHARK.displayName());
        Player target = pickTarget(mob, 32);
        if (target != null) shark.setTarget(target);
        announce(mob, "message.changed_tsc.boss.squid_release", 32);
    }

    /**
     * Tiger Shark, after the original game: he swims after you, faster than the other sharks,
     * and calls two lines of sharks, one from each side, so do not stand in the middle. Every
     * swoop also brings one more shark that homes in on you. After his third swoop he is worn out
     * and slows down: that is your opening. He is strong in water and weak on dry floor.
     */
    private static void tickTigerShark(ServerLevel level, Mob mob, boolean enraged) {
        int t = mob.tickCount;
        CompoundTag data = mob.getPersistentData();
        long now = level.getGameTime();

        if (data.getLong(TIRED_UNTIL) > now) {
            if (t % 10 == 0) {
                level.sendParticles(ParticleTypes.SPLASH, mob.getX(), mob.getY() + 1.0, mob.getZ(), 12, 0.4, 0.5, 0.4, 0.0);
            }
            return;
        }
        if (mob.isInWaterOrBubble()) {
            if (t % 20 == 0) mob.heal(2.0F);
        } else if (t % 40 == 0) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        Player target = pickTarget(mob, 32);
        if (target == null) return;
        int phase = t % (enraged ? 110 : 150);
        if (phase == 0) {
            sharkLines(level, mob, target, enraged ? 3 : 2);
            announce(mob, "message.changed_tsc.boss.shark_lines", 32);
        } else if (phase == 40) {
            // the fin cuts the water: he is about to swoop
            level.playSound(null, mob.blockPosition(), SoundEvents.DOLPHIN_SPLASH, SoundSource.HOSTILE, 2.0F, 0.6F);
            level.sendParticles(ParticleTypes.BUBBLE_POP, mob.getX(), mob.getY() + 0.5, mob.getZ(), 30, 0.8, 0.3, 0.8, 0.02);
        } else if (phase == 55) {
            swoop(mob, target, enraged ? 2.2 : 1.8);
            Mob homing = spawnMinion(level, mob, TSCConfig.BOSS_MINIONS.get(BossType.TIGER_SHARK).get(),
                    around(level, mob, 3), 400, false);
            if (homing != null) homing.setTarget(target);

            int swoops = data.getInt(SWOOPS) + 1;
            if (swoops >= 3) {
                swoops = 0;
                int rest = enraged ? 120 : 160;
                data.putLong(TIRED_UNTIL, now + rest);
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rest, 4));
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, rest, 1));
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, rest, 0, false, false));
                level.playSound(null, mob.blockPosition(), SoundEvents.DOLPHIN_HURT, SoundSource.HOSTILE, 2.0F, 0.5F);
                announce(mob, "message.changed_tsc.boss.shark_tired", 32);
            }
            data.putInt(SWOOPS, swoops);
        }
    }

    /** Two rows of sharks, one on each side of the player, that rush across. The gaps are the safe spots. */
    private static void sharkLines(ServerLevel level, Mob boss, Player target, int perSide) {
        Vec3 forward = new Vec3(target.getX() - boss.getX(), 0.0, target.getZ() - boss.getZ());
        forward = forward.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : forward.normalize();
        Vec3 sideways = new Vec3(-forward.z, 0.0, forward.x);
        String spec = TSCConfig.BOSS_MINIONS.get(BossType.TIGER_SHARK).get();
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < perSide; i++) {
                double along = (i - (perSide - 1) / 2.0) * 3.0;
                Vec3 pos = target.position().add(sideways.scale(side * 8.0)).add(forward.scale(along));
                Mob shark = spawnMinion(level, boss, spec, pos, 200, false);
                if (shark == null) continue;
                shark.setDeltaMovement(sideways.scale(-side * 0.9));
                shark.hasImpulse = true;
                shark.setTarget(target);
            }
        }
    }

    private static void swoop(Mob mob, Player target, double strength) {
        Vec3 dir = target.position().subtract(mob.position());
        if (dir.lengthSqr() < 4.0) return;
        dir = dir.normalize();
        mob.setDeltaMovement(dir.x * strength, Math.max(0.2, dir.y * strength), dir.z * strength);
        mob.hasImpulse = true;
        mob.setTarget(target);
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    /**
     * Wolf King: the leader of the dark latex pack. He howls for his wolves and is hard to hurt
     * while two or more of them stand with him, so deal with the pack first. Between howls he
     * crouches and pounces on the spot where you stood; move away from the mark.
     */
    private static void tickWolfKing(ServerLevel level, Mob mob, boolean enraged) {
        int phase = mob.tickCount % (enraged ? 180 : 240);
        if (phase == 0) {
            level.playSound(null, mob.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 3.0F, 0.6F);
            summonMinions(level, mob, BossType.WOLF_KING, enraged ? 4 : 3);
            announce(mob, "message.changed_tsc.boss.wolf_howl", 32);
        }
        Player target = pickTarget(mob, 32);
        if (target == null) return;
        CompoundTag data = mob.getPersistentData();
        if (phase == 90) {
            Vec3 spot = target.position();
            data.putDouble(POUNCE_X, spot.x);
            data.putDouble(POUNCE_Y, spot.y);
            data.putDouble(POUNCE_Z, spot.z);
            markStrike(level, mob, spot, 30, enraged ? 14.0F : 10.0F, 3.0F, false);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 10, false, false));
            level.playSound(null, mob.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 3.0F, 0.5F);
        } else if (phase == 115) {
            Vec3 jump = new Vec3(data.getDouble(POUNCE_X) - mob.getX(), 0.0, data.getDouble(POUNCE_Z) - mob.getZ()).scale(0.12);
            if (jump.lengthSqr() > 2.5 * 2.5) jump = jump.normalize().scale(2.5);
            mob.setDeltaMovement(jump.x, 0.6, jump.z);
            mob.hasImpulse = true;
        } else if (enraged && phase == 150) {
            shockwave(level, mob, 8.0F);
        }
    }

    /**
     * The Elder One: an old white latex. White latex bursts out of the floor under everyone, it
     * regenerates, darkens the room, and calls other latex. When enraged it absorbs its own
     * helpers to heal, so do not leave them alive.
     */
    private static void tickElderOne(ServerLevel level, Mob mob, boolean enraged) {
        int t = mob.tickCount;
        if (t % 40 == 0) mob.heal(1.0F);
        if (t % 200 == 0) elderPulse(level, mob);
        if (t % (enraged ? 50 : 80) == 25) {
            for (Player p : playersNear(mob, 20)) markStrike(level, mob, p.position(), 20, enraged ? 9.0F : 7.0F, 2.0F, true);
        }
        if (t % (enraged ? 240 : 360) == 0) summonMinions(level, mob, BossType.ELDER_ONE, 3);
        if (enraged && t % 300 == 150) {
            int absorbed = 0;
            for (Mob minion : tagged(mob, MINION_TAG, 16)) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, minion.getX(), minion.getY() + 1.0, minion.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                minion.discard();
                absorbed++;
            }
            if (absorbed > 0) {
                mob.heal(15.0F * absorbed);
                announce(mob, "message.changed_tsc.boss.elder_absorb", 32);
            }
        }
    }

    // ------------------------------------------------------------------ marked strikes (tentacles, pounces, eruptions)

    /** Marks a spot on the floor. After the delay it is hit; anyone still standing there is hurt. */
    private static void markStrike(ServerLevel level, Mob boss, Vec3 pos, int delay, float damage, float radius, boolean white) {
        STRIKES.computeIfAbsent(boss.getUUID(), id -> new ArrayList<>())
                .add(new Strike(pos, level.getGameTime() + delay, damage, radius, white));
    }

    private static void tickStrikes(ServerLevel level, Mob boss) {
        List<Strike> strikes = STRIKES.get(boss.getUUID());
        if (strikes == null || strikes.isEmpty()) return;
        long now = level.getGameTime();
        boolean hitItself = false;
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            if (now < s.due()) {
                // warning ring on the floor
                if (now % 4 == 0) {
                    level.sendParticles(s.white() ? ParticleTypes.SNOWFLAKE : ParticleTypes.SQUID_INK,
                            s.pos().x, s.pos().y + 0.15, s.pos().z, 8, s.radius() * 0.45, 0.05, s.radius() * 0.45, 0.0);
                }
                continue;
            }
            it.remove();
            AABB box = new AABB(s.pos(), s.pos()).inflate(s.radius(), 2.0, s.radius());
            for (Player p : level.getEntitiesOfClass(Player.class, box, pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
                p.hurt(level.damageSources().mobAttack(boss), s.damage());
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, s.pos().x, s.pos().y + 0.8, s.pos().z, 4, s.radius() * 0.4, 0.3, s.radius() * 0.4, 0.0);
            level.playSound(null, BlockPos.containing(s.pos()), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.5F);
            if (boss.position().distanceToSqr(s.pos()) < 3.0 * 3.0) hitItself = true;
        }
        if (hitItself && typeOf(boss).orElse(null) == BossType.SQUID_DOG) squidSelfHit(level, boss);
    }

    // ------------------------------------------------------------------ helpers

    /**
     * How much of an attacker's damage gets through: below 1 = protected, above 1 = wide open.
     * Squid Dog is shielded while a shield bearer lives, the Wolf King while his pack stands with
     * him, and the Tiger Shark is open while worn out or out of the water.
     */
    public static float damageTakenMultiplier(LivingEntity boss) {
        BossType type = typeOf(boss).orElse(null);
        if (type == null) return 1.0F;
        return switch (type) {
            case SQUID_DOG -> tagged(boss, SHIELD_TAG, 24).isEmpty() ? 1.0F : 0.2F;
            case TIGER_SHARK -> boss.getPersistentData().getLong(TIRED_UNTIL) > boss.level().getGameTime() ? 2.0F
                    : boss.isInWaterOrBubble() ? 1.0F : 1.5F;
            case WOLF_KING -> tagged(boss, MINION_TAG, 24).size() >= 2 ? 0.5F : 1.0F;
            case ELDER_ONE -> 1.0F;
        };
    }

    /** Removes timed helpers (shark lines) when their time is up. Called once a second. */
    public static void tickMinion(Mob minion) {
        long expire = minion.getPersistentData().getLong(EXPIRE);
        if (expire > 0L && minion.level().getGameTime() > expire) {
            if (minion.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SPLASH, minion.getX(), minion.getY() + 0.5, minion.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
            }
            minion.discard();
        }
    }

    private static List<Mob> tagged(LivingEntity boss, String tag, double radius) {
        return boss.level().getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(radius),
                m -> m.isAlive() && m != boss && m.getTags().contains(tag));
    }

    @Nullable
    private static Player pickTarget(Mob mob, double radius) {
        if (mob.getTarget() instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator()) return p;
        Player best = null;
        for (Player p : playersNear(mob, radius)) {
            if (best == null || p.distanceToSqr(mob) < best.distanceToSqr(mob)) best = p;
        }
        return best;
    }

    private static Vec3 around(ServerLevel level, Mob mob, int spread) {
        return mob.position().add(level.random.nextInt(spread * 2 + 1) - spread, 0.0, level.random.nextInt(spread * 2 + 1) - spread);
    }

    private static void announce(Mob boss, String key, double radius) {
        Component name = typeOf(boss).map(BossType::displayName).orElse(boss.getName());
        for (Player p : boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(radius), Player::isAlive)) {
            p.displayClientMessage(Component.translatable(key, name).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /**
     * Spawns one helper for a boss. Returns null if the cap is reached, nothing matches the spec,
     * or the spot is blocked. {@code lifetime} above 0 removes the helper after that many ticks.
     */
    @Nullable
    private static Mob spawnMinion(ServerLevel level, Mob boss, String spec, Vec3 at, int lifetime, boolean ignoreCap,
                                   String... extraTags) {
        if (!ignoreCap && tagged(boss, MINION_TAG, 32).size() >= TSCConfig.MAX_MINIONS.get()) return null;
        EntityType<?> type;
        if ("pool".equalsIgnoreCase(spec)) {
            List<EntityType<?>> pool = EntityResolver.latexPool();
            type = pool.isEmpty() ? null : pool.get(level.random.nextInt(pool.size()));
        } else {
            type = EntityResolver.resolve(spec).orElse(null);
        }
        if (type == null) return null;
        if (!level.noCollision(type.getAABB(at.x, at.y, at.z))) return null;

        String[] tags = new String[extraTags.length + 1];
        tags[0] = MINION_TAG;
        System.arraycopy(extraTags, 0, tags, 1, extraTags.length);
        Entity entity = EntitySpawning.spawn(level, type, BlockPos.containing(at), MobSpawnType.MOB_SUMMONED, false, tags);
        if (!(entity instanceof Mob minion)) {
            if (entity != null) entity.discard();
            return null;
        }
        if (lifetime > 0) minion.getPersistentData().putLong(EXPIRE, level.getGameTime() + lifetime);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 10, 0.3, 0.5, 0.3, 0.01);
        return minion;
    }

    private static void updateBarPlayers(ServerLevel level, Mob mob, ServerBossEvent bar) {
        List<ServerPlayer> inRange = level.getPlayers(p -> p.isAlive() && p.distanceToSqr(mob) < 48 * 48);
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
            if (!inRange.contains(p)) bar.removePlayer(p);
        }
        for (ServerPlayer p : inRange) bar.addPlayer(p);
    }

    private static List<Player> playersNear(Mob mob, double radius) {
        return mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    // ------------------------------------------------------------------ abilities

    private static void inkBurst(ServerLevel level, Mob mob) {
        for (Player p : playersNear(mob, 10)) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), mob);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), mob);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, mob.getX(), mob.getY() + 1.0, mob.getZ(), 80, 2.5, 1.0, 2.5, 0.05);
        level.playSound(null, mob.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private static void tentaclePull(Mob mob) {
        Player target = mob.level().getNearestPlayer(mob, 12.0);
        if (target == null || target.isCreative() || target.isSpectator()) return;
        Vec3 dir = mob.position().subtract(target.position()).normalize().scale(0.9);
        target.setDeltaMovement(target.getDeltaMovement().add(dir.x, 0.35, dir.z));
        target.hurtMarked = true;
    }

    private static void elderPulse(ServerLevel level, Mob mob) {
        for (Player p : playersNear(mob, 14)) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0), mob);
            p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0), mob);
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 40, 3.0, 1.5, 3.0, 0.02);
        level.playSound(null, mob.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    private static void shockwave(ServerLevel level, Mob mob, float damage) {
        for (Player p : playersNear(mob, 6)) {
            p.hurt(level.damageSources().mobAttack(mob), damage);
            p.knockback(1.6, mob.getX() - p.getX(), mob.getZ() - p.getZ());
            p.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 2.5, 0.3, 2.5, 0.0);
        level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    private static void summonMinions(ServerLevel level, Mob boss, BossType type, int count) {
        String spec = TSCConfig.BOSS_MINIONS.get(type).get();
        for (int i = 0; i < count; i++) {
            Mob minion = spawnMinion(level, boss, spec, around(level, boss, 3), 0, false);
            if (minion != null && boss.getTarget() != null) minion.setTarget(boss.getTarget());
        }
    }

    // ------------------------------------------------------------------ death / cleanup

    public static void onBossDeath(LivingEntity boss) {
        removeBar(boss);
        if (!(boss.level() instanceof ServerLevel level)) return;
        BossType type = typeOf(boss).orElse(null);
        if (type == null) return;
        boss.spawnAtLocation(new ItemStack(ModItems.coreFor(type)));
        boss.spawnAtLocation(new ItemStack(Items.DIAMOND, 2 + level.random.nextInt(4)));
        boss.spawnAtLocation(new ItemStack(ModItems.FIRST_AID_KIT.get(), 2));
        boss.spawnAtLocation(new ItemStack(ModItems.BULLET.get(), 32));
        ExperienceOrb.award(level, boss.position(), 300);
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(boss) < 64 * 64)) {
            p.sendSystemMessage(Component.translatable("message.changed_tsc.boss_defeated", type.displayName())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    public static void removeBar(Entity e) {
        STRIKES.remove(e.getUUID());
        ServerBossEvent bar = BARS.remove(e.getUUID());
        if (bar != null) bar.removeAllPlayers();
    }

    public static void clearAll() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        STRIKES.clear();
    }
}
