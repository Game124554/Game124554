package net.realisticoverhaul;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.dev.ServerSelfTest;
import net.realisticoverhaul.entity.Corpses;
import net.realisticoverhaul.entity.MobRealism;
import net.realisticoverhaul.net.ModNet;
import net.realisticoverhaul.hell.PiglinCurse;
import net.realisticoverhaul.physics.BlockPhysics;
import net.realisticoverhaul.physics.GlassPhysics;
import net.realisticoverhaul.world.BedrockRemover;
import net.realisticoverhaul.world.ModBlocks;
import net.realisticoverhaul.world.Weather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealisticOverhaul implements ModInitializer {
	public static final String MOD_ID = "realisticoverhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ROConfig.load();
		ModBlocks.register();

		// Realistic terrain shape: a built-in data pack, on by default for new worlds.
		// It can be switched off in "Data Packs" when creating a world.
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container ->
				ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "realistic_terrain"), container,
						Text.literal("Realistic Terrain"), ResourcePackActivationType.DEFAULT_ENABLED));

		// Block physics: check neighbours after a block is broken.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world instanceof ServerWorld sw) BlockPhysics.queue(sw, pos);
		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			BlockPhysics.tick(world);
			Weather.tick(world);
			GlassPhysics.tickSprinting(world);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Corpses.tick();
			PiglinCurse.tick(server);
			ModNet.tickSettings(server);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BlockPhysics.clear();
			Corpses.clear();
			ModNet.resetSettings();
		});

		// Realistic mobs: hit splatter
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(MobRealism::onDamage);

		// Corpses / ragdoll
		ServerLivingEntityEvents.AFTER_DEATH.register(Corpses::onDeath);
		AttackEntityCallback.EVENT.register(Corpses::onAttack);

		// Bedrock removal in newly loaded chunks
		ServerChunkEvents.CHUNK_LOAD.register(BedrockRemover::onChunkLoad);

		// Death -> Nether -> Piglin
		ServerPlayerEvents.AFTER_RESPAWN.register(PiglinCurse::onRespawn);
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(PiglinCurse::onChangeWorld);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			PiglinCurse.sendTo(handler.player);
			ModNet.sendSettings(handler.player);
		});

		ServerSelfTest.init();

		LOGGER.info("Realistic Overhaul loaded");
	}
}
