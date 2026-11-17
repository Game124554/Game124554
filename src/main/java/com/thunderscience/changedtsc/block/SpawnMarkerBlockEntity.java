package com.thunderscience.changedtsc.block;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.boss.BossManager;
import com.thunderscience.changedtsc.boss.BossType;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.registry.ModBlockEntities;
import com.thunderscience.changedtsc.registry.ModEntities;
import com.thunderscience.changedtsc.util.EntityResolver;
import com.thunderscience.changedtsc.util.EntitySpawning;
import com.thunderscience.changedtsc.util.FurniturePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import com.thunderscience.changedtsc.util.LatexHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Holds a "Key" telling what to spawn. Keys used by the structures:
 *   guard, scientist, staff, puro, latex, boss:squid_dog, boss:tiger_shark, boss:elder_one, boss:wolf_king
 *   furniture:lab_table, furniture:office_chair, furniture:computer ... (see FurniturePlacer)
 * Any other key is treated as an entity spec (e.g. "changed:dark_latex_wolf_male" or "auto:shark").
 * When a player comes close the mobs appear and the marker removes itself.
 */
public class SpawnMarkerBlockEntity extends BlockEntity {
    private String key = "latex";
    private int count = 1;
    /** Furniture markers only: which way the furniture faces, and the vanilla stand-in. */
    private String facing = "north";
    private String fallback = "";

    private static final String FURNITURE = "furniture:";

    public SpawnMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPAWN_MARKER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpawnMarkerBlockEntity marker) {
        if (level.getGameTime() % 20L != 0L || !(level instanceof ServerLevel server)) return;
        if (marker.key.startsWith(FURNITURE)) {
            // furniture appears as soon as the area loads; something standing on a table waits for the table
            if (level.getBlockState(pos.below()).is(state.getBlock())) return;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            try {
                Direction direction = Direction.byName(marker.facing);
                FurniturePlacer.place(server, pos, marker.key.substring(FURNITURE.length()),
                        direction == null || direction.getAxis().isVertical() ? Direction.NORTH : direction, marker.fallback);
            } catch (Exception e) {
                ChangedTSC.LOGGER.error("Furniture marker '{}' at {} failed", marker.key, pos, e);
            }
            return;
        }
        boolean boss = marker.key.startsWith("boss:");
        double range = boss ? TSCConfig.BOSS_TRIGGER_RANGE.get() : TSCConfig.MARKER_TRIGGER_RANGE.get();
        if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range)) return;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        try {
            marker.spawnContents(server, pos);
        } catch (Exception e) {
            ChangedTSC.LOGGER.error("Spawn marker '{}' at {} failed", marker.key, pos, e);
        }
    }

    private void spawnContents(ServerLevel level, BlockPos pos) {
        if (key.startsWith("boss:")) {
            BossType.byId(key.substring(5)).ifPresent(type -> BossManager.spawnBoss(level, pos, type));
            return;
        }
        for (int i = 0; i < Math.max(1, count); i++) {
            EntityType<?> type = pickType(level);
            if (type == null) {
                ChangedTSC.LOGGER.debug("Spawn marker '{}' resolved to nothing", key);
                return;
            }
            if (key.equals("puro")) {
                Entity puro = EntitySpawning.spawn(level, type, pos, MobSpawnType.STRUCTURE, true, LatexHelper.FRIENDLY_TAG);
                if (puro != null) LatexHelper.namePuro(puro);
            } else {
                EntitySpawning.spawn(level, type, pos, MobSpawnType.STRUCTURE, true);
            }
        }
    }

    private EntityType<?> pickType(ServerLevel level) {
        return switch (key) {
            case "guard" -> ModEntities.GUARD.get();
            case "scientist" -> ModEntities.SCIENTIST.get();
            case "staff" -> ModEntities.STAFF.get();
            case "puro" -> TSCConfig.FRIENDLY_PURO_ENABLED.get()
                    ? EntityResolver.resolve(TSCConfig.FRIENDLY_SPAWN_ENTITY.get()).orElse(null) : null;
            case "latex" -> {
                List<EntityType<?>> pool = EntityResolver.latexPool();
                yield pool.isEmpty() ? null : pool.get(level.random.nextInt(pool.size()));
            }
            default -> EntityResolver.resolve(key).orElse(null);
        };
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Key")) this.key = tag.getString("Key");
        this.count = tag.contains("Count") ? tag.getInt("Count") : 1;
        if (tag.contains("Facing")) this.facing = tag.getString("Facing");
        if (tag.contains("Fallback")) this.fallback = tag.getString("Fallback");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Key", this.key);
        tag.putInt("Count", this.count);
        tag.putString("Facing", this.facing);
        tag.putString("Fallback", this.fallback);
    }
}
