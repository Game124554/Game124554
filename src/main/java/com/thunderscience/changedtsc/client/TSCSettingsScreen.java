package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.boss.BossType;
import com.thunderscience.changedtsc.compat.FirstAidCompat;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.util.EntityResolver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * In-game settings menu for the add-on. Open it with the "TSC Settings" button on the title screen
 * or pause menu, with the settings key (default O), or with "Config" in the Mods list.
 *
 * Every switch and slider writes straight into config/changed_tsc-common.toml, so the menu and the
 * file always agree. In single player the changes apply at once. On a multiplayer server the
 * server's own config file decides the gameplay rules.
 */
public class TSCSettingsScreen extends Screen {
    private enum Page {
        MOBS("mobs"), STEALTH("stealth"), GOO("goo"), FIRST_AID("first_aid"), DOLL("doll"), PURO("puro"), BOSSES("bosses");

        private final String id;

        Page(String id) {
            this.id = id;
        }
    }

    private record Label(Component text, int y, int color) {}

    private static final String PREFIX = "settings.changed_tsc.";
    private static final int WHITE = 0xFFFFFF;
    private static final int GRAY = 0xA0A0A0;
    private static final int GREEN = 0x55FF55;
    private static final int RED = 0xFF5555;

    /** The page stays selected while the game is running. */
    private static Page page = Page.MOBS;

    @Nullable
    private final Screen parent;
    private final List<Label> labels = new ArrayList<>();
    private int slot;
    private boolean dirty;

