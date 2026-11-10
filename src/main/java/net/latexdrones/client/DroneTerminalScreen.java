package net.latexdrones.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.latexdrones.content.MobDef;
import net.latexdrones.drone.DroneLife;
import net.latexdrones.drone.DroneTask;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.ModNetworking;
import net.latexdrones.registry.ModEntities;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The hive terminal UI (opened by the Drone Terminal block or by talking to a scientist).
 * Shows the unit's status, energy, obedience and current order, with buttons to request / report /
 * refuse an order and to enter standby. All buttons are checked again on the server.
 */
public class DroneTerminalScreen extends Screen {
    private static final int W = 270;
    private static final int H = 196;
    private static final int CYAN = 0xFF35E0FF;

    public DroneTerminalScreen() {
        super(Component.translatable("gui.latexdrones.terminal"));
    }

    private InfectionData.Entry self() {
        return this.minecraft == null || this.minecraft.player == null ? null
                : InfectionData.CLIENT.get(this.minecraft.player.getUUID());
    }

    @Override
    protected void init() {
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;
        int bw = (W - 30) / 2;
        int y = top + H - 58;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.latexdrones.request"), b -> send(DroneLife.ACTION_REQUEST))
                .bounds(left + 10, y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.latexdrones.report"), b -> send(DroneLife.ACTION_REPORT))
                .bounds(left + 20 + bw, y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.latexdrones.refuse"), b -> send(DroneLife.ACTION_REFUSE))
                .bounds(left + 10, y + 24, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.latexdrones.standby"), b -> {
            send(DroneLife.ACTION_STANDBY);
            this.onClose();
        }).bounds(left + 20 + bw, y + 24, bw, 20).build());
    }

    private static void send(int action) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        ClientPlayNetworking.send(ModNetworking.TERMINAL_ACTION, buf);
    }

    @Override
    public void tick() {
        InfectionData.Entry e = this.self();
        if (e == null || !e.transformed) {
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void bar(GuiGraphics g, int x, int y, int w, int value, int color, String label) {
        g.drawString(this.font, label, x, y, 0xFFB8C4D0, false);
        int bx = x + 70;
        int bwid = w - 70 - 34;
        g.fill(bx - 1, y - 1, bx + bwid + 1, y + 9, 0xFF3A3F4A);
        g.fill(bx, y, bx + bwid, y + 8, 0xFF101218);
        g.fill(bx, y, bx + (int) (bwid * Mth.clamp(value / 100.0F, 0.0F, 1.0F)), y + 8, color);
        g.drawString(this.font, value + "%", bx + bwid + 6, y, 0xFFE0E8F0, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;
        // frame
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF35E0FF);
        g.fill(left, top, left + W, top + H, 0xF00A0E14);
        g.fill(left, top, left + W, top + 16, 0xFF12303C);
        g.drawCenteredString(this.font, this.title, left + W / 2, top + 4, CYAN);

        InfectionData.Entry e = this.self();
        if (e != null) {
            MobDef def = ModEntities.form(e.form);
            String name = def == null ? "?" : Component.translatable("form.latexdrones." + def.name).getString();
            String rank = Component.translatable("rank.latexdrones." + DroneLife.RANKS[DroneLife.rank(e.tasksDone)]).getString();
            g.drawString(this.font, Component.translatable("gui.latexdrones.unit", String.valueOf(e.unit), name).getString(),
                    left + 10, top + 22, 0xFFFFFFFF, false);
            String status;
            int statusColor;
            if (e.obedience < InfectionData.ROGUE) {
                status = Component.translatable("gui.latexdrones.status_rogue").getString();
                statusColor = 0xFFFF5050;
            } else if (e.obedience >= InfectionData.LOYAL) {
                status = Component.translatable("gui.latexdrones.status_loyal").getString();
                statusColor = 0xFFFF80FF;
            } else {
                status = Component.translatable("gui.latexdrones.status_normal").getString();
                statusColor = 0xFF80FFA0;
            }
            g.drawString(this.font, status + "  |  " + rank, left + 10, top + 34, statusColor, false);
            bar(g, left + 10, top + 50, W - 20, e.energy, e.energy < 20 ? 0xFFFF6040 : 0xFF35E0FF,
                    Component.translatable("gui.latexdrones.energy").getString());
            bar(g, left + 10, top + 62, W - 20, e.obedience, e.obedience < InfectionData.ROGUE ? 0xFFFF4040 : 0xFFB050FF,
                    Component.translatable("gui.latexdrones.obedience").getString());

            int ty = top + 80;
            g.fill(left + 8, ty - 3, left + W - 8, ty + 52, 0xFF141A24);
            g.drawString(this.font, Component.translatable("gui.latexdrones.task").getString(), left + 12, ty, 0xFF8090A0, false);
            DroneTask task = DroneTask.byId(e.task);
            if (task == DroneTask.NONE) {
                g.drawString(this.font, Component.translatable("task.latexdrones.none").getString(), left + 12, ty + 12, 0xFFA0A0A8, false);
            } else {
                g.drawString(this.font, DroneLife.describe(e), left + 12, ty + 12, 0xFFFFE070, false);
                String giver = e.taskGiver.isEmpty() || "hive".equals(e.taskGiver)
                        ? Component.translatable("giver.latexdrones.hive").getString()
                        : Component.translatable("entity.latexdrones." + e.taskGiver).getString();
                g.drawString(this.font, Component.translatable("gui.latexdrones.given_by", giver).getString(), left + 12, ty + 24, 0xFFB8C4D0, false);
                g.drawString(this.font, Component.translatable("gui.latexdrones.progress", e.taskProgress, e.taskGoal).getString()
                        + "    " + Component.translatable("gui.latexdrones.time_left", DroneLife.formatTime(e.taskTicks)).getString(),
                        left + 12, ty + 36, e.taskTicks < 600 ? 0xFFFF8060 : 0xFFE0E8F0, false);
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
