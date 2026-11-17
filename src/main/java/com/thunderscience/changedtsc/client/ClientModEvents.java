package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.entity.AbstractTSCHuman;
import com.thunderscience.changedtsc.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ChangedTSC.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    /** Opens the TSC settings screen. Rebindable under Options > Controls > Key Binds. */
    public static final KeyMapping OPEN_SETTINGS =
            new KeyMapping("key.changed_tsc.settings", GLFW.GLFW_KEY_O, "key.categories.changed_tsc");

    /** Opens the body screen with the big paper doll. */
    public static final KeyMapping OPEN_BODY =
            new KeyMapping("key.changed_tsc.body", GLFW.GLFW_KEY_G, "key.categories.changed_tsc");

    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
        event.register(OPEN_BODY);
    }

    /** The paper doll on the HUD. */
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("paper_doll", PaperDollHud::render);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<AbstractTSCHuman>registerEntityRenderer(ModEntities.GUARD.get(), TSCHumanRenderer::new);
        event.<AbstractTSCHuman>registerEntityRenderer(ModEntities.SCIENTIST.get(), TSCHumanRenderer::new);
        event.<AbstractTSCHuman>registerEntityRenderer(ModEntities.STAFF.get(), TSCHumanRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), ThrownItemRenderer::new);
    }

    /** Goo / fur overlay on every player skin (wide and slim). */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new GooLayer<>(renderer));
        }
    }
}
