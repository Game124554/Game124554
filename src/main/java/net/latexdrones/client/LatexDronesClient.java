package net.latexdrones.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.latexdrones.LatexDrones;
import net.latexdrones.client.model.AnthroModel;
import net.latexdrones.client.model.BeastModel;
import net.latexdrones.client.model.BlobModel;
import net.latexdrones.client.model.HoverModel;
import net.latexdrones.client.model.ModModelLayers;
import net.latexdrones.client.render.BeastRenderer;
import net.latexdrones.client.render.BlobRenderer;
import net.latexdrones.client.render.HoverRenderer;
import net.latexdrones.client.render.HumanoidDroneRenderer;
import net.latexdrones.client.render.LatexFormLayer;
import net.latexdrones.content.MobDef;
import net.latexdrones.content.MobDefs;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.ModNetworking;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** Client entry point: models, renderers, render layers, HUD, networking. */
public class LatexDronesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ANTHRO, AnthroModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BEAST, BeastModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BLOB, BlobModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.HOVER, HoverModel::createBodyLayer);

        for (MobDef def : MobDefs.ALL) {
            switch (def.archetype) {
                case BEAST:
                    EntityRendererRegistry.register(def.type, BeastRenderer::new);
                    break;
                case BLOB:
                    EntityRendererRegistry.register(def.type, BlobRenderer::new);
                    break;
                case HOVER:
                    EntityRendererRegistry.register(def.type, HoverRenderer::new);
                    break;
                default:
                    EntityRendererRegistry.register(def.type, HumanoidDroneRenderer::new);
                    break;
            }
        }
        EntityRendererRegistry.register(ModEntities.DART, ThrownItemRenderer::new);

        for (Block block : ModBlocks.CUTOUT) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout());
        }
        for (Block block : ModBlocks.TRANSLUCENT) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.translucent());
        }

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYNC,
                (client, handler, buf, responseSender) -> ClientHooks.receiveSync(client, buf));
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_TERMINAL,
                (client, handler, buf, responseSender) -> client.execute(() -> client.setScreen(new DroneTerminalScreen())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            InfectionData.CLIENT.clear();
            ClientHooks.revealTicks = 0;
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
        HudRenderCallback.EVENT.register(HudOverlay::render);

        // latex skin + ears / tails on players
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                registrationHelper.register(new LatexFormLayer(playerRenderer, context.getModelSet()));
            }
        });

        // one-line usage hints on the mod's items (lang keys item.latexdrones.<name>.tip)
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (LatexDrones.MOD_ID.equals(id.getNamespace())) {
                String key = "item." + LatexDrones.MOD_ID + "." + id.getPath() + ".tip";
                if (I18n.exists(key)) {
                    lines.add(Math.min(1, lines.size()), Component.translatable(key).withStyle(ChatFormatting.GRAY));
                }
            }
        });
    }
}
