package com.thunderscience.changedtsc.config;

import com.thunderscience.changedtsc.boss.BossType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * All settings live in config/changed_tsc-common.toml (created on first launch).
 *
 * Entity "specs" used below accept:
 *   - a plain id, e.g. "changed:dark_latex_wolf_male"
 *   - "auto:keyword" = every entity from a latex namespace whose id contains the keyword
 *   - several options separated by "|" (first one that exists wins), e.g. "auto:puro|auto:dark_latex_wolf_male|auto:dark_latex_wolf|auto:dark_latex|minecraft:wolf"
 * Use /tsc resolve in game to see exactly which entities were found.
 */
public final class TSCConfig {
    public static final ForgeConfigSpec SPEC;

    // general
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LATEX_NAMESPACES;

    // friendly puro
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_PURO_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FRIENDLY_LATEX;
    public static final ForgeConfigSpec.ConfigValue<String> FRIENDLY_SPAWN_ENTITY;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_RETALIATE;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FOLLOW_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIGHT_HOSTILES;

    // stealth
    public static final ForgeConfigSpec.BooleanValue STEALTH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SNEAK_NOTICE_RANGE;
    public static final ForgeConfigSpec.DoubleValue SNEAK_VISIBILITY;
    public static final ForgeConfigSpec.DoubleValue DARKNESS_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue STEALTH_VS_BOSSES;

    // first aid
    public static final ForgeConfigSpec.BooleanValue FIRST_AID_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FIRST_AID_HEAL;
    public static final ForgeConfigSpec.DoubleValue BANDAGE_HEAL;
    public static final ForgeConfigSpec.IntValue FIRST_AID_USE_TICKS;
    public static final ForgeConfigSpec.IntValue FIRST_AID_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue FIRST_AID_CURES_EFFECTS;
    public static final ForgeConfigSpec.IntValue FIRST_AID_REGEN_TICKS;

    // First Aid mod (ichttt's "First Aid", mod id firstaid) compatibility
    public static final ForgeConfigSpec.BooleanValue FA_MOD_COMPAT;
    public static final ForgeConfigSpec.BooleanValue FA_MOD_LOOT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FA_MOD_LOOT_ITEMS;
    public static final ForgeConfigSpec.BooleanValue FA_MOD_MORPHINE;
    public static final ForgeConfigSpec.BooleanValue FA_MOD_ONLY;

    // menu
    public static final ForgeConfigSpec.BooleanValue MENU_BUTTON;

    // furniture
    public static final ForgeConfigSpec.BooleanValue FURNITURE_USE_CHANGED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FURNITURE_KINDS;

    // paper doll
    public static final List<String> DOLL_POSITIONS =
            List.of("top_left", "middle_left", "bottom_left", "top_right", "middle_right", "bottom_right");
    public static final ForgeConfigSpec.BooleanValue DOLL_HUD;
    public static final ForgeConfigSpec.BooleanValue DOLL_ALWAYS;
    public static final ForgeConfigSpec.ConfigValue<String> DOLL_POSITION;
    public static final ForgeConfigSpec.IntValue DOLL_SCALE;
    public static final ForgeConfigSpec.BooleanValue DOLL_PAWS;
    public static final ForgeConfigSpec.BooleanValue DOLL_CHANGED_PROGRESS;

    // weapons
    public static final ForgeConfigSpec.BooleanValue GUNS_REQUIRE_AMMO;
    public static final ForgeConfigSpec.DoubleValue PISTOL_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue RIFLE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SHOTGUN_PELLET_DAMAGE;
    public static final ForgeConfigSpec.IntValue PISTOL_COOLDOWN;
    public static final ForgeConfigSpec.IntValue RIFLE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SHOTGUN_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue BULLET_LATEX_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue PURIFIER_LATEX_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HAZMAT_LATEX_REDUCTION;

    // vanilla mobs
    public static final ForgeConfigSpec.BooleanValue REMOVE_VANILLA_HOSTILES;
    public static final ForgeConfigSpec.BooleanValue REMOVE_VANILLA_BOSSES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VANILLA_WHITELIST;

    // npcs
    public static final ForgeConfigSpec.DoubleValue GUARD_BULLET_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue NPCS_FIGHT_VANILLA_MONSTERS;
    public static final ForgeConfigSpec.BooleanValue SCIENTISTS_HEAL;
    public static final ForgeConfigSpec.BooleanValue LATEX_HUNT_HUMANS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_LATEX_POOL;
    public static final ForgeConfigSpec.DoubleValue MARKER_TRIGGER_RANGE;

