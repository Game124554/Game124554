package com.thunderscience.changedtsc.util;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.config.TSCConfig;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts the Changed mod's furniture into the structures.
 *
 * The structure files do not name Changed's blocks directly. They hold "furniture markers" that say
 * what belongs there (lab_table, office_chair, computer, microscope...). When the structure loads,
 * each marker looks up the matching Changed block by name and places it the same way a player
 * would (so blocks made of several parts are placed whole and face the right way). If Changed has
 * no such block, or it does not fit, the marker's vanilla stand-in is placed instead, so a room is
 * never left empty. Which names are searched for each kind is set in the config ("furniture.kinds").
 */
public final class FurniturePlacer {
    /** Never picked by a name search: these are not furniture and could hurt the player. */
    private static final List<String> NEVER = List.of("latex", "gas", "laser", "crystal", "fluid");

    private static final Map<String, Optional<Block>> CACHE = new ConcurrentHashMap<>();
    private static volatile Map<String, String> specs;
    private static final Set<Block> WARNED = ConcurrentHashMap.newKeySet();

    private FurniturePlacer() {}

    public static void invalidate() {
        CACHE.clear();
        specs = null;
    }

    /** kind -> search spec, from the config ("kind=name1|name2"). */
    public static Map<String, String> specs() {
        Map<String, String> map = specs;
        if (map == null) {
            map = new LinkedHashMap<>();
            for (String line : TSCConfig.FURNITURE_KINDS.get()) {
                int eq = line.indexOf('=');
                if (eq > 0) map.put(line.substring(0, eq).trim().toLowerCase(Locale.ROOT), line.substring(eq + 1).trim());
            }
            specs = map;
        }
        return map;
    }

    /** The Changed block used for this kind of furniture, if one was found. */
    public static Optional<Block> resolve(String kind) {
        if (!TSCConfig.FURNITURE_USE_CHANGED.get()) return Optional.empty();
        return CACHE.computeIfAbsent(kind, FurniturePlacer::find);
    }

    private static Optional<Block> find(String kind) {
        String spec = specs().getOrDefault(kind, kind);
        Set<String> namespaces = EntityResolver.latexNamespaces();
        for (String raw : spec.split("\\|")) {
            String option = raw.trim().toLowerCase(Locale.ROOT);
            if (option.isEmpty()) continue;
            if (option.contains(":")) {
                ResourceLocation id = ResourceLocation.tryParse(option);
                if (id != null && ForgeRegistries.BLOCKS.containsKey(id)) return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(id));
                continue;
            }
            Optional<ResourceLocation> best = ForgeRegistries.BLOCKS.getKeys().stream()
                    .filter(id -> namespaces.contains(id.getNamespace()))
                    .filter(id -> id.getPath().contains(option))
                    .filter(id -> NEVER.stream().noneMatch(id.getPath()::contains))
                    // an exact name first, then the shortest name that contains the word
                    .min(Comparator.comparingInt((ResourceLocation id) -> id.getPath().equals(option) ? 0 : 1)
                            .thenComparingInt(id -> id.getPath().length())
                            .thenComparing(ResourceLocation::toString));
            if (best.isPresent()) return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(best.get()));
        }
        return Optional.empty();
    }

    /**
     * Places one piece of furniture at an empty position.
     *
     * @param facing   the way the furniture faces (for a chair: the way the seated person looks)
     * @param fallback vanilla block state used when Changed has nothing for this kind; may be empty.
     *                 "{facing}" and "{opposite}" are replaced by directions.
     */
    public static void place(ServerLevel level, BlockPos pos, String kind, Direction facing, String fallback) {
        Optional<Block> block = resolve(kind);
        if (block.isPresent() && tryPlace(level, pos, block.get(), facing)) return;
        placeFallback(level, pos, fallback, facing);
    }

    private static boolean tryPlace(ServerLevel level, BlockPos pos, Block block, Direction facing) {
        try {
            Item item = block.asItem();
            if (item instanceof BlockItem blockItem) {
                // Place it like a player looking the opposite way would, so the block's own placement
                // code picks the facing and adds any extra parts (tall or wide furniture).
                InteractionResult result = blockItem.place(
                        new DirectionalPlaceContext(level, pos, facing.getOpposite(), new ItemStack(item), Direction.UP));
                if (!level.getBlockState(pos).isAir()) return true;
                warnOnce(block, "placing it returned " + result);
                return false;
            }
            BlockState state = withFacing(block.defaultBlockState(), facing);
            return state.canSurvive(level, pos) && level.setBlock(pos, state, 3);
        } catch (Throwable t) {
            warnOnce(block, t.toString());
            return !level.getBlockState(pos).isAir();
        }
    }

    private static void warnOnce(Block block, String reason) {
        if (WARNED.add(block)) {
            ChangedTSC.LOGGER.warn("Furniture {} could not be placed ({}); using the vanilla stand-in for it",
                    ForgeRegistries.BLOCKS.getKey(block), reason);
        }
    }

    private static void placeFallback(ServerLevel level, BlockPos pos, String fallback, Direction facing) {
        if (fallback == null || fallback.isBlank()) return;
        String text = fallback.replace("{facing}", facing.getName()).replace("{opposite}", facing.getOpposite().getName());
        try {
            BlockState state = BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), text, false).blockState();
            level.setBlock(pos, state, 3);
        } catch (Exception e) {
            ChangedTSC.LOGGER.debug("Bad furniture fallback '{}': {}", text, e.toString());
        }
    }

    private static BlockState withFacing(BlockState state, Direction facing) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) return state.setValue(BlockStateProperties.FACING, facing);
        return state;
    }
}
