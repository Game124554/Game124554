package com.mandelahorror.client;

import com.mandelahorror.item.StrangePaperItem;
import com.mandelahorror.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/** Client entrypoint: renderers, HUD, ticking, packets. (Camera shake + skin glitch are mixins.) */
public class MandelaHorrorClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.ALTERNATE, AlternateRenderer::new);
        EntityRendererRegistry.register(ModEntities.MIMIC, MimicRenderer::new);

        HudRenderCallback.EVENT.register(HorrorOverlay::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientHorror.clientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHorror.reset());

        ClientNetwork.registerReceivers();
        StrangePaperItem.clientOpener = ClientHorror::openPaper;
    }
}
