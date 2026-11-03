package com.latexdrone.client;

import com.latexdrone.entity.EvilScientistEntity;
import com.latexdrone.entity.LatexMobEntity;
import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModBlocks;
import com.latexdrone.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.EntityType;

import java.util.UUID;

public class LatexDroneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(LatexPartsModel.LAYER, LatexPartsModel::getTexturedModelData);

        for (EntityType<LatexMobEntity> type : ModEntities.LATEX_TYPES) {
            EntityRendererRegistry.register(type, LatexMobRenderer::new);
        }
        for (EntityType<EvilScientistEntity> type : ModEntities.SCIENTIST_TYPES) {
            EntityRendererRegistry.register(type, ScientistRenderer::new);
        }
        EntityRendererRegistry.register(ModEntities.SYRINGE_DART, SyringeDartRenderer::new);
        EntityRendererRegistry.register(ModEntities.PURIFIER_SHOT, FlyingItemEntityRenderer::new);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                ModBlocks.LATEX_CRYSTAL, ModBlocks.WHITE_LATEX_CRYSTAL, ModBlocks.METAL_GRATE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(),
                ModBlocks.REINFORCED_LAB_GLASS, ModBlocks.CONTAINMENT_TANK);

        // Player skin replacement extras: latex overlay during the transformation, then ears/tail and glowing eyes.
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new TransformOverlayFeature(playerRenderer));
                helper.register(new LatexPartsFeature<>(playerRenderer, context, PlayerLatexSpecs::parts));
                helper.register(new LatexGlowFeature<>(playerRenderer, PlayerLatexSpecs::glow));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(InfectionManager.SYNC_PACKET, (client, handler, buf, sender) -> {
            UUID id = buf.readUuid();
            float infection = buf.readFloat();
            boolean transformed = buf.readBoolean();
            int form = buf.readVarInt();
            int pending = buf.readVarInt();
            int ticks = buf.readVarInt();
            client.execute(() -> ClientLatexData.update(id, infection, transformed, form, pending, ticks));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLatexData.clear());

        HudRenderCallback.EVENT.register(LatexHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::tick);
    }
}
