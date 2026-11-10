package net.latexdrones.registry;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.latexdrones.LatexDrones;
import net.latexdrones.content.MobDef;
import net.latexdrones.content.MobDefs;
import net.latexdrones.item.FlamethrowerItem;
import net.latexdrones.item.GunItem;
import net.latexdrones.item.HypnoRayItem;
import net.latexdrones.item.MeleeWeaponItem;
import net.latexdrones.item.MopItem;
import net.latexdrones.item.SyringeItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Tiers;

/** The nine weapons, ammo / syringes / materials, 100 spawn eggs and the three creative tabs. */
public final class ModItems {
    private ModItems() {
    }

    /** Weapons and other non-block items, in creative-tab order. */
    public static final List<Item> GEAR = new ArrayList<>();
    public static final List<Item> EGGS = new ArrayList<>();

    // ---- the 9 weapons -------------------------------------------------------------------
    public static final Item STUN_BATON = gear("stun_baton",
            new MeleeWeaponItem(MeleeWeaponItem.Kind.STUN_BATON, Tiers.IRON, 2, -2.2F, new Item.Properties()));
    public static final Item TESLA_PROD = gear("tesla_prod",
            new MeleeWeaponItem(MeleeWeaponItem.Kind.TESLA_PROD, Tiers.DIAMOND, 2, -2.6F, new Item.Properties()));
    public static final Item BONE_SAW = gear("bone_saw",
            new MeleeWeaponItem(MeleeWeaponItem.Kind.BONE_SAW, Tiers.IRON, 1, -1.4F, new Item.Properties()));
    public static final Item LATEX_BLADE = gear("latex_blade",
            new MeleeWeaponItem(MeleeWeaponItem.Kind.LATEX_BLADE, Tiers.DIAMOND, 3, -2.4F, new Item.Properties()));
    public static final Item SYRINGE_GUN = gear("syringe_gun",
            new GunItem(GunItem.Kind.SYRINGE_GUN, new Item.Properties().durability(384)));
    public static final Item TRANQ_RIFLE = gear("tranq_rifle",
            new GunItem(GunItem.Kind.TRANQ_RIFLE, new Item.Properties().durability(320)));
    public static final Item GOO_LAUNCHER = gear("goo_launcher",
            new GunItem(GunItem.Kind.GOO_LAUNCHER, new Item.Properties().durability(256)));
    public static final Item PURIFIER_FLAMETHROWER = gear("purifier_flamethrower",
            new FlamethrowerItem(new Item.Properties().durability(600)));
    public static final Item HYPNO_RAY = gear("hypno_ray",
            new HypnoRayItem(new Item.Properties().durability(200)));

    // ---- ammo, syringes, materials -------------------------------------------------------
    public static final Item RAW_LATEX = gear("raw_latex", new Item(new Item.Properties()));
    public static final Item DRONE_CORE = gear("drone_core", new Item(new Item.Properties()));
    public static final Item DRONE_MOP = gear("drone_mop", new MopItem(new Item.Properties().durability(250)));
    public static final Item LATEX_DART = gear("latex_dart", new Item(new Item.Properties()));
    public static final Item TRANQ_DART = gear("tranq_dart", new Item(new Item.Properties()));
    public static final Item ANTIDOTE_DART = gear("antidote_dart", new Item(new Item.Properties()));
    public static final Item GOO_CANISTER = gear("goo_canister", new Item(new Item.Properties()));
    public static final Item LATEX_SYRINGE = gear("latex_syringe",
            new SyringeItem(SyringeItem.Kind.LATEX, new Item.Properties().stacksTo(16)));
    public static final Item ANTIDOTE_SYRINGE = gear("antidote_syringe",
            new SyringeItem(SyringeItem.Kind.ANTIDOTE, new Item.Properties().stacksTo(16)));
    public static final Item CURE_SERUM = gear("cure_serum",
            new SyringeItem(SyringeItem.Kind.CURE, new Item.Properties().stacksTo(8)));

    private static Item gear(String name, Item item) {
        Registry.register(BuiltInRegistries.ITEM, LatexDrones.id(name), item);
        GEAR.add(item);
        return item;
    }

    public static void init() {
        // one spawn egg per mob; the egg textures are painted per mob, so the tint colours stay white
        for (MobDef def : MobDefs.ALL) {
            Item egg = new SpawnEggItem(def.type, 0xFFFFFF, 0xFFFFFF, new Item.Properties());
            Registry.register(BuiltInRegistries.ITEM, LatexDrones.id(def.name + "_spawn_egg"), egg);
            EGGS.add(egg);
        }

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LatexDrones.id("blocks"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.latexdrones.blocks"))
                .icon(() -> new ItemStack(ModBlocks.BLACK_LATEX_BLOCK))
                .displayItems((parameters, output) -> {
                    for (Item item : ModBlocks.ITEMS) {
                        output.accept(item);
                    }
                })
                .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LatexDrones.id("items"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.latexdrones.items"))
                .icon(() -> new ItemStack(SYRINGE_GUN))
                .displayItems((parameters, output) -> {
                    for (Item item : GEAR) {
                        output.accept(item);
                    }
                })
                .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LatexDrones.id("mobs"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.latexdrones.mobs"))
                .icon(() -> new ItemStack(EGGS.get(6)))
                .displayItems((parameters, output) -> {
                    for (Item item : EGGS) {
                        output.accept(item);
                    }
                })
                .build());
    }
}