    // goo infection
    public static final ForgeConfigSpec.BooleanValue GOO_ENABLED;
    public static final ForgeConfigSpec.BooleanValue GOO_INFECT_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue GOO_INFECT_HUMANS;
    public static final ForgeConfigSpec.DoubleValue GOO_PER_HIT;
    public static final ForgeConfigSpec.DoubleValue GOO_BOSS_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue GOO_STAGE2_AT;
    public static final ForgeConfigSpec.DoubleValue GOO_STAGE3_AT;
    public static final ForgeConfigSpec.DoubleValue GOO_RECOVERY_STAGE1;
    public static final ForgeConfigSpec.DoubleValue GOO_SPREAD_STAGE2;
    public static final ForgeConfigSpec.DoubleValue GOO_SPREAD_STAGE3;
    public static final ForgeConfigSpec.BooleanValue GOO_SYMPTOMS;
    public static final ForgeConfigSpec.BooleanValue GOO_BODY_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue GOO_KIT_CURE;
    public static final ForgeConfigSpec.DoubleValue GOO_BANDAGE_CURE;
    public static final ForgeConfigSpec.ConfigValue<String> GOO_TRANSFUR_COMMAND;
    public static final ForgeConfigSpec.ConfigValue<String> GOO_REGRESS_COMMAND;
    public static final ForgeConfigSpec.ConfigValue<String> GOO_TRANSFUR_FALLBACK;

    // bosses
    public static final ForgeConfigSpec.BooleanValue BOSSES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BOSS_TRIGGER_RANGE;
    public static final ForgeConfigSpec.IntValue MAX_MINIONS;
    public static final Map<BossType, ForgeConfigSpec.ConfigValue<String>> BOSS_ENTITY = new EnumMap<>(BossType.class);
    public static final Map<BossType, ForgeConfigSpec.ConfigValue<String>> BOSS_MINIONS = new EnumMap<>(BossType.class);
    public static final Map<BossType, ForgeConfigSpec.DoubleValue> BOSS_HEALTH = new EnumMap<>(BossType.class);
    public static final Map<BossType, ForgeConfigSpec.DoubleValue> BOSS_DAMAGE = new EnumMap<>(BossType.class);

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        LATEX_NAMESPACES = b.comment("Mod namespaces whose mobs count as 'latex' (Changed and its add-ons).")
                .defineList("latexNamespaces", List.of("changed", "changed_addon"), o -> o instanceof String);
        b.pop();

        b.push("friendlyPuro");
        FRIENDLY_PURO_ENABLED = b.comment("Make Puro (and anything in friendlyLatex) friendly to players and TSC humans.")
                .define("enabled", true);
        FRIENDLY_LATEX = b.comment("Entity specs that are friendly. 'auto:puro' = every Changed entity with 'puro' in its id.")
                .defineList("friendlyLatex", List.of("auto:puro"), o -> o instanceof String);
        FRIENDLY_SPAWN_ENTITY = b.comment("Which entity is spawned as Puro (structures and /tsc puro). The one that is spawned is tagged",
                        "'changed_tsc_friendly' and named Puro, so it is friendly even if its entity type is a normal dark latex wolf.")
                .define("friendlySpawnEntity", "auto:puro|changed:dark_latex_wolf_male|auto:dark_latex_wolf|minecraft:wolf");
        FRIENDLY_RETALIATE = b.comment("If true, a friendly Puro you hit will fight back.")
                .define("retaliateWhenHit", false);
        FRIENDLY_FOLLOW_PLAYERS = b.comment("Friendly Puro follow the nearest player.")
                .define("followPlayers", true);
        FRIENDLY_FIGHT_HOSTILES = b.comment("Friendly Puro attack hostile latex and monsters.")
                .define("fightHostiles", true);
        b.pop();

        b.push("stealth");
        STEALTH_ENABLED = b.comment("Sneaking makes latex not notice you unless you are close.")
                .define("enabled", true);
        SNEAK_NOTICE_RANGE = b.comment("Latex only notice a sneaking player within this many blocks.")
                .defineInRange("sneakNoticeRange", 4.0, 0.0, 64.0);
        SNEAK_VISIBILITY = b.comment("Visibility multiplier for sneaking players seen by latex (lower = harder to see).")
                .defineInRange("sneakVisibility", 0.3, 0.0, 1.0);
        DARKNESS_RANGE_MULTIPLIER = b.comment("Notice range multiplier when the player stands in darkness (light < 7).")
                .defineInRange("darknessRangeMultiplier", 0.6, 0.0, 1.0);
        STEALTH_VS_BOSSES = b.comment("Does stealth also work against bosses?")
                .define("worksAgainstBosses", false);
        b.pop();

