package com.thunderscience.changedtsc.infection;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.compat.FirstAidCompat;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.entity.AbstractTSCHuman;
import com.thunderscience.changedtsc.network.TSCNetwork;
import com.thunderscience.changedtsc.registry.ModEffects;
import com.thunderscience.changedtsc.util.EntityResolver;
import com.thunderscience.changedtsc.util.EntitySpawning;
import com.thunderscience.changedtsc.util.LatexHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

/**
 * Goo infection: latex hits put goo inside the body. It grows through three stages and, at 100%,
 * the victim transfurs.
 * <pre>
 *   1  Goo Inside Body  – still a human body, goo moving inside. Slowly fights it off.
 *   2  Furry Body       – fur and goo spread over the skin. The goo now grows by itself.
 *   3  Transfurring     – almost gone. Grows fast. Use first aid NOW.
 *   100%                – transfur (Changed's /transfur command, or the configured fallback).
 * </pre>
 * Works on players and on TSC humans. Stored in the entity's persistent data, so it survives
 * relogging; dying clears it.
 */
public final class GooInfection {
    private static final String KEY = "changed_tsc_goo";
    private static final String STAGE_KEY = "changed_tsc_goo_stage";
    private static final String WHITE_KEY = "changed_tsc_goo_white";
    private static final String SOURCE_KEY = "changed_tsc_goo_source";
    private static final String LAST_SHOWN_KEY = "changed_tsc_goo_shown";
    private static final String SENT_KEY = "changed_tsc_goo_sent";

