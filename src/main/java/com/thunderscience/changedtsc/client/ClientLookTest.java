package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.ChangedTSC;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Automatic "look" test for the client side, used by the GitHub build. It does nothing in a normal
 * game: it only runs when the game is started with -Dchanged_tsc.lookTest=true.
 *
 * It opens every page of the settings menu, creates a flat test world, sets up a small scene
 * (guns, humans, Puro, a boss, furniture, goo in the body), saves a screenshot of each thing into
 * the screenshots folder, and closes the game. Looking at the pictures shows whether the menus,
 * the paper doll and the 3D guns really appear.
 */
@Mod.EventBusSubscriber(modid = ChangedTSC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientLookTest {
    private static final boolean ENABLED = Boolean.getBoolean("changed_tsc.lookTest");
    private static final int PAGES = 7;
    private static final int DONE = 1000;

    private static int step = -1;
    private static int wait;
    private static int ticks;

    private ClientLookTest() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        try {
            if (ticks > 20 * 60 * 6) throw new IllegalStateException("timed out");
            if (wait > 0) {
                wait--;
                return;
            }
            run(mc);
        } catch (Throwable t) {
            ChangedTSC.LOGGER.error("[TSC LOOK] FAILED at step {}", step, t);
            step = DONE;
            mc.stop();
        }
    }

    private static void run(Minecraft mc) {
        if (step >= DONE) return;
        int world = 1 + PAGES * 2; // first step after the settings pages
        if (step == -1) {
            // Wait until the game has finished loading and shows its first screen. That is the title
            // screen, or a first-start screen in front of it (accessibility, Changed's content warning);
            // those are left alone, the test just carries on from there.
            if (mc.getOverlay() == null && mc.screen != null) next(40);
        } else if (step == 0) {
            if (mc.screen instanceof TitleScreen) shot(mc, "01_title_screen");
            next(5);
        } else if (step < world) {
            // each settings page: open it, wait, photograph it
            int page = (step - 1) / 2;
            if ((step - 1) % 2 == 0) {
                mc.setScreen(TSCSettingsScreen.openPageForTest(page));
                next(10);
            } else {
                shot(mc, "02_settings_page_" + (page + 1));
                next(5);
            }
        } else if (step == world) {
            ChangedTSC.LOGGER.info("[TSC LOOK] creating the test world");
            LevelSettings settings = new LevelSettings("TSC look test", GameType.CREATIVE, false, Difficulty.EASY, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("tsc_look_test", settings, new WorldOptions(74110L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions());
            next(20);
        } else if (step == world + 1) {
            if (mc.player == null || mc.level == null || mc.screen != null) return; // still loading
            next(100);
        } else if (step == world + 2) {
            commands(mc, List.of(
                    "time set noon", "weather clear", "gamerule doMobSpawning false",
                    "execute as @p at @s run tp @s ~ ~ ~ 0 8",
                    "give @p changed_tsc:tsc_assault_rifle",
                    // a desk with a computer and a microscope, a chair, boxes and an IV rack, a few blocks ahead
                    marker(-1, 0, 5, "lab_table", "north", "minecraft:smooth_quartz_slab[type=top]"),
                    marker(0, 0, 5, "lab_table", "north", "minecraft:smooth_quartz_slab[type=top]"),
                    marker(-1, 1, 5, "computer", "north", "minecraft:daylight_detector"),
                    marker(0, 1, 5, "microscope", "north", "minecraft:brewing_stand"),
                    marker(0, 0, 4, "office_chair", "south", "minecraft:smooth_quartz_stairs[facing={opposite},half=bottom]"),
                    marker(2, 0, 5, "cardboard_box", "north", "minecraft:barrel[facing=up]"),
                    marker(3, 0, 5, "iron_crate", "north", "minecraft:barrel[facing=up]"),
                    marker(-3, 0, 5, "iv_rack", "north", ""),
                    "execute as @p at @s run summon changed_tsc:tsc_guard ~-3 ~ ~3",
                    "execute as @p at @s run summon changed_tsc:tsc_scientist ~3 ~ ~3",
                    "execute as @p at @s run summon changed_tsc:assimilation_staff ~-2 ~ ~7",
                    "execute as @p at @s positioned ~2 ~ ~7 run tsc puro",
                    "tsc goo @p 60"));
            next(80);
        } else if (step == world + 3) {
            shot(mc, "10_first_person_rifle_doll_furniture");
            commands(mc, List.of("item replace entity @p weapon.mainhand with changed_tsc:tsc_pistol"));
            next(20);
        } else if (step == world + 4) {
            shot(mc, "11_first_person_pistol");
            commands(mc, List.of("item replace entity @p weapon.mainhand with changed_tsc:tsc_shotgun"));
            next(20);
        } else if (step == world + 5) {
            shot(mc, "12_first_person_shotgun");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            next(20);
        } else if (step == world + 6) {
            shot(mc, "13_third_person_back");
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            next(20);
        } else if (step == world + 7) {
            shot(mc, "14_third_person_front");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setScreen(new GooBodyScreen());
            next(20);
        } else if (step == world + 8) {
            shot(mc, "15_body_screen");
            mc.setScreen(null);
            commands(mc, List.of("tsc goo @p 100", "execute as @p at @s positioned ~ ~ ~9 run tsc boss wolf_king"));
            next(60);
        } else if (step == world + 9) {
            shot(mc, "16_boss_bar");
            mc.pauseGame(false);
            next(20);
        } else if (step == world + 10) {
            shot(mc, "17_pause_menu_button");
            next(10);
        } else {
            ChangedTSC.LOGGER.info("[TSC LOOK] finished, all screenshots taken");
            step = DONE;
            mc.stop();
        }
    }

    private static void next(int delay) {
        wait = delay;
        step++;
    }

    private static String marker(int dx, int dy, int dz, String kind, String facing, String fallback) {
        return "execute as @p at @s run setblock ~" + dx + " ~" + dy + " ~" + dz + " changed_tsc:spawn_marker{Key:\"furniture:"
                + kind + "\",Count:1,Facing:\"" + facing + "\",Fallback:\"" + fallback + "\"}";
    }

    private static void commands(Minecraft mc, List<String> commands) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("no integrated server");
        server.execute(() -> {
            CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
            for (String command : commands) {
                int result = server.getCommands().performPrefixedCommand(source, command);
                ChangedTSC.LOGGER.info("[TSC LOOK] /{} -> {}", command, result);
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> ChangedTSC.LOGGER.info("[TSC LOOK] screenshot {}", message.getString()));
    }
}
