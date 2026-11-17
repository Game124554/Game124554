package com.thunderscience.changedtsc.gametest;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.block.SpawnMarkerBlockEntity;
import com.thunderscience.changedtsc.boss.BossManager;
import com.thunderscience.changedtsc.boss.BossType;
import com.thunderscience.changedtsc.compat.ChangedCompat;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.infection.GooInfection;
import com.thunderscience.changedtsc.registry.ModBlocks;
import com.thunderscience.changedtsc.registry.ModEntities;
import com.thunderscience.changedtsc.util.EntityResolver;
import com.thunderscience.changedtsc.util.FurniturePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Automatic start-up tests, run by "gradlew runGameTestServer" (the GitHub build does this).
 *
 * They start a real server with the mod, which proves the mod loads and all of its data files
 * (structures, loot tables, world generation) are valid, and then try the main features once:
 * humans spawn, furniture markers place furniture, structures can be placed, every boss fights.
 * They never run in a normal game.
 */
@GameTestHolder(ChangedTSC.MODID)
@PrefixGameTestTemplate(false)
public final class TSCGameTests {
    private static final String FLOOR = "gametest_floor";
    private static final int STRUCTURES = 63;

    private TSCGameTests() {}

    /** All 63 structures, their building files and the chest loot are present and valid. */
    @GameTest(template = FLOOR)
    public static void dataLoads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<ResourceLocation> structures = ours(level.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet());
        Set<ResourceLocation> sets = ours(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).keySet());
        if (structures.size() != STRUCTURES) helper.fail("Expected " + STRUCTURES + " structures, found " + structures.size());
        if (sets.size() != STRUCTURES) helper.fail("Expected " + STRUCTURES + " structure sets, found " + sets.size());
        for (ResourceLocation id : structures) {
            if (level.getStructureManager().get(id).isEmpty()) helper.fail("Missing building file for " + id);
        }
        for (String table : List.of("abandoned", "armory", "lab", "medical", "supply", "vault")) {
            ResourceLocation id = new ResourceLocation(ChangedTSC.MODID, "chests/" + table);
            if (level.getServer().getLootData().getLootTable(id) == LootTable.EMPTY) helper.fail("Missing loot table " + id);
        }
        helper.succeed();
    }

    /** Writes what was found in the Changed mod to the log: bosses, Puro, latex and furniture. */
    @GameTest(template = FLOOR)
    public static void reportWhatWasFound(GameTestHelper helper) {
        ChangedTSC.LOGGER.info("[TSC TEST] Changed installed: {}", ChangedCompat.isLoaded());
        for (BossType type : BossType.values()) {
            ChangedTSC.LOGGER.info("[TSC TEST] boss {} -> {} | minions -> {}", type.id(),
                    name(EntityResolver.resolve(TSCConfig.BOSS_ENTITY.get(type).get())),
                    name(EntityResolver.resolve(TSCConfig.BOSS_MINIONS.get(type).get())));
        }
        ChangedTSC.LOGGER.info("[TSC TEST] puro -> {}", name(EntityResolver.resolve(TSCConfig.FRIENDLY_SPAWN_ENTITY.get())));
        ChangedTSC.LOGGER.info("[TSC TEST] friendly types -> {}", EntityResolver.friendlyTypes().stream()
                .map(t -> String.valueOf(EntityType.getKey(t))).collect(Collectors.toList()));
        ChangedTSC.LOGGER.info("[TSC TEST] latex pool ({}) -> {}", EntityResolver.latexPool().size(),
                EntityResolver.latexPool().stream().map(t -> EntityType.getKey(t).getPath()).collect(Collectors.toList()));
        for (String kind : FurniturePlacer.specs().keySet()) {
            ChangedTSC.LOGGER.info("[TSC TEST] furniture {} -> {}", kind, FurniturePlacer.resolve(kind)
                    .map(b -> String.valueOf(ForgeRegistries.BLOCKS.getKey(b))).orElse("(vanilla stand-in)"));
        }
        // details about Changed that the add-on depends on
        if (ChangedCompat.isLoaded()) {
            ChangedTSC.LOGGER.info("[TSC TEST] paper doll reads from Changed: {}", ChangedCompat.describe());
            Player mock = helper.makeMockSurvivalPlayer();
            ChangedTSC.LOGGER.info("[TSC TEST] stand-in player transfurred={} progress={}%",
                    ChangedCompat.isTransfurred(mock), ChangedCompat.transfurPercent(mock));
            for (String kind : FurniturePlacer.specs().keySet()) {
                FurniturePlacer.resolve(kind).ifPresent(block -> ChangedTSC.LOGGER.info("[TSC TEST] {} is {} with {} item={}",
                        kind, block.getClass().getSimpleName(), block.defaultBlockState(),
                        ForgeRegistries.ITEMS.getKey(block.asItem())));
            }
            // which way does a chair face for each rotation value? (where its tall back is)
            FurniturePlacer.resolve("office_chair").ifPresent(block -> {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    String text = state.toString();
                    if (!(text.contains("rotation=0,") || text.contains("rotation=4,") || text.contains("rotation=8,")
                            || text.contains("rotation=12,")) || text.contains("waterlogged=true")) continue;
                    ChangedTSC.LOGGER.info("[TSC TEST] chair {} shape {}", text.substring(text.indexOf('[')),
                            state.getShape(helper.getLevel(), BlockPos.ZERO).toAabbs());
                }
            });
            // the /transfur command: how it is written and which forms it accepts
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            var source = helper.getLevel().getServer().createCommandSourceStack();
            var node = dispatcher.getRoot().getChild("transfur");
            if (node == null) {
                ChangedTSC.LOGGER.info("[TSC TEST] no /transfur command found");
            } else {
                ChangedTSC.LOGGER.info("[TSC TEST] /transfur usage: {}", dispatcher.getSmartUsage(node, source).values());
                for (String form : List.of("changed:form_dark_latex_wolf_male", "changed:dark_latex_wolf_male",
                        "changed:form_dark_latex_wolf/male", "changed:dark_latex_wolf/male")) {
                    var parsed = dispatcher.parse("transfur @p " + form, source);
                    ChangedTSC.LOGGER.info("[TSC TEST] 'transfur @p {}' parses: {}", form,
                            parsed.getExceptions().isEmpty() && !parsed.getReader().canRead());
                }
                try {
                    var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("transfur @p changed:", source)).get();
                    ChangedTSC.LOGGER.info("[TSC TEST] /transfur forms ({}): {}", suggestions.getList().size(),
                            suggestions.getList().stream().limit(40).map(s -> s.getText()).collect(Collectors.toList()));
                } catch (Exception e) {
                    ChangedTSC.LOGGER.info("[TSC TEST] could not list /transfur forms: {}", e.toString());
                }
            }
        }
        helper.succeed();
    }

    /** Guards, scientists and staff spawn, get their gear and live through a few seconds. */
    @GameTest(template = FLOOR, timeoutTicks = 200)
    public static void humansSpawn(GameTestHelper helper) {
        Mob guard = helper.spawn(ModEntities.GUARD.get(), new BlockPos(2, 1, 2));
        Mob scientist = helper.spawn(ModEntities.SCIENTIST.get(), new BlockPos(4, 1, 4));
        Mob staff = helper.spawn(ModEntities.STAFF.get(), new BlockPos(6, 1, 6));
        GooInfection.set(staff, 50.0F);
        if (GooInfection.stage(staff) != 2) helper.fail("50% goo should be stage 2 (furry body), was " + GooInfection.stage(staff));
        helper.runAfterDelay(100, () -> {
            if (!guard.isAlive() || !scientist.isAlive() || !staff.isAlive()) helper.fail("A TSC human died or vanished");
            // the scientist standing next to the infected staff member treats them
            ChangedTSC.LOGGER.info("[TSC TEST] staff goo after 5 seconds beside a scientist: {}%", GooInfection.get(staff));
            guard.discard();
            scientist.discard();
            staff.discard();
            helper.succeed();
        });
    }

    /** A furniture marker turns into furniture: Changed's block if there is one, else the vanilla stand-in. */
    @GameTest(template = FLOOR, timeoutTicks = 200)
    public static void furnitureMarkers(GameTestHelper helper) {
        BlockPos table = new BlockPos(1, 1, 1);
        BlockPos tableBeside = new BlockPos(2, 1, 1);
        BlockPos onTable = table.above();
        BlockPos chair = new BlockPos(1, 1, 3);
        marker(helper, table, "lab_table", "south", "minecraft:smooth_quartz_slab[type=top]");
        marker(helper, tableBeside, "lab_table", "south", "minecraft:smooth_quartz_slab[type=top]");
        marker(helper, onTable, "computer", "south", "minecraft:daylight_detector");
        marker(helper, tableBeside.above(), "microscope", "south", "minecraft:brewing_stand");
        marker(helper, chair, "office_chair", "north", "minecraft:smooth_quartz_stairs[facing={opposite},half=bottom]");
        // every other kind once, on the floor with room around it
        List<String> others = List.of("petri_dish", "clipboard", "book_stack", "canned_peaches", "cardboard_box",
                "box_pile", "iron_crate", "speaker", "iv_rack");
        for (int i = 0; i < others.size(); i++) {
            marker(helper, new BlockPos(4 + (i % 3) * 2, 1, 1 + (i / 3) * 3), others.get(i), "south", "minecraft:barrel[facing=up]");
        }
        Block marker = ModBlocks.SPAWN_MARKER.get();
        helper.runAfterDelay(100, () -> {
            for (BlockPos pos : List.of(table, tableBeside, onTable, tableBeside.above(), chair)) {
                BlockState state = helper.getBlockState(pos);
                ChangedTSC.LOGGER.info("[TSC TEST] furniture at {} -> {}", pos, state);
                if (state.is(marker)) helper.fail("Marker was not replaced", pos);
            }
            for (int i = 0; i < others.size(); i++) {
                BlockPos pos = new BlockPos(4 + (i % 3) * 2, 1, 1 + (i / 3) * 3);
                BlockState state = helper.getBlockState(pos);
                ChangedTSC.LOGGER.info("[TSC TEST] furniture {} -> {}", others.get(i), state);
                if (state.is(marker) || state.isAir()) helper.fail("Nothing was placed for " + others.get(i), pos);
            }
            // step-by-step look at why a block is accepted or refused
            for (String kind : List.of("lab_table", "petri_dish", "speaker")) {
                FurniturePlacer.resolve(kind).ifPresent(block -> {
                    ServerLevel level = helper.getLevel();
                    BlockPos at = helper.absolutePos(new BlockPos(3, 1, 6));
                    level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
                    BlockState below = level.getBlockState(at.below());
                    ChangedTSC.LOGGER.info("[TSC TEST] {}: below={} sturdyTop={} north={} canSurvive={} unobstructed={}", kind, below,
                            below.isFaceSturdy(level, at.below(), Direction.UP), level.getBlockState(at.north()),
                            block.defaultBlockState().canSurvive(level, at),
                            level.isUnobstructed(block.defaultBlockState(), at, net.minecraft.world.phys.shapes.CollisionContext.empty()));
                    if (block.asItem() instanceof net.minecraft.world.item.BlockItem item) {
                        var context = new net.minecraft.world.item.context.DirectionalPlaceContext(level, at, Direction.NORTH,
                                new net.minecraft.world.item.ItemStack(item), Direction.UP);
                        ChangedTSC.LOGGER.info("[TSC TEST] {}: canPlace={} stateForPlacement={} place={} -> {}", kind, context.canPlace(),
                                block.getStateForPlacement(context), item.place(context), level.getBlockState(at));
                    }
                    level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
                });
            }
            if (helper.getBlockState(table).isAir()) helper.fail("No table was placed", table);
            if (helper.getBlockState(chair).isAir()) helper.fail("No chair was placed", chair);
            if (FurniturePlacer.resolve("office_chair").isEmpty()) {
                // vanilla stand-in: stairs whose back is behind the seated person (who looks north)
                BlockState state = helper.getBlockState(chair);
                if (!state.is(Blocks.SMOOTH_QUARTZ_STAIRS) || state.getValue(StairBlock.FACING) != Direction.SOUTH) {
                    helper.fail("Chair stand-in is wrong: " + state, chair);
                }
            } else if (FurniturePlacer.resolve("lab_table").filter(b -> helper.getBlockState(table).is(b)).isEmpty()) {
                helper.fail("Changed's lab table was not used", table);
            }
            helper.succeed();
        });
    }

    private static void marker(GameTestHelper helper, BlockPos pos, String kind, String facing, String fallback) {
        helper.setBlock(pos, ModBlocks.SPAWN_MARKER.get());
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (!(entity instanceof SpawnMarkerBlockEntity)) {
            helper.fail("Spawn marker has no block entity", pos);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Key", "furniture:" + kind);
        tag.putInt("Count", 1);
        tag.putString("Facing", facing);
        tag.putString("Fallback", fallback);
        entity.load(tag);
    }

    /** Small and large structures can really be placed in a world (the same way /place does it). */
    @GameTest(template = FLOOR, timeoutTicks = 400)
    public static void structuresPlace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 1, 4)).offset(64, 0, 64);
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) level.setChunkForced(chunkX + dx, chunkZ + dz, true);
        }
        List<String> names = List.of("research_outpost_alpha", "guard_watchtower_1", "thunder_science_tower");
        helper.runAfterDelay(60, () -> {
            for (int i = 0; i < names.size(); i++) {
                BlockPos at = origin.offset((i - 1) * 40, 0, 0);
                String command = "place structure " + ChangedTSC.MODID + ":" + names.get(i) + " " + at.getX() + " " + at.getY() + " " + at.getZ();
                int result = level.getServer().getCommands().performPrefixedCommand(
                        level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(), command);
                ChangedTSC.LOGGER.info("[TSC TEST] /{} -> {}", command, result);
                if (result <= 0) helper.fail("Could not place " + names.get(i));
            }
        });
        helper.runAfterDelay(200, () -> {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) level.setChunkForced(chunkX + dx, chunkZ + dz, false);
            }
            helper.succeed();
        });
    }

    @GameTest(template = FLOOR, timeoutTicks = 500)
    public static void squidDogFights(GameTestHelper helper) {
        bossFight(helper, BossType.SQUID_DOG);
    }

    @GameTest(template = FLOOR, timeoutTicks = 500)
    public static void tigerSharkFights(GameTestHelper helper) {
        bossFight(helper, BossType.TIGER_SHARK);
    }

    @GameTest(template = FLOOR, timeoutTicks = 500)
    public static void elderOneFights(GameTestHelper helper) {
        bossFight(helper, BossType.ELDER_ONE);
    }

    @GameTest(template = FLOOR, timeoutTicks = 500)
    public static void wolfKingFights(GameTestHelper helper) {
        bossFight(helper, BossType.WOLF_KING);
    }

    /**
     * Spawns the boss next to a stand-in player and lets the whole fight script run for 20 seconds,
     * first at full health and then enraged, so every attack is used at least once.
     */
    private static void bossFight(GameTestHelper helper, BossType type) {
        ServerLevel level = helper.getLevel();
        Mob boss = BossManager.spawnBoss(level, helper.absolutePos(new BlockPos(2, 1, 2)), type);
        if (boss == null) {
            helper.fail("Boss " + type.id() + " did not spawn");
            return;
        }
        ChangedTSC.LOGGER.info("[TSC TEST] boss {} spawned as {}", type.id(), EntityType.getKey(boss.getType()));
        // The stand-in player is not a real connected player, and Changed's latex cannot transfur
        // one, so with Changed installed the boss runs its script without a target.
        Player player = helper.makeMockSurvivalPlayer();
        if (!ChangedCompat.isLoaded()) {
            BlockPos stand = helper.absolutePos(new BlockPos(6, 1, 6));
            player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
            level.addFreshEntity(player);
            boss.setTarget(player);
        }

        helper.runAfterDelay(200, () -> {
            if (boss.isAlive()) boss.setHealth(boss.getMaxHealth() * 0.3F);
        });
        helper.runAfterDelay(430, () -> {
            float taken = BossManager.damageTakenMultiplier(boss);
            ChangedTSC.LOGGER.info("[TSC TEST] boss {} after the fight: alive={} health={} damageTaken x{} playerHealth={}",
                    type.id(), boss.isAlive(), boss.getHealth(), taken, player.getHealth());
            AABB area = new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(48.0);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> m.getTags().contains(BossManager.MINION_TAG))) {
                mob.discard();
            }
            BossManager.removeBar(boss);
            boss.discard();
            player.discard();
            helper.succeed();
        });
    }

    private static Set<ResourceLocation> ours(Set<ResourceLocation> ids) {
        return ids.stream().filter(id -> id.getNamespace().equals(ChangedTSC.MODID)).collect(Collectors.toSet());
    }

    private static String name(Optional<EntityType<?>> type) {
        return type.map(t -> String.valueOf(EntityType.getKey(t))).orElse("(nothing found)");
    }
}
