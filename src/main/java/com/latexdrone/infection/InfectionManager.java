package com.latexdrone.infection;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.content.LatexForm;
import com.latexdrone.registry.ModEffects;
import com.latexdrone.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

/**
 * Server-side infection logic.
 * 0-30%  : fades slowly on its own
 * 30-60% : stable, heartbeat
 * 60-100%: grows on its own (point of no return), nausea
 * 100%   : 5-second transformation, then you ARE that latex creature
 */
public final class InfectionManager {
    public static final Identifier SYNC_PACKET = LatexDroneMod.id("sync");
    public static final int TRANSFORM_TIME = 100;

    private static final StatusEffect[] ABILITY_EFFECTS = {
            StatusEffects.NIGHT_VISION, StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.WATER_BREATHING,
            StatusEffects.DOLPHINS_GRACE, StatusEffects.RESISTANCE, StatusEffects.STRENGTH, StatusEffects.SLOW_FALLING,
            StatusEffects.HASTE};

    private InfectionManager() {}

    public static LatexPlayerAccess data(PlayerEntity p) {
        return (LatexPlayerAccess) p;
    }

    /** True for players who are latex (or mid-transformation). Used so latex mobs ignore them. */
    public static boolean isLatex(LivingEntity e) {
        if (e instanceof PlayerEntity p) {
            LatexPlayerAccess d = data(p);
            return d.latexdrone$isTransformed() || d.latexdrone$getTransformTicks() > 0;
        }
        return false;
    }

    public static void addInfection(ServerPlayerEntity p, float amount, int formIndex) {
        if (p.isCreative() || p.isSpectator() || isLatex(p)) return;
        LatexPlayerAccess d = data(p);
        if (formIndex >= 0) d.latexdrone$setPendingForm(formIndex);
        d.latexdrone$setInfection(d.latexdrone$getInfection() + amount);
    }

    public static void reduceInfection(ServerPlayerEntity p, float amount) {
        LatexPlayerAccess d = data(p);
        if (d.latexdrone$isTransformed() || d.latexdrone$getTransformTicks() > 0) return;
        d.latexdrone$setInfection(d.latexdrone$getInfection() - amount);
    }

