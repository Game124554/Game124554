package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.network.ClientGooData;
import com.thunderscience.changedtsc.network.TSCNetwork;
import com.thunderscience.changedtsc.network.TreatPacket;
import com.thunderscience.changedtsc.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * The body screen (default key G): a big paper doll of your body, human or transfurred, with the
 * goo marked on every part, and buttons that use a Bandage, First Aid Kit or Anti-Goo Syringe from
 * your inventory to clear the goo. Hover a body part to read its state.
 */
public class GooBodyScreen extends Screen {
    private static final String PREFIX = "screen.changed_tsc.body.";
    private static final int SCALE = 4;

    private Button bandage;
    private Button kit;
    private Button syringe;
    private int dollX;
    private int dollY;
    private int panelX;

    public GooBodyScreen() {
        super(Component.translatable(PREFIX + "title"));
    }

    @Override
    protected void init() {
        int dollWidth = PaperDoll.WIDTH * SCALE;
        int total = dollWidth + 20 + 170;
        this.dollX = (this.width - total) / 2;
        this.dollY = Math.max(34, this.height / 2 - PaperDoll.HEIGHT * SCALE / 2) + PaperDoll.EARS * SCALE / 2;
        this.panelX = this.dollX + dollWidth + 20;

        int top = this.dollY + 58;
        this.bandage = this.addRenderableWidget(Button.builder(Component.empty(), b -> treat(TreatPacket.BANDAGE))
                .bounds(this.panelX, top, 170, 20).build());
        this.kit = this.addRenderableWidget(Button.builder(Component.empty(), b -> treat(TreatPacket.KIT))
                .bounds(this.panelX, top + 24, 170, 20).build());
        this.syringe = this.addRenderableWidget(Button.builder(Component.empty(), b -> treat(TreatPacket.SYRINGE))
                .bounds(this.panelX, top + 48, 170, 20).build());
        this.bandage.setTooltip(Tooltip.create(Component.translatable(PREFIX + "bandage.tip")));
        this.kit.setTooltip(Tooltip.create(Component.translatable(PREFIX + "kit.tip")));
        this.syringe.setTooltip(Tooltip.create(Component.translatable(PREFIX + "syringe.tip")));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
        refreshButtons();
    }

    private void treat(int kind) {
        TSCNetwork.CHANNEL.sendToServer(new TreatPacket(kind));
    }

    @Override
    public void tick() {
        refreshButtons();
    }

    private void refreshButtons() {
        if (this.minecraft == null || this.minecraft.player == null || this.bandage == null) return;
        Player player = this.minecraft.player;
        boolean creative = player.getAbilities().instabuild;
        boolean goo = ClientGooData.selfPercent() > 0.0F;
        boolean hurt = player.getHealth() < player.getMaxHealth();
        boolean firstAid = TSCConfig.FIRST_AID_ENABLED.get();

        update(this.bandage, "bandage", ModItems.BANDAGE.get(), player, creative, firstAid && (goo || hurt),
                TSCConfig.GOO_BANDAGE_CURE.get());
        update(this.kit, "kit", ModItems.FIRST_AID_KIT.get(), player, creative, firstAid && (goo || hurt),
                TSCConfig.GOO_KIT_CURE.get());
        update(this.syringe, "syringe", ModItems.ANTI_GOO_SYRINGE.get(), player, creative, goo, 100.0);
    }

    private void update(Button button, String key, Item item, Player player, boolean creative, boolean useful, double cure) {
        int count = player.getInventory().countItem(item);
        button.setMessage(Component.translatable(PREFIX + key, count, Math.round(cure)));
        button.active = useful && (count > 0 || creative) && !player.getCooldowns().isOnCooldown(item);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        float goo = ClientGooData.selfPercent();
        float percent = PaperDoll.percent(player);
        boolean transfurred = PaperDoll.transfurred(player);
        boolean white = ClientGooData.selfWhite();

        graphics.fill(this.dollX - 8, this.dollY - PaperDoll.EARS * SCALE - 8,
                this.dollX + PaperDoll.WIDTH * SCALE + 8, this.dollY + PaperDoll.HEIGHT * SCALE + 8, 0x80000000);
        PaperDoll.draw(graphics, this.dollX, this.dollY, SCALE, percent, white, transfurred);

        // status lines
        int color = PaperDollHud.stageColor(percent, transfurred);
        String form = transfurred ? "transfurred" : percent >= TSCConfig.GOO_STAGE3_AT.get() ? "transfurring"
                : percent >= TSCConfig.GOO_STAGE2_AT.get() ? "furry" : percent > 0.0F ? "inside" : "human";
        int y = this.dollY;
        graphics.drawString(this.font, Component.translatable(PREFIX + "form." + form), this.panelX, y, color);
        graphics.drawString(this.font, Component.translatable(PREFIX + "goo", Math.round(goo)), this.panelX, y + 13, 0xFFFFFF);
        float changed = PaperDoll.changedPercent(player);
        if (changed > 0.0F && !transfurred) {
            graphics.drawString(this.font, Component.translatable(PREFIX + "changed_progress", Math.round(changed)),
                    this.panelX, y + 26, 0xC77DFF);
        }
        graphics.drawString(this.font, Component.translatable(PREFIX + "hint." + form), this.panelX, y + 41, 0xA0A0A0);

        super.render(graphics, mouseX, mouseY, partialTick);

        PaperDoll.Part part = PaperDoll.partAt(this.dollX, this.dollY, SCALE, mouseX, mouseY);
        if (part != null) {
            PaperDoll.State state = PaperDoll.state(part, percent, transfurred);
            String stateKey = switch (state) {
                case HUMAN -> "human";
                case GOO_INSIDE -> "inside";
                case FURRY -> part.paw ? "paw" : "furry";
            };
            graphics.renderTooltip(this.font, Component.translatable(PREFIX + "part_line",
                    Component.translatable(PREFIX + "part." + part.id),
                    Component.translatable(PREFIX + "state." + stateKey)), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
