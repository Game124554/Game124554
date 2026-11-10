package com.teardownvoxel;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.registry.ModBlocks;
import com.teardownvoxel.registry.ModEntities;
import com.teardownvoxel.registry.ModItems;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.registry.ModTags;
import com.teardownvoxel.world.DebrisSpawner;
import com.teardownvoxel.world.ServerScheduler;
import com.teardownvoxel.world.StructuralIntegrity;
import com.teardownvoxel.world.VoxelDamage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.item.MiningToolItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TeardownVoxel implements ModInitializer {
    public static final String MOD_ID = "teardown_voxel";
    public static final Logger LOGGER = LoggerFactory.getLogger("Teardown Voxel Physics");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        TVConfig.load();
        ModTags.init();
        ModParticles.register();
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            ServerScheduler.tick(world);
            StructuralIntegrity.tick(world);
            VoxelDamage.tick(world);
            DebrisSpawner.tickBudget(world);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerScheduler.clear();
            StructuralIntegrity.clear();
            VoxelDamage.clear();
            DebrisSpawner.clear();
        });

        // Hand-mined blocks: optional chips + structural collapse check.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld sw)) return;
            TVConfig cfg = TVConfig.get();
            if (cfg.toolBreakDebris && player.getMainHandStack().getItem() instanceof MiningToolItem) {
                Vec3d push = player.getRotationVector().multiply(0.15);
                DebrisSpawner.shatter(sw, pos, state, push, 1 + sw.random.nextInt(2), 0.18);
            }
            VoxelDamage.clearAt(sw, pos);
            if (cfg.collapseOnPlayerBreak) {
                StructuralIntegrity.onBlocksDestroyed(sw, List.of(pos.toImmutable()));
            }
        });

        SelfTest.init();
        LOGGER.info("Teardown Voxel Physics loaded - things are about to get messy.");
    }
}