    public static final ResourceKey<DamageType> GOO_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ChangedTSC.MODID, "goo_transfur"));

    private GooInfection() {}

    // ------------------------------------------------------------------ queries

    public static boolean isHost(Entity e) {
        return e instanceof Player || e instanceof AbstractTSCHuman;
    }

    public static boolean canBeInfected(LivingEntity e) {
        if (!TSCConfig.GOO_ENABLED.get()) return false;
        if (e instanceof Player p) return TSCConfig.GOO_INFECT_PLAYERS.get() && !p.isCreative() && !p.isSpectator();
        return e instanceof AbstractTSCHuman && TSCConfig.GOO_INFECT_HUMANS.get();
    }

    public static float get(LivingEntity e) {
        return isHost(e) ? e.getPersistentData().getFloat(KEY) : 0.0F;
    }

    public static int stage(LivingEntity e) {
        return stageFor(get(e));
    }

    public static boolean isWhiteGoo(LivingEntity e) {
        return isHost(e) && e.getPersistentData().getBoolean(WHITE_KEY);
    }

    public static int stageFor(float value) {
        if (value <= 0.0F) return 0;
        if (value >= TSCConfig.GOO_STAGE3_AT.get()) return 3;
        if (value >= TSCConfig.GOO_STAGE2_AT.get()) return 2;
        return 1;
    }

    // ------------------------------------------------------------------ changes

    /** Add goo after a hit from a latex. */
    public static void infect(LivingEntity victim, Entity source, float amount) {
        if (amount <= 0.0F || !canBeInfected(victim)) return;
        CompoundTag tag = victim.getPersistentData();
        ResourceLocation sourceId = EntityType.getKey(source.getType());
        tag.putString(SOURCE_KEY, sourceId.toString());
        tag.putBoolean(WHITE_KEY, sourceId.getPath().contains("white"));
        setRaw(victim, get(victim) + amount);
        showPercent(victim);
        update(victim, false);
    }

    /** Remove goo (first aid). Returns the amount actually removed. */
    public static float cure(LivingEntity e, float amount) {
        float before = get(e);
        if (before <= 0.0F || amount <= 0.0F) return 0.0F;
        setRaw(e, before - amount);
        if (get(e) <= 0.0F && e instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.changed_tsc.goo.cured").withStyle(ChatFormatting.GREEN), true);
        } else {
            showPercent(e);
        }
        update(e, false);
        return before - get(e);
    }

    /** Admin / command use. */
    public static void set(LivingEntity e, float value) {
        if (!isHost(e)) return;
        setRaw(e, value);
        update(e, false);
    }

    private static void setRaw(LivingEntity e, float value) {
        e.getPersistentData().putFloat(KEY, Mth.clamp(value, 0.0F, 100.0F));
    }

    private static void showPercent(LivingEntity e) {
        if (e instanceof Player p) {
            int pct = Math.round(get(e));
            ChatFormatting color = pct >= TSCConfig.GOO_STAGE3_AT.get() ? ChatFormatting.DARK_RED
                    : pct >= TSCConfig.GOO_STAGE2_AT.get() ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            p.displayClientMessage(Component.translatable("message.changed_tsc.goo.status", pct).withStyle(color), true);
        }
    }

    // ------------------------------------------------------------------ ticking (once per second)

    public static void tickSecond(LivingEntity e) {
        float value = get(e);
        if (value <= 0.0F) return;
        if (!TSCConfig.GOO_ENABLED.get()) {
            set(e, 0.0F);
            return;
        }
        if (!canBeInfected(e)) return; // creative players: frozen, not cured
        int stage = stageFor(value);
        float delta = switch (stage) {
            case 1 -> -TSCConfig.GOO_RECOVERY_STAGE1.get().floatValue();
            case 2 -> TSCConfig.GOO_SPREAD_STAGE2.get().floatValue();
            case 3 -> TSCConfig.GOO_SPREAD_STAGE3.get().floatValue();
            default -> 0.0F;
        };
        // First Aid mod: morphine holds the goo back and numbs the symptoms
        boolean numbed = FirstAidCompat.morphineActive(e);
        if (numbed && delta > 0.0F) delta = 0.0F;
        setRaw(e, value + delta);
        if (TSCConfig.GOO_SYMPTOMS.get() && !numbed) symptoms(e, stage);
        update(e, true);

        // show the % every 10 seconds while it is spreading
        if (stage >= 2 && e instanceof Player && e.tickCount % 200 < 20) showPercent(e);
    }

    /** Apply the stage effect, sync the overlay, and (from the tick only) transfur at 100%. */
    private static void update(LivingEntity e, boolean allowTransfur) {
        float value = get(e);
        int stage = stageFor(value);
        CompoundTag tag = e.getPersistentData();
        int old = tag.getInt(STAGE_KEY);
        if (stage != old) {
            tag.putInt(STAGE_KEY, stage);
            TSCNetwork.sendGooStage(e, stage, isWhiteGoo(e));
            if (e instanceof Player p && stage > old) {
                p.displayClientMessage(Component.translatable("message.changed_tsc.goo.stage" + stage)
                        .withStyle(stage == 3 ? ChatFormatting.DARK_RED : stage == 2 ? ChatFormatting.GOLD : ChatFormatting.GRAY), false);
                e.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8F, 0.6F);
            }
        }
        applyStageEffect(e, stage);
        syncPercent(e, false);
        if (allowTransfur && value >= 100.0F) transfur(e);
    }

    /** Keeps the player's paper doll up to date (sent in half-percent steps, not every tick). */
    private static void syncPercent(LivingEntity e, boolean force) {
        if (!(e instanceof ServerPlayer player)) return;
        float value = get(e);
        int step = Math.round(value * 2.0F) + 1;
        CompoundTag tag = e.getPersistentData();
        if (!force && tag.getInt(SENT_KEY) == step) return;
        tag.putInt(SENT_KEY, step);
        TSCNetwork.sendGooPercent(player, value, isWhiteGoo(e));
    }

    private static void applyStageEffect(LivingEntity e, int stage) {
        MobEffect want = switch (stage) {
            case 1 -> ModEffects.GOO_INSIDE.get();
            case 2 -> ModEffects.FURRY_BODY.get();
            case 3 -> ModEffects.TRANSFURRING.get();
            default -> null;
        };
        for (MobEffect effect : List.of(ModEffects.GOO_INSIDE.get(), ModEffects.FURRY_BODY.get(), ModEffects.TRANSFURRING.get())) {
            if (effect != want && e.hasEffect(effect)) e.removeEffect(effect);
        }
        if (want != null) e.addEffect(new MobEffectInstance(want, 60, 0, false, false, true));
    }

    private static void hidden(LivingEntity e, MobEffect effect, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(effect, ticks, amp, true, false, false));
    }

    private static void symptoms(LivingEntity e, int stage) {
        var rng = e.getRandom();
        Vector3f color = isWhiteGoo(e) ? new Vector3f(0.95F, 0.95F, 0.97F) : new Vector3f(0.06F, 0.06F, 0.08F);
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(color, 1.0F), e.getX(), e.getY() + e.getBbHeight() * 0.5,
                    e.getZ(), stage * 2, 0.25, 0.45, 0.25, 0.0);
        }
        switch (stage) {
            case 1 -> {
                if (rng.nextFloat() < 0.15F) hidden(e, MobEffects.HUNGER, 80, 0);
                if (rng.nextFloat() < 0.04F) message(e, "message.changed_tsc.goo.feel1");
            }
            case 2 -> {
                hidden(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 0);
                hidden(e, MobEffects.WEAKNESS, 40, 0);
                if (rng.nextFloat() < 0.04F) message(e, "message.changed_tsc.goo.feel2");
            }
            case 3 -> {
                hidden(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                hidden(e, MobEffects.WEAKNESS, 40, 1);
                if (rng.nextFloat() < 0.12F) hidden(e, MobEffects.CONFUSION, 120, 0);
                if (rng.nextFloat() < 0.08F) hidden(e, MobEffects.DARKNESS, 60, 0);
                if (rng.nextFloat() < 0.08F) message(e, "message.changed_tsc.goo.feel3");
            }
            default -> {}
        }
    }

    private static void message(LivingEntity e, String key) {
        if (!(e instanceof Player p)) return;
        long now = e.level().getGameTime();
        CompoundTag tag = e.getPersistentData();
        if (now - tag.getLong(LAST_SHOWN_KEY) < 300) return; // max one flavour message per 15 s
        tag.putLong(LAST_SHOWN_KEY, now);
        p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY), false);
    }

    // ------------------------------------------------------------------ transfur

    private static void transfur(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return;
        String sourceId = e.getPersistentData().getString(SOURCE_KEY);
        setRaw(e, 0.0F);
        e.getPersistentData().putInt(STAGE_KEY, 0);
        TSCNetwork.sendGooStage(e, 0, false);
        applyStageEffect(e, 0);
        syncPercent(e, true);

        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.07F), 2.0F),
                e.getX(), e.getY() + 1.0, e.getZ(), 60, 0.4, 0.8, 0.4, 0.0);

        Component name = e.getDisplayName();
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(e) < 48 * 48)) {
            p.sendSystemMessage(Component.translatable("message.changed_tsc.goo.transfurred", name).withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (e instanceof ServerPlayer player) {
            String cmd = TSCConfig.GOO_TRANSFUR_COMMAND.get();
            boolean done = false;
            if (!cmd.isBlank() && ModList.get().isLoaded("changed")) {
                String filled = fill(cmd, player, sourceId, 0.0F);
                if (filled.contains("{form}")) {
                    // try each way Changed has named its forms until one is accepted
                    for (String form : formCandidates(sourceId)) {
                        if (runCommand(player.server, filled.replace("{form}", form))) {
                            done = true;
                            break;
                        }
                    }
                } else {
                    done = runCommand(player.server, filled);
                }
            }
            if (done) return;
            switch (TSCConfig.GOO_TRANSFUR_FALLBACK.get().toLowerCase(Locale.ROOT)) {
                case "kill_and_spawn" -> {
                    spawnLatex(level, e, sourceId, player.getGameProfile().getName());
                    player.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(GOO_DAMAGE)), Float.MAX_VALUE);
                }
                case "spawn_only" -> {
                    spawnLatex(level, e, sourceId, player.getGameProfile().getName());
                    hidden(player, MobEffects.CONFUSION, 200, 0);
                }
                default -> {}
            }
        } else if (e instanceof AbstractTSCHuman human) {
            spawnLatex(level, human, sourceId, null);
            human.discard();
        }
    }

    private static void spawnLatex(ServerLevel level, LivingEntity at, String sourceId, String formerName) {
        EntityType<?> type = sourceId.isEmpty() ? null : EntityResolver.resolve(sourceId).orElse(null);
        if (type == null || !LatexHelper.isLatexType(type)) {
            List<EntityType<?>> pool = EntityResolver.latexPool();
            type = pool.isEmpty() ? null : pool.get(level.random.nextInt(pool.size()));
        }
        if (type == null) return;
        Entity latex = EntitySpawning.spawn(level, type, at.blockPosition(), MobSpawnType.CONVERSION, true);
        if (latex != null && formerName != null) {
            latex.setCustomName(Component.translatable("entity.changed_tsc.transfurred_name", formerName));
        }
    }

    // ------------------------------------------------------------------ Changed integration

    /** Ask Changed to lower its own transfur progress too (best effort; silent if the command is wrong). */
    public static void regressChanged(LivingEntity e, float curePercent) {
        if (!(e instanceof ServerPlayer player) || curePercent <= 0.0F) return;
        String cmd = TSCConfig.GOO_REGRESS_COMMAND.get();
        if (cmd.isBlank() || !ModList.get().isLoaded("changed")) return;
        runCommand(player.server, fill(cmd, player, "", curePercent / 10.0F));
    }

    /** Possible Changed form ids for the latex that infected the player, most likely first. */
    private static List<String> formCandidates(String sourceId) {
        List<String> out = new java.util.ArrayList<>();
        ResourceLocation src = sourceId.isEmpty() ? null : ResourceLocation.tryParse(sourceId);
        if (src == null || src.getPath().isEmpty()) return out;
        String ns = src.getNamespace();
        String path = src.getPath();
        out.add(ns + ":form_" + path);
        out.add(ns + ":" + path);
        for (String gender : new String[]{"male", "female"}) {
            if (path.endsWith("_" + gender)) {
                String base = path.substring(0, path.length() - gender.length() - 1);
                out.add(ns + ":form_" + base + "/" + gender);
                out.add(ns + ":" + base + "/" + gender);
            }
        }
        return out;
    }

    private static String fill(String template, ServerPlayer player, String sourceId, float amount) {
        ResourceLocation src = ResourceLocation.tryParse(sourceId);
        return template.replace("{player}", player.getGameProfile().getName())
                .replace("{entity}", sourceId)
                .replace("{entity_path}", src == null ? "" : src.getPath())
                .replace("{amount}", String.format(Locale.ROOT, "%.2f", amount));
    }

    private static boolean runCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            return server.getCommands().performPrefixedCommand(source, command) > 0;
        } catch (Exception ex) {
            ChangedTSC.LOGGER.debug("TSC command '{}' failed: {}", command, ex.toString());
            return false;
        }
    }

    // ------------------------------------------------------------------ sync helpers

    public static void syncTo(ServerPlayer viewer, LivingEntity target) {
        int stage = stage(target);
        if (stage > 0) TSCNetwork.sendGooStageTo(viewer, target, stage, isWhiteGoo(target));
    }

    public static void syncSelf(ServerPlayer player) {
        TSCNetwork.sendGooStageTo(player, player, stage(player), isWhiteGoo(player));
        syncPercent(player, true);
    }
}