    public static void startTransformation(ServerPlayerEntity p, int formIndex) {
        LatexPlayerAccess d = data(p);
        if (d.latexdrone$getTransformTicks() > 0) return;
        d.latexdrone$setPendingForm(formIndex);
        d.latexdrone$setInfection(100f);
        d.latexdrone$setTransformTicks(TRANSFORM_TIME);
        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.TRANSFORM, SoundCategory.PLAYERS, 1.0f, 1.0f);
        p.sendMessage(Text.translatable("message.latexdrone.transforming").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC), true);
        sync(p);
    }

    private static void completeTransformation(ServerPlayerEntity p) {
        LatexPlayerAccess d = data(p);
        LatexForm form = LatexForm.get(d.latexdrone$getPendingForm());
        d.latexdrone$setTransformed(true);
        d.latexdrone$setForm(form.index());
        d.latexdrone$setTransformTicks(0);
        d.latexdrone$setInfection(0f);
        p.removeStatusEffect(ModEffects.LATEX_INFECTION);
        p.removeStatusEffect(StatusEffects.NAUSEA);
        p.removeStatusEffect(StatusEffects.SLOWNESS);
        p.removeStatusEffect(StatusEffects.BLINDNESS);
        p.setHealth(p.getMaxHealth());
        if (p.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getBodyY(0.5), p.getZ(), 60, 0.5, 0.9, 0.5, 0.1);
            sw.spawnParticles(ParticleTypes.WITCH, p.getX(), p.getBodyY(0.5), p.getZ(), 30, 0.5, 0.9, 0.5, 0.1);
        }
        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.POD_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.8f);
        p.sendMessage(Text.translatable("message.latexdrone.transformed", Text.translatable("entity.latexdrone." + form.id()))
                .formatted(Formatting.LIGHT_PURPLE), false);
        sync(p);
    }

    /** Antidote: back to human. */
    public static void cure(ServerPlayerEntity p) {
        LatexPlayerAccess d = data(p);
        boolean wasLatex = d.latexdrone$isTransformed();
        d.latexdrone$setTransformed(false);
        d.latexdrone$setTransformTicks(0);
        d.latexdrone$setInfection(0f);
        p.removeStatusEffect(ModEffects.LATEX_INFECTION);
        p.removeStatusEffect(ModEffects.HYPNOSIS);
        p.removeStatusEffect(StatusEffects.NAUSEA);
        if (wasLatex) {
            for (StatusEffect e : ABILITY_EFFECTS) p.removeStatusEffect(e);
            p.sendMessage(Text.translatable("message.latexdrone.cured").formatted(Formatting.AQUA), false);
        }
        sync(p);
    }

    public static void tick(ServerPlayerEntity p) {
        LatexPlayerAccess d = data(p);
        int ticks = d.latexdrone$getTransformTicks();
        ServerWorld world = (ServerWorld) p.getWorld();

        if (ticks > 0) {
            // ---- the transformation sequence ----
            ticks--;
            d.latexdrone$setTransformTicks(ticks);
            float progress = 1f - ticks / (float) TRANSFORM_TIME;
            LatexForm form = LatexForm.get(d.latexdrone$getPendingForm());
            int c = form.primary();
            Vector3f color = new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
            world.spawnParticles(new DustParticleEffect(color, 1.6f), p.getX(), p.getY() + progress * 1.9, p.getZ(),
                    6, 0.35, 0.15, 0.35, 0.0);
            if (p.age % 4 == 0) {
                world.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getBodyY(0.5), p.getZ(), 3, 0.3, 0.6, 0.3, 0.02);
            }
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 4, false, false, false));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 4, false, false, false));
            if (ticks == 80 || ticks == 55 || ticks == 30) {
                p.playSound(ModSounds.HEARTBEAT, SoundCategory.PLAYERS, 1.0f, 1.0f + progress * 0.4f);
            }
            if (ticks == 0) completeTransformation(p);
            else if (ticks % 10 == 0) sync(p);
            return;
        }

        if (d.latexdrone$isTransformed()) {
            // ---- latex form perks ----
            if (p.age % 40 == 0) applyFormPerks(p, LatexForm.get(d.latexdrone$getForm()));
            return;
        }

        float inf = d.latexdrone$getInfection();
        if (inf > 0f) {
            if (inf < 30f) inf -= 0.01f;
            else if (inf >= 60f) inf += 0.02f;
            d.latexdrone$setInfection(inf);

            if (p.age % 20 == 0) {
                p.addStatusEffect(new StatusEffectInstance(ModEffects.LATEX_INFECTION, 45, Math.min(3, (int) (inf / 25f)), false, false, true));
                if (inf >= 40f) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 45, 0, false, false, false));
                if (inf >= 75f) p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, false, false, false));
            }
            if (inf >= 50f && p.age % 60 == 0) {
                p.playSound(ModSounds.HEARTBEAT, SoundCategory.PLAYERS, 0.7f, 1.0f);
            }
            if (p.age % 10 == 0 && inf >= 40f) {
                world.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getBodyY(0.3), p.getZ(), 1, 0.2, 0.3, 0.2, 0.0);
            }
            if (inf >= 100f) {
                startTransformation(p, d.latexdrone$getPendingForm());
                return;
            }
        }

        int rounded = (int) d.latexdrone$getInfection();
        if (rounded != d.latexdrone$getLastSent() && p.age % 5 == 0) sync(p);
    }

    private static void applyFormPerks(ServerPlayerEntity p, LatexForm form) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, true, false, true));
        switch (form.ability()) {
            case "speed" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0, true, false, true));
            case "jump" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 100, 1, true, false, true));
            case "water" -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 100, 0, true, false, true));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 100, 0, true, false, true));
            }
            case "resist" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0, true, false, true));
            case "strength" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 0, true, false, true));
            case "slowfall" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0, true, false, true));
            case "haste" -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 1, true, false, true));
            default -> { }
        }
    }

    // ---------------- networking ----------------

    private static PacketByteBuf write(ServerPlayerEntity p) {
        LatexPlayerAccess d = data(p);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(p.getUuid());
        buf.writeFloat(d.latexdrone$getInfection());
        buf.writeBoolean(d.latexdrone$isTransformed());
        buf.writeVarInt(d.latexdrone$getForm());
        buf.writeVarInt(d.latexdrone$getPendingForm());
        buf.writeVarInt(d.latexdrone$getTransformTicks());
        return buf;
    }

    /** Sends this player's state to everyone (so others see your latex skin). */
    public static void sync(ServerPlayerEntity p) {
        if (p.getServer() == null) return;
        data(p).latexdrone$setLastSent((int) data(p).latexdrone$getInfection());
        for (ServerPlayerEntity other : p.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(other, SYNC_PACKET, write(p));
        }
    }

    public static void onJoin(ServerPlayerEntity joined) {
        if (joined.getServer() == null) return;
        for (ServerPlayerEntity other : joined.getServer().getPlayerManager().getPlayerList()) {
            if (other != joined) ServerPlayNetworking.send(joined, SYNC_PACKET, write(other));
        }
        sync(joined);
    }

    /** Respawn: latex form is permanent until cured; plain infection is lost on death. */
    public static void copyFrom(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        LatexPlayerAccess o = data(oldPlayer), n = data(newPlayer);
        n.latexdrone$setTransformed(o.latexdrone$isTransformed());
        n.latexdrone$setForm(o.latexdrone$getForm());
        n.latexdrone$setPendingForm(o.latexdrone$getPendingForm());
        if (alive) {
            n.latexdrone$setInfection(o.latexdrone$getInfection());
            n.latexdrone$setTransformTicks(o.latexdrone$getTransformTicks());
        }
    }
}
