package net.latexdrones.client;

import java.util.UUID;

import net.latexdrones.effect.HypnosisEffect;
import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/** Client-side state and per-tick effects (sync packet handling, hypnosis camera pull, heartbeat). */
public final class ClientHooks {
    private ClientHooks() {
    }

    /** Ticks left of the black "waking up as a drone" fade after a transformation completes. */
    public static int revealTicks;
    public static final int REVEAL_LENGTH = 40;

    /** Handles the sync packet (called on the network thread, so the work is handed to the client thread). */
    public static void receiveSync(Minecraft client, FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        int infection = buf.readVarInt();
        boolean transformed = buf.readBoolean();
        String form = buf.readUtf();
        int transformTicks = buf.readVarInt();
        int unit = buf.readVarInt();
        int task = buf.readVarInt();
        int taskProgress = buf.readVarInt();
        int taskGoal = buf.readVarInt();
        int tasksDone = buf.readVarInt();
        boolean standby = buf.readBoolean();
        client.execute(() -> {
            InfectionData.Entry old = InfectionData.CLIENT.get(id);
            InfectionData.Entry e = new InfectionData.Entry();
            e.infection = infection;
            e.transformed = transformed;
            e.form = form;
            e.unit = unit;
            e.task = task;
            e.taskProgress = taskProgress;
            e.taskGoal = taskGoal;
            e.tasksDone = tasksDone;
            e.standby = standby;
            // keep our own smoothly advancing counter while the sequence is running
            e.transformTicks = (transformTicks > 0 && old != null && old.transformTicks > 0) ? Math.max(old.transformTicks, 1) : transformTicks;
            InfectionData.CLIENT.put(id, e);
            LocalPlayer self = client.player;
            if (self != null && self.getUUID().equals(id) && transformed && old != null && !old.transformed) {
                revealTicks = REVEAL_LENGTH;
            }
        });
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.isPaused()) {
            return;
        }
        for (InfectionData.Entry e : InfectionData.CLIENT.values()) {
            if (e.transformTicks > 0 && e.transformTicks < InfectionData.TRANSFORM_TICKS) {
                e.transformTicks++;
            }
        }
        if (revealTicks > 0) {
            revealTicks--;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (player.hasEffect(ModEffects.HYPNOSIS)) {
            // "you can't look away": the camera drifts towards the nearest hypnotist, or sways if there is none
            LatexDroneEntity source = HypnosisEffect.nearestHypnotist(player, 16.0D);
            if (source != null) {
                double dx = source.getX() - player.getX();
                double dy = source.getEyeY() - player.getEyeY();
                double dz = source.getZ() - player.getZ();
                float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0D / Math.PI));
                player.setYRot(Mth.rotLerp(0.12F, player.getYRot(), yaw));
                player.setXRot(Mth.rotLerp(0.12F, player.getXRot(), pitch));
            } else {
                player.setYRot(player.getYRot() + Mth.sin(player.tickCount * 0.1F) * 0.8F);
            }
            if (player.tickCount % 48 == 0) {
                player.playSound(ModSounds.HYPNOSIS, 0.5F, 1.0F);
            }
        }
        InfectionData.Entry own = InfectionData.CLIENT.get(player.getUUID());
        if (own != null && !own.transformed && own.transformTicks <= 0 && own.infection >= InfectionData.MAX / 2) {
            // heartbeat that speeds up as the infection closes in
            int interval = own.infection >= 850 ? 16 : (own.infection >= 700 ? 26 : 40);
            if (player.tickCount % interval == 0) {
                player.playSound(ModSounds.HEARTBEAT, 0.7F, 1.0F);
            }
        }
    }
}
