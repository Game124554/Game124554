package com.latexdrone.client;

import com.latexdrone.entity.EvilScientistEntity;
import com.latexdrone.registry.ModEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/** Client tick: hypnosis pulls your gaze toward the nearest scientist and whispers to you. */
public final class ClientEvents {
    private ClientEvents() {}

    public static void tick(MinecraftClient mc) {
        ClientLatexData.tick();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.isPaused()) return;
        if (!player.hasStatusEffect(ModEffects.HYPNOSIS)) return;

        List<EvilScientistEntity> sci = player.getWorld().getEntitiesByClass(EvilScientistEntity.class,
                player.getBoundingBox().expand(20.0), e -> e.isAlive());
        EvilScientistEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (EvilScientistEntity e : sci) {
            double d = e.squaredDistanceTo(player);
            if (d < best) { best = d; nearest = e; }
        }
        if (nearest != null) {
            double dx = nearest.getX() - player.getX();
            double dz = nearest.getZ() - player.getZ();
            double dy = nearest.getEyeY() - player.getEyeY();
            float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            float targetPitch = (float) (-(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
            player.setYaw(player.getYaw() + MathHelper.wrapDegrees(targetYaw - player.getYaw()) * 0.06f);
            player.setPitch(player.getPitch() + (targetPitch - player.getPitch()) * 0.06f);
        } else {
            player.setYaw(player.getYaw() + MathHelper.sin(player.age * 0.08f) * 0.5f);
        }
        if (player.age % 90 == 0) {
            int line = 1 + player.getRandom().nextInt(6);
            player.sendMessage(Text.translatable("message.latexdrone.hypno." + line).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC), true);
        }
    }
}
