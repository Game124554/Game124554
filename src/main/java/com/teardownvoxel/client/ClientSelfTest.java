package com.teardownvoxel.client;

import com.teardownvoxel.TeardownVoxel;
import com.teardownvoxel.client.render.VoxelDebrisRenderer;
import com.teardownvoxel.registry.ModBlocks;
import com.teardownvoxel.registry.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Headless client smoke test (CI only, -Dteardown_voxel.selftest=true): boots to the title screen,
 * audits client mixins, and checks our item/block models and textures resolved (no missing-texture
 * sprites), then quits.
 */
public final class ClientSelfTest {
    private static int titleTicks;
    private static boolean done;

    private ClientSelfTest() {
    }

    public static void init() {
        if (!Boolean.getBoolean("teardown_voxel.selftest")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (done) return;
            // First launch on 1.20 shows the accessibility onboarding screen instead of the title screen.
            boolean menu = client.currentScreen instanceof TitleScreen
                    || client.currentScreen instanceof net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
            if (!menu || client.getOverlay() != null) return;
            if (++titleTicks < 40) return;
            done = true;
            run(client);
        });
    }

    private static void run(MinecraftClient client) {
        boolean ok = true;
        try {
            MixinEnvironment.getCurrentEnvironment().audit();
            TeardownVoxel.LOGGER.info("CLIENT SELFTEST mixin audit OK");
        } catch (Throwable t) {
            ok = false;
            TeardownVoxel.LOGGER.error("CLIENT SELFTEST mixin audit failed", t);
        }
        Identifier missing = new Identifier("minecraft", "missingno");
        for (var item : new net.minecraft.item.Item[]{ModItems.SLEDGEHAMMER, ModItems.PIPE_BOMB, ModItems.BLOWTORCH,
                ModItems.NITRO_CHARGE, ModItems.CHARRED_PLANKS, ModItems.CHARRED_LOG}) {
            BakedModel model = client.getItemRenderer().getModel(new ItemStack(item), null, null, 0);
            Sprite sprite = model.getParticleSprite();
            boolean bad = sprite.getContents().getId().equals(missing);
            TeardownVoxel.LOGGER.info("CLIENT SELFTEST item model {} -> particle sprite {}{}", item, sprite.getContents().getId(), bad ? " (MISSING)" : "");
            ok &= !bad;
        }
        for (var block : new net.minecraft.block.Block[]{ModBlocks.NITRO_CHARGE, ModBlocks.CHARRED_PLANKS, ModBlocks.CHARRED_LOG}) {
            Sprite sprite = client.getBlockRenderManager().getModels().getModelParticleSprite(block.getDefaultState());
            boolean bad = sprite.getContents().getId().equals(missing);
            TeardownVoxel.LOGGER.info("CLIENT SELFTEST block model {} -> {}{}", block, sprite.getContents().getId(), bad ? " (MISSING)" : "");
            ok &= !bad;
        }
        // Touch the renderer class so a static-init failure would surface here.
        TeardownVoxel.LOGGER.info("CLIENT SELFTEST renderer class {}", VoxelDebrisRenderer.class.getSimpleName());
        TeardownVoxel.LOGGER.info(ok ? "CLIENT SELFTEST PASS" : "CLIENT SELFTEST FAIL");
        client.scheduleStop();
    }
}
