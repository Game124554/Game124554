package net.realisticoverhaul.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.color.world.BiomeColors;
import net.realisticoverhaul.client.smooth.SmoothTerrain;
import net.realisticoverhaul.dev.ClientSelfTest;
import net.realisticoverhaul.net.ModNet;
import net.realisticoverhaul.physics.StepAssist;
import net.realisticoverhaul.world.ModBlocks;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RealisticOverhaulClient implements ClientModInitializer {
	private static KeyBinding openConfig;

	@Override
	public void onInitializeClient() {
		openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.realisticoverhaul.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.realisticoverhaul"));

		SmoothTerrain.reload();
		SmoothTerrain.registerModels();

		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PUDDLE, RenderLayer.getTranslucent());
		// Puddles mirror the bright sky more than they show the water's own blue.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			int water = world != null && pos != null ? BiomeColors.getWaterColor(world, pos) : 0x3F76E4;
			int r = ((water >> 16) & 0xFF) * 3 / 10 + 0xC8 * 7 / 10;
			int g = ((water >> 8) & 0xFF) * 3 / 10 + 0xD4 * 7 / 10;
			int b = (water & 0xFF) * 3 / 10 + 0xE2 * 7 / 10;
			return (r << 16) | (g << 8) | b;
		}, ModBlocks.PUDDLE);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfig.wasPressed()) {
				client.setScreen(new ConfigScreen(client.currentScreen));
			}
			ClientEffects.tick(client);
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNet.SHAKE, (client, handler, buf, responseSender) -> {
			float amount = buf.readFloat();
			client.execute(() -> CameraShake.add(amount));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNet.PIGLINS, (client, handler, buf, responseSender) -> {
			int n = buf.readVarInt();
			Set<UUID> set = new HashSet<>();
			for (int i = 0; i < n; i++) set.add(buf.readUuid());
			client.execute(() -> ClientPiglin.set(set));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNet.SETTINGS, (client, handler, buf, responseSender) -> {
			boolean stepAssist = buf.readBoolean();
			client.execute(() -> StepAssist.serverAllows = stepAssist);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientPiglin.clear();
			StepAssist.serverAllows = false;
		});

		// "Realistic Overhaul..." button in the Options screen.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof OptionsScreen) {
				Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Realistic Overhaul..."),
								b -> client.setScreen(new ConfigScreen(screen)))
						.dimensions(5, 5, 120, 20).build());
			}
		});

		// Glowing eyes on zombies and skeletons.
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
			if (MobAnimations.hasGlowEyes(entityType)) registerGlowEyes(helper, entityRenderer);
		});

		ClientSelfTest.init();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registerGlowEyes(LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper,
										 net.minecraft.client.render.entity.LivingEntityRenderer<?, ?> renderer) {
		helper.register(new GlowEyesFeature(renderer));
	}
}
