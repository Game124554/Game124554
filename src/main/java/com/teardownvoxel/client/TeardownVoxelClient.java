package com.teardownvoxel.client;

import com.teardownvoxel.client.particle.DustCloudParticle;
import com.teardownvoxel.client.particle.PebbleParticle;
import com.teardownvoxel.client.particle.SparkParticle;
import com.teardownvoxel.client.render.FallingStructureRenderer;
import com.teardownvoxel.client.render.VoxelDebrisRenderer;
import com.teardownvoxel.network.ModNetworking;
import com.teardownvoxel.registry.ModBlocks;
import com.teardownvoxel.registry.ModEntities;
import com.teardownvoxel.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class TeardownVoxelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.VOXEL_DEBRIS, VoxelDebrisRenderer::new);
        EntityRendererRegistry.register(ModEntities.FALLING_STRUCTURE, FallingStructureRenderer::new);
        EntityRendererRegistry.register(ModEntities.PIPE_BOMB, FlyingItemEntityRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.SPARK, SparkParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.DUST_CLOUD, DustCloudParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.PEBBLE, PebbleParticle.Factory::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.NITRO_CHARGE, RenderLayer.getCutout());

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SCREEN_SHAKE, (client, handler, buf, responseSender) -> {
            float intensity = buf.readFloat();
            int duration = buf.readVarInt();
            client.execute(() -> ScreenShake.add(intensity, duration));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused()) ScreenShake.tick();
        });
    }
}