    public TSCSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable(PREFIX + "title"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        this.labels.clear();
        this.slot = 0;

        Page[] pages = Page.values();
        int tabWidth = Math.min(this.width - 12, pages.length * 76) / pages.length;
        int left = (this.width - tabWidth * pages.length) / 2;
        for (int i = 0; i < pages.length; i++) {
            Page target = pages[i];
            Button tab = Button.builder(Component.translatable(PREFIX + "page." + target.id), b -> openPage(target))
                    .bounds(left + i * tabWidth, 30, tabWidth - 2, 20).build();
            tab.active = target != page;
            this.addRenderableWidget(tab);
        }

        switch (page) {
            case MOBS -> {
                toggle("remove_vanilla_hostiles", TSCConfig.REMOVE_VANILLA_HOSTILES);
                toggle("remove_vanilla_bosses", TSCConfig.REMOVE_VANILLA_BOSSES);
                toggle("latex_hunt_humans", TSCConfig.LATEX_HUNT_HUMANS);
                toggle("npcs_fight_monsters", TSCConfig.NPCS_FIGHT_VANILLA_MONSTERS);
                toggle("scientists_heal", TSCConfig.SCIENTISTS_HEAL);
                toggle("guns_require_ammo", TSCConfig.GUNS_REQUIRE_AMMO);
                toggle("furniture_changed", TSCConfig.FURNITURE_USE_CHANGED);
                toggle("menu_button", TSCConfig.MENU_BUTTON);
            }
            case STEALTH -> {
                toggle("stealth_enabled", TSCConfig.STEALTH_ENABLED);
                toggle("stealth_vs_bosses", TSCConfig.STEALTH_VS_BOSSES);
                slider("sneak_notice_range", TSCConfig.SNEAK_NOTICE_RANGE, 0.0, 16.0, 0.5);
                slider("sneak_visibility", TSCConfig.SNEAK_VISIBILITY, 0.0, 1.0, 0.05);
                slider("darkness_multiplier", TSCConfig.DARKNESS_RANGE_MULTIPLIER, 0.0, 1.0, 0.05);
            }
            case GOO -> {
                toggle("goo_enabled", TSCConfig.GOO_ENABLED);
                toggle("goo_symptoms", TSCConfig.GOO_SYMPTOMS);
                toggle("goo_infect_players", TSCConfig.GOO_INFECT_PLAYERS);
                toggle("goo_infect_humans", TSCConfig.GOO_INFECT_HUMANS);
                toggle("goo_body_overlay", TSCConfig.GOO_BODY_OVERLAY);
                slider("goo_per_hit", TSCConfig.GOO_PER_HIT, 0.0, 50.0, 1.0);
                slider("goo_furry_at", TSCConfig.GOO_STAGE2_AT, 5.0, 90.0, 1.0);
                slider("goo_transfurring_at", TSCConfig.GOO_STAGE3_AT, 10.0, 99.0, 1.0);
                slider("goo_spread_stage2", TSCConfig.GOO_SPREAD_STAGE2, 0.0, 2.0, 0.05);
                slider("goo_spread_stage3", TSCConfig.GOO_SPREAD_STAGE3, 0.0, 2.0, 0.05);
            }
            case FIRST_AID -> {
                toggle("first_aid_enabled", TSCConfig.FIRST_AID_ENABLED);
                toggle("first_aid_cures_effects", TSCConfig.FIRST_AID_CURES_EFFECTS);
                slider("kit_heal", TSCConfig.FIRST_AID_HEAL, 0.0, 40.0, 1.0);
                slider("bandage_heal", TSCConfig.BANDAGE_HEAL, 0.0, 20.0, 1.0);
                slider("kit_goo_cure", TSCConfig.GOO_KIT_CURE, 0.0, 100.0, 1.0);
                slider("bandage_goo_cure", TSCConfig.GOO_BANDAGE_CURE, 0.0, 100.0, 1.0);
                boolean installed = FirstAidCompat.isLoaded();
                label(Component.translatable(PREFIX + (installed ? "first_aid_mod.installed" : "first_aid_mod.missing")),
                        installed ? GREEN : GRAY);
                toggle("fa_mod_compat", TSCConfig.FA_MOD_COMPAT).active = installed;
                toggle("fa_mod_loot", TSCConfig.FA_MOD_LOOT).active = installed;
                toggle("fa_mod_morphine", TSCConfig.FA_MOD_MORPHINE).active = installed;
                toggle("fa_mod_only", TSCConfig.FA_MOD_ONLY).active = installed;
            }
            case DOLL -> {
                toggle("doll_hud", TSCConfig.DOLL_HUD);
                toggle("doll_always", TSCConfig.DOLL_ALWAYS);
                toggle("doll_paws", TSCConfig.DOLL_PAWS);
                toggle("doll_changed", TSCConfig.DOLL_CHANGED_PROGRESS);
                choice("doll_position", TSCConfig.DOLL_POSITION, TSCConfig.DOLL_POSITIONS);
                intSlider("doll_scale", TSCConfig.DOLL_SCALE, 1, 4);
                label(Component.translatable(PREFIX + "doll_hint", ClientModEvents.OPEN_BODY.getTranslatedKeyMessage()), GRAY);
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "open_body"),
                                    b -> this.minecraft.setScreen(new GooBodyScreen()))
                            .bounds(this.width / 2 - 75, slotY(), 150, 20).build());
                    this.slot += 2;
                }
            }
            case PURO -> {
                toggle("puro_enabled", TSCConfig.FRIENDLY_PURO_ENABLED);
                toggle("puro_follow", TSCConfig.FRIENDLY_FOLLOW_PLAYERS);
                toggle("puro_fight", TSCConfig.FRIENDLY_FIGHT_HOSTILES);
                toggle("puro_retaliate", TSCConfig.FRIENDLY_RETALIATE);
                label(Component.translatable(PREFIX + "puro_hint"), GRAY);
            }
            case BOSSES -> {
                toggle("bosses_enabled", TSCConfig.BOSSES_ENABLED);
                intSlider("max_minions", TSCConfig.MAX_MINIONS, 0, 20);
                for (BossType type : BossType.values()) {
                    slider(Component.translatable(PREFIX + "boss_health", type.displayName()),
                            Component.translatable(PREFIX + "boss_health.tip"),
                            TSCConfig.BOSS_HEALTH.get(type), 20.0, 1000.0, 10.0);
                }
                label(Component.translatable(PREFIX + "bosses_hint"), GRAY);
            }
        }

        if (this.minecraft != null && this.minecraft.level != null && !this.minecraft.hasSingleplayerServer()) {
            this.labels.add(new Label(Component.translatable(PREFIX + "multiplayer_note"), this.height - 46, RED));
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void openPage(Page target) {
        page = target;
        this.clearWidgets();
        this.setFocused(null);
        this.init();
    }

    private int slotX() {
        return this.width / 2 - 155 + (this.slot % 2) * 160;
    }

    private int slotY() {
        return 58 + (this.slot / 2) * 24;
    }

    // ------------------------------------------------------------------ widgets

    private CycleButton<Boolean> toggle(String key, ForgeConfigSpec.BooleanValue value) {
        Component tip = Component.translatable(PREFIX + key + ".tip");
        CycleButton<Boolean> button = CycleButton.onOffBuilder(value.get())
                .withTooltip(state -> Tooltip.create(tip))
                .create(slotX(), slotY(), 150, 20, Component.translatable(PREFIX + key), (b, state) -> {
                    value.set(state);
                    this.dirty = true;
                });
        this.slot++;
        return this.addRenderableWidget(button);
    }

    /** A button that steps through a fixed list of choices. */
    private void choice(String key, ForgeConfigSpec.ConfigValue<String> value, List<String> options) {
        String current = options.contains(value.get()) ? value.get() : options.get(0);
        CycleButton<String> button = CycleButton.<String>builder(option -> Component.translatable(PREFIX + key + "." + option))
                .withValues(options)
                .withInitialValue(current)
                .withTooltip(option -> Tooltip.create(Component.translatable(PREFIX + key + ".tip")))
                .create(slotX(), slotY(), 150, 20, Component.translatable(PREFIX + key), (b, option) -> {
                    value.set(option);
                    this.dirty = true;
                });
        this.slot++;
        this.addRenderableWidget(button);
    }

    private void slider(String key, ForgeConfigSpec.DoubleValue value, double min, double max, double step) {
        slider(Component.translatable(PREFIX + key), Component.translatable(PREFIX + key + ".tip"), value, min, max, step);
    }

    private void slider(Component name, Component tip, ForgeConfigSpec.DoubleValue setting, double min, double max, double step) {
        double current = Math.max(min, Math.min(max, setting.get()));
        ForgeSlider slider = new ForgeSlider(slotX(), slotY(), 150, 20, name.copy().append(": "), Component.empty(),
                min, max, current, step, step < 1.0 ? 2 : 0, true) {
            @Override
            protected void applyValue() {
                // (named "setting", because inside a slider "value" is the slider's own 0-1 position)
                setting.set(this.getValue());
                TSCSettingsScreen.this.dirty = true;
            }
        };
        slider.setTooltip(Tooltip.create(tip));
        this.slot++;
        this.addRenderableWidget(slider);
    }

    private void intSlider(String key, ForgeConfigSpec.IntValue setting, int min, int max) {
        int current = Math.max(min, Math.min(max, setting.get()));
        ForgeSlider slider = new ForgeSlider(slotX(), slotY(), 150, 20,
                Component.translatable(PREFIX + key).append(": "), Component.empty(), min, max, current, 1.0, 0, true) {
            @Override
            protected void applyValue() {
                setting.set(this.getValueInt());
                TSCSettingsScreen.this.dirty = true;
            }
        };
        slider.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + ".tip")));
        this.slot++;
        this.addRenderableWidget(slider);
    }

    /** A centred line of text that takes a whole row. */
    private void label(Component text, int color) {
        if (this.slot % 2 == 1) this.slot++;
        this.labels.add(new Label(text, slotY() + 6, color));
        this.slot += 2;
    }

    // ------------------------------------------------------------------ drawing and saving

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, WHITE);
        for (Label label : this.labels) {
            graphics.drawCenteredString(this.font, label.text(), this.width / 2, label.y(), label.color());
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        save();
    }

    private void save() {
        if (!this.dirty) return;
        this.dirty = false;
        TSCConfig.SPEC.save();
        EntityResolver.invalidate();
    }
}
