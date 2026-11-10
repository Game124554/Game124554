package net.latexdrones.client;

import net.latexdrones.drone.DroneLife;
import net.latexdrones.drone.DroneTask;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.latexdrones.LatexDrones;
import net.latexdrones.content.MobDef;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Screen overlays: infection meter + creeping vignette, the transformation "drip", drone HUD tag, hypnosis spiral. */
public final class HudOverlay {
    private HudOverlay() {
    }

    private static final ResourceLocation SPIRAL = LatexDrones.id("textures/gui/hypnosis_spiral.png");
    private static final ResourceLocation VIGNETTE = LatexDrones.id("textures/gui/infection_vignette.png");
    private static final ResourceLocation DRIP = LatexDrones.id("textures/gui/transform_drip.png");
    private static final int GOO = 0xFF0A0A0E;

    public static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float time = player.tickCount + tickDelta;
        InfectionData.Entry e = InfectionData.CLIENT.get(player.getUUID());

        if (e != null) {
            if (e.transformTicks > 0) {
                // black goo runs down the screen until nothing is left
                float t = Mth.clamp((e.transformTicks + tickDelta) / InfectionData.TRANSFORM_TICKS, 0.0F, 1.0F);
                fullscreen(graphics, VIGNETTE, w, h, Math.min(1.0F, 0.45F + t));
                int top = (int) (h * 1.75F * t) - h;
                begin(1.0F, graphics);
                graphics.blit(DRIP, 0, top, 0, 0.0F, 0.0F, w, h, w, h);
                end(graphics);
                if (top > 0) {
                    graphics.fill(0, 0, w, top, GOO);
                }
            } else if (e.transformed) {
                MobDef def = ModEntities.form(e.form);
                if (def != null) {
                    String tag = Component.translatable("hud.latexdrones.unit", String.valueOf(e.unit)).getString() + "  "
                            + Component.translatable("rank.latexdrones." + DroneLife.RANKS[DroneLife.rank(e.tasksDone)]).getString() + "  "
                            + Component.translatable("form.latexdrones." + def.name).getString();
                    String stats = Component.translatable("hud.latexdrones.energy", e.energy).getString() + "   "
                            + Component.translatable("hud.latexdrones.obedience", e.obedience).getString();
                    DroneTask task = DroneTask.byId(e.task);
                    String order;
                    int orderColor;
                    if (task == DroneTask.NONE) {
                        order = Component.translatable("hud.latexdrones.no_order").getString();
                        orderColor = 0xFFA0A0A8;
                    } else {
                        order = Component.translatable("hud.latexdrones.task", DroneLife.describe(e), e.taskProgress, e.taskGoal,
                                DroneLife.formatTime(e.taskTicks)).getString();
                        orderColor = e.taskTicks < 600 ? 0xFFFF8060 : 0xFFFFE070;
                    }
                    int tw = Math.max(mc.font.width(tag), Math.max(mc.font.width(order), mc.font.width(stats)));
                    graphics.fill(w / 2 - tw / 2 - 4, 3, w / 2 + tw / 2 + 4, 38, 0x90000000);
                    graphics.drawCenteredString(mc.font, tag, w / 2, 6, 0xFF000000 | def.glowColor);
                    int statColor = e.obedience < InfectionData.ROGUE ? 0xFFFF5050 : (e.energy < 20 ? 0xFFFFA040 : 0xFFB8E8FF);
                    graphics.drawCenteredString(mc.font, stats, w / 2, 17, statColor);
                    graphics.drawCenteredString(mc.font, order, w / 2, 28, orderColor);
                }
                if (e.standby) {
                    // standby = sleep: the screen powers down, a battery shows the charge
                    graphics.fill(0, 0, w, h, 0xC8050508);
                    float charge = Mth.clamp(e.energy / 100.0F, 0.0F, 1.0F);
                    int bw = 80;
                    int bx = w / 2 - bw / 2;
                    int by = h / 2 - 4;
                    graphics.fill(bx - 2, by - 2, bx + bw + 2, by + 12, 0xFF60646E);
                    graphics.fill(bx, by, bx + bw, by + 10, 0xFF101014);
                    graphics.fill(bx + bw + 2, by + 2, bx + bw + 5, by + 8, 0xFF60646E);
                    int pulse = (int) (40.0F * (0.5F + 0.5F * Mth.sin(time * 0.15F)));
                    graphics.fill(bx, by, bx + (int) (bw * charge), by + 10, 0xFF000000 | ((0x30 + pulse) << 16) | (0xC0 << 8) | 0xFF);
                    graphics.drawCenteredString(mc.font, Component.translatable("hud.latexdrones.standby", e.energy).getString(), w / 2, by - 18, 0xFF5FD8FF);
                    graphics.drawCenteredString(mc.font, Component.translatable("hud.latexdrones.standby_hint").getString(), w / 2, by + 30, 0xFF707884);
                }
            } else if (e.infection > 0) {
                float f = e.infection / (float) InfectionData.MAX;
                float pulse = 0.85F + 0.15F * Mth.sin(time * (0.10F + 0.25F * f));
                fullscreen(graphics, VIGNETTE, w, h, Math.min(1.0F, f * 0.95F) * pulse);
                // meter, top centre
                int bw = 122;
                int x = w / 2 - bw / 2;
                int y = 16;
                graphics.fill(x - 1, y - 1, x + bw + 1, y + 7, 0xC0000000);
                graphics.fill(x, y, x + bw, y + 6, 0xFF2A2A34);
                int fill = Math.max(1, (int) (bw * f));
                graphics.fill(x, y, x + fill, y + 6, f > 0.75F ? 0xFFB040FF : 0xFF7A35C9);
                graphics.fill(x, y, x + fill, y + 2, 0x50FFFFFF);
                String label = Component.translatable("hud.latexdrones.infection").getString() + " " + (int) (f * 100.0F) + "%";
                graphics.drawCenteredString(mc.font, label, w / 2, 5, f > 0.75F ? 0xFFFF70FF : 0xFFE0D0FF);
            }
        }

        if (ClientHooks.revealTicks > 0) {
            // fade back in from black after the transformation
            int alpha = (int) (255.0F * Mth.clamp((ClientHooks.revealTicks - tickDelta) / ClientHooks.REVEAL_LENGTH, 0.0F, 1.0F));
            graphics.fill(0, 0, w, h, (alpha << 24) | 0x0A0A0E);
        }

        if (player.hasEffect(ModEffects.HYPNOSIS)) {
            float alpha = 0.34F + 0.10F * Mth.sin(time * 0.15F);
            int size = (int) Math.sqrt((double) w * w + (double) h * h) + 2;
            begin(alpha, graphics);
            graphics.pose().pushPose();
            graphics.pose().translate(w / 2.0F, h / 2.0F, 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(time * 4.0F));
            graphics.blit(SPIRAL, -size / 2, -size / 2, 0, 0.0F, 0.0F, size, size, size, size);
            graphics.pose().popPose();
            end(graphics);
        }
    }

    private static void fullscreen(GuiGraphics graphics, ResourceLocation texture, int w, int h, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        begin(alpha, graphics);
        graphics.blit(texture, 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
        end(graphics);
    }

    private static void begin(float alpha, GuiGraphics graphics) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    private static void end(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
