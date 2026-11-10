package net.latexdrones.client;

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
                            + Component.translatable("form.latexdrones." + def.name).getString();
                    int tw = mc.font.width(tag);
                    graphics.fill(w / 2 - tw / 2 - 4, 3, w / 2 + tw / 2 + 4, 16, 0x90000000);
                    graphics.drawCenteredString(mc.font, tag, w / 2, 6, 0xFF000000 | def.glowColor);
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