        b.push("firstAid");
        FIRST_AID_ENABLED = b.define("enabled", true);
        FIRST_AID_HEAL = b.comment("Health restored by a First Aid Kit (2 = one heart).")
                .defineInRange("kitHeal", 12.0, 0.0, 100.0);
        BANDAGE_HEAL = b.defineInRange("bandageHeal", 4.0, 0.0, 100.0);
        FIRST_AID_USE_TICKS = b.comment("Ticks needed to apply a First Aid Kit (20 = 1 second).")
                .defineInRange("kitUseTicks", 40, 1, 400);
        FIRST_AID_COOLDOWN = b.defineInRange("cooldownTicks", 100, 0, 6000);
        FIRST_AID_CURES_EFFECTS = b.comment("First Aid Kit removes harmful effects.")
                .define("curesHarmfulEffects", true);
        FIRST_AID_REGEN_TICKS = b.defineInRange("regenerationTicks", 100, 0, 6000);
        b.pop();

        b.push("firstAidMod");
        FA_MOD_COMPAT = b.comment("Work together with the First Aid mod (mod id 'firstaid') when it is installed.",
                        "Nothing in this section does anything if that mod is missing.")
                .define("enabled", true);
        FA_MOD_LOOT = b.comment("Put the First Aid mod's own items into TSC structure chests and into scientist / boss drops.",
                        "Chest loot changes need /reload or a world restart.")
                .define("addItemsToLootAndDrops", true);
        FA_MOD_LOOT_ITEMS = b.comment("Which First Aid mod items to add. Ids that do not exist are skipped.")
                .defineList("lootItems", List.of("firstaid:bandage", "firstaid:plaster", "firstaid:morphine"), o -> o instanceof String);
        FA_MOD_MORPHINE = b.comment("While you are under the First Aid mod's morphine effect the goo stops spreading and its symptoms are numbed.")
                .define("morphineHoldsBackGoo", true);
        FA_MOD_ONLY = b.comment("Leave all health healing to the First Aid mod: the TSC First Aid Kit and Bandage",
                        "then only push goo out of the body (and cure effects), they do not restore health.")
                .define("healthOnlyFromFirstAidMod", false);
        b.pop();

        b.push("menu");
        MENU_BUTTON = b.comment("Show the 'TSC Settings' button on the title screen and the pause menu.",
                        "The settings screen can always be opened with its key (default O) or from the Mods list.")
                .define("showMenuButton", true);
        b.pop();

        b.push("furniture");
        FURNITURE_USE_CHANGED = b.comment("Furnish the structures with the Changed mod's own furniture (lab tables, office chairs,",
                        "computers, microscopes, boxes...). Off = plain vanilla stand-ins (slabs, stairs, barrels).",
                        "Only affects structures that have not been visited yet.")
                .define("useChangedBlocks", true);
        FURNITURE_KINDS = b.comment("Which Changed block is used for each kind of furniture: kind=name1|name2|...",
                        "A plain word finds the block whose id contains it (exact name first, then the shortest).",
                        "Write a full id to force one, e.g. \"computer=changed:computer\". /tsc resolve shows what was found.")
                .defineList("kinds", List.of(
                        "lab_table=lab_table|table|desk",
                        "office_chair=office_chair|chair|stool",
                        "computer=computer|laptop|terminal",
                        "microscope=microscope",
                        "petri_dish=petri_dish|petri",
                        "clipboard=clipboard|note",
                        "book_stack=book_stack|books",
                        "canned_peaches=canned_peaches|peaches|canned",
                        "cardboard_box=cardboard_box|cardboard",
                        "box_pile=box_pile|cardboard_box",
                        "iron_crate=iron_crate|crate",
                        "speaker=speaker",
                        "iv_rack=bedside_iv_rack|iv_rack"), o -> o instanceof String);
        b.pop();

