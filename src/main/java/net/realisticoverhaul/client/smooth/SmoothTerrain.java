package net.realisticoverhaul.client.smooth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.config.ROConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NoCubes-style smooth terrain: which blocks are smoothed and whether the feature is on.
 * The list comes from "smoothBlocks" in the config (block ids, or #tags).
 */
public final class SmoothTerrain {
	private static volatile boolean enabled = true;
	private static volatile Set<Block> blocks = Set.of();
	private static volatile List<TagKey<Block>> tags = List.of();

	private SmoothTerrain() {}

	public static boolean enabled() {
		return enabled;
	}

	public static boolean isSmoothable(BlockState state) {
		if (blocks.contains(state.getBlock())) return true;
		List<TagKey<Block>> t = tags;
		for (int i = 0; i < t.size(); i++) {
			if (state.isIn(t.get(i))) return true;
		}
		return false;
	}

	/** Reads the config. Call after loading or changing it. */
	public static void reload() {
		ROConfig cfg = ROConfig.get();
		Set<Block> b = new HashSet<>();
		List<TagKey<Block>> tg = new ArrayList<>();
		if (cfg.smoothBlocks != null) {
			for (String raw : cfg.smoothBlocks) {
				if (raw == null || raw.isBlank()) continue;
				String s = raw.trim();
				try {
					if (s.startsWith("#")) {
						tg.add(TagKey.of(RegistryKeys.BLOCK, new Identifier(s.substring(1))));
					} else {
						Identifier id = new Identifier(s);
						if (Registries.BLOCK.containsId(id)) b.add(Registries.BLOCK.get(id));
					}
				} catch (Exception e) {
					RealisticOverhaul.LOGGER.warn("Bad smoothBlocks entry '{}'", s);
				}
			}
		}
		blocks = Set.copyOf(b);
		tags = List.copyOf(tg);
		enabled = cfg.smoothTerrain;
	}

	/**
	 * Wraps block models so they can be drawn smooth. Every opaque full-cube block is wrapped,
	 * because the smooth list can use tags that are only known after joining a world.
	 */
	public static void registerModels() {
		ModelLoadingPlugin.register(ctx -> ctx.modifyModelAfterBake().register((model, context) -> {
			if (model == null) return null;
			if (context.id() instanceof ModelIdentifier mid && !"inventory".equals(mid.getVariant())) {
				Identifier blockId = new Identifier(mid.getNamespace(), mid.getPath());
				if (Registries.BLOCK.containsId(blockId)) {
					Block block = Registries.BLOCK.get(blockId);
					if (block == Blocks.SNOW) return new SmoothSnowModel(model);
					if (block != Blocks.AIR && (blocks.contains(block)
							|| block.getDefaultState().isOpaqueFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN))) {
						return new SmoothBakedModel(model);
					}
				}
			}
			return model;
		}));
	}

	/** Rebuild all chunk meshes (after toggling the feature or changing the list). */
	public static void rebuildChunks() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.worldRenderer != null && client.world != null) {
			client.worldRenderer.reload();
		}
	}
}
