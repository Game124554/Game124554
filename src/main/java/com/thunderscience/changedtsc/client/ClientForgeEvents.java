package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.network.ClientGooData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChangedTSC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    /** "TSC Settings" button in the top-left corner of the title screen and the pause menu. */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) return;
        if (!TSCConfig.MENU_BUTTON.get()) return;
        event.addListener(Button.builder(Component.translatable("settings.changed_tsc.button"),
                        b -> Minecraft.getInstance().setScreen(new TSCSettingsScreen(screen)))
                .bounds(4, 4, 100, 20).build());
    }

    /** Settings key (default O). */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN_SETTINGS.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new TSCSettingsScreen(null));
        }
        while (ClientModEvents.OPEN_BODY.consumeClick()) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new GooBodyScreen());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientGooData.clear();
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) ClientGooData.remove(event.getEntity().getId());
    }
}