        b.push("paperDoll");
        DOLL_HUD = b.comment("Show the paper doll on screen: a small body figure (like the First Aid mod's) that shows",
                        "where the goo is. Human body parts are skin colored, goo parts turn black or white,",
                        "and when you are transfurred the doll gets ears, a tail and paws.")
                .define("showOnHud", true);
        DOLL_ALWAYS = b.comment("Show the doll even while your body is clean. Off = it only appears when there is goo or you are transfurred.")
                .define("showWhenClean", false);
        DOLL_POSITION = b.comment("Where the doll sits: top_left, middle_left, bottom_left, top_right, middle_right, bottom_right.",
                        "The First Aid mod puts its own figure top_left by default, so middle_left keeps them apart.")
                .defineInList("position", "middle_left", DOLL_POSITIONS);
        DOLL_SCALE = b.comment("Size of the doll on the HUD.").defineInRange("scale", 2, 1, 4);
        DOLL_PAWS = b.comment("Mark hands and feet with a paw print once they have turned into paws.")
                .define("markPaws", true);
        DOLL_CHANGED_PROGRESS = b.comment("Also show the Changed mod's own transfur progress on the doll (whichever is higher).")
                .define("showChangedProgress", true);
        b.pop();

        b.push("weapons");
        GUNS_REQUIRE_AMMO = b.define("gunsRequireAmmo", true);
        PISTOL_DAMAGE = b.defineInRange("pistolDamage", 6.0, 0.0, 200.0);
        RIFLE_DAMAGE = b.defineInRange("rifleDamage", 5.0, 0.0, 200.0);
        SHOTGUN_PELLET_DAMAGE = b.defineInRange("shotgunPelletDamage", 3.0, 0.0, 200.0);
        PISTOL_COOLDOWN = b.defineInRange("pistolCooldownTicks", 8, 0, 200);
        RIFLE_COOLDOWN = b.defineInRange("rifleCooldownTicks", 3, 0, 200);
        SHOTGUN_COOLDOWN = b.defineInRange("shotgunCooldownTicks", 22, 0, 200);
        BULLET_LATEX_MULTIPLIER = b.comment("Bullet damage multiplier against latex.")
                .defineInRange("bulletLatexMultiplier", 1.25, 0.0, 10.0);
        PURIFIER_LATEX_MULTIPLIER = b.comment("Purifier Blade damage multiplier against latex.")
                .defineInRange("purifierLatexMultiplier", 2.0, 0.0, 10.0);
        HAZMAT_LATEX_REDUCTION = b.comment("Damage reduction from latex while wearing the full Hazmat suit (0.5 = half damage).")
                .defineInRange("hazmatLatexReduction", 0.5, 0.0, 1.0);
        b.pop();

        b.push("vanillaMobs");
        REMOVE_VANILLA_HOSTILES = b.comment("Remove all vanilla (minecraft:) hostile mobs: zombies, skeletons, creepers, spiders...")
                .define("removeVanillaHostiles", false);
        REMOVE_VANILLA_BOSSES = b.comment("Also remove vanilla bosses (Ender Dragon, Wither, Elder Guardian, Warden).",
                        "WARNING: removing the Ender Dragon means the End fight cannot happen.")
                .define("removeVanillaBosses", false);
        VANILLA_WHITELIST = b.comment("Vanilla hostiles that are never removed, e.g. [\"minecraft:slime\"]")
                .defineList("whitelist", List.of(), o -> o instanceof String);
        b.pop();

        b.push("npcs");
        GUARD_BULLET_DAMAGE = b.defineInRange("guardBulletDamage", 4.0, 0.0, 100.0);
        NPCS_FIGHT_VANILLA_MONSTERS = b.define("fightVanillaMonsters", true);
        SCIENTISTS_HEAL = b.comment("Scientists use first aid on hurt players, humans and friendly Puro nearby.")
                .define("scientistsHeal", true);
        LATEX_HUNT_HUMANS = b.comment("Hostile latex actively hunt TSC guards, scientists and staff.")
                .define("latexHuntHumans", true);
        STRUCTURE_LATEX_POOL = b.comment("Latex that spawn in structures. Empty = every hostile entity from the latex namespaces.")
                .defineList("structureLatexPool", List.of(), o -> o instanceof String);
        MARKER_TRIGGER_RANGE = b.comment("Structure mobs appear when a player comes within this range.")
                .defineInRange("markerTriggerRange", 28.0, 4.0, 128.0);
        b.pop();

        b.push("gooInfection");
        GOO_ENABLED = b.comment("Latex hits put goo inside your body. It grows through 3 stages:",
                        "  1 Goo Inside Body (human body)  ->  2 Furry Body  ->  3 Transfurring  ->  100% = transfur.",
                        "First Aid Kits, Bandages and the Anti-Goo Syringe push it back.")
                .define("enabled", true);
        GOO_INFECT_PLAYERS = b.define("infectPlayers", true);
        GOO_INFECT_HUMANS = b.comment("TSC guards / scientists / staff can be infected and transfur into latex too.")
                .define("infectHumans", true);
        GOO_PER_HIT = b.comment("Infection % added per latex hit (full Hazmat suit reduces it).")
                .defineInRange("infectionPerHit", 8.0, 0.0, 100.0);
        GOO_BOSS_MULTIPLIER = b.defineInRange("bossHitMultiplier", 2.0, 0.0, 10.0);
        GOO_STAGE2_AT = b.comment("Infection % where Furry Body starts.").defineInRange("furryBodyAt", 34.0, 1.0, 99.0);
        GOO_STAGE3_AT = b.comment("Infection % where Transfurring starts.").defineInRange("transfurringAt", 67.0, 1.0, 99.0);
        GOO_RECOVERY_STAGE1 = b.comment("% per second the body fights off goo during stage 1.")
                .defineInRange("recoveryPerSecondStage1", 0.15, 0.0, 10.0);
        GOO_SPREAD_STAGE2 = b.comment("% per second the goo spreads by itself during Furry Body.")
                .defineInRange("spreadPerSecondStage2", 0.2, 0.0, 10.0);
        GOO_SPREAD_STAGE3 = b.comment("% per second the goo spreads by itself during Transfurring.")
                .defineInRange("spreadPerSecondStage3", 0.4, 0.0, 10.0);
        GOO_SYMPTOMS = b.comment("Stage symptoms: hunger, slowness, weakness, nausea, darkness, messages.")
                .define("symptoms", true);
        GOO_BODY_OVERLAY = b.comment("Show goo and fur spreading over the body (client side).")
                .define("bodyOverlay", true);
        GOO_KIT_CURE = b.comment("Infection % removed by a First Aid Kit.").defineInRange("firstAidKitCure", 50.0, 0.0, 100.0);
        GOO_BANDAGE_CURE = b.comment("Infection % removed by a Bandage (also used by scientists).")
                .defineInRange("bandageCure", 12.0, 0.0, 100.0);
        GOO_TRANSFUR_COMMAND = b.comment("Command run at 100% to transfur a player with the Changed mod (run as server, output hidden).",
                        "Placeholders: {player}, {form} (the form of the latex that infected you; several spellings are tried",
                        "until Changed accepts one), {entity} (id of that latex), {entity_path}.",
                        "To always become one form, write it out, e.g. \"transfur {player} changed:form_dark_latex_wolf/male\"",
                        "(see the exact ids with tab-complete on /transfur). Empty = always use the fallback.")
                .define("transfurCommand", "transfur {player} {form}");
        GOO_REGRESS_COMMAND = b.comment("Command run when first aid treats a player, to also lower Changed's own transfur progress.",
                        "Placeholders: {player} {amount} (cure % / 10). Empty = off.")
                .define("regressCommand", "regresstf {player} {amount}");
        GOO_TRANSFUR_FALLBACK = b.comment("What happens at 100% if the transfur command is empty or fails:",
                        "  kill_and_spawn = you die to the goo and a latex appears in your place",
                        "  spawn_only     = a latex appears, you survive with nausea",
                        "  none           = nothing (infection resets)")
                .define("transfurFallback", "kill_and_spawn");
        b.pop();

        b.push("bosses");
        BOSSES_ENABLED = b.define("enabled", true);
        BOSS_TRIGGER_RANGE = b.comment("A boss altar in a structure awakens when a player is this close.")
                .defineInRange("altarTriggerRange", 12.0, 2.0, 64.0);
        MAX_MINIONS = b.comment("Max minions a boss keeps alive around itself.")
                .defineInRange("maxMinions", 6, 0, 50);
        for (BossType type : BossType.values()) {
            b.push(type.id());
            BOSS_ENTITY.put(type, b.comment("Base entity spec for this boss.")
                    .define("entity", type.defaultEntity()));
            BOSS_MINIONS.put(type, b.comment("Minion entity spec ('pool' = random structure latex).")
                    .define("minions", type.defaultMinions()));
            BOSS_HEALTH.put(type, b.defineInRange("health", type.defaultHealth(), 20.0, 1024.0));
            BOSS_DAMAGE.put(type, b.defineInRange("attackDamage", type.defaultDamage(), 1.0, 200.0));
            b.pop();
        }
        b.pop();

        SPEC = b.build();
    }

    private TSCConfig() {}
}
