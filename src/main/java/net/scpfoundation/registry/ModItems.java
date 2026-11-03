package net.scpfoundation.registry;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.item.CandyItem;
import net.scpfoundation.item.KeycardItem;
import net.scpfoundation.item.Scp005Item;
import net.scpfoundation.item.Scp018Item;
import net.scpfoundation.item.Scp063Item;
import net.scpfoundation.item.Scp207Item;
import net.scpfoundation.item.Scp268Item;
import net.scpfoundation.item.Scp294CupItem;
import net.scpfoundation.item.Scp427Item;
import net.scpfoundation.item.Scp500Item;
import net.scpfoundation.item.Scp714Item;
import net.scpfoundation.item.ScpDocumentItem;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final List<Item> ALL = new ArrayList<>();

    public static final Item SCP_DOCUMENT = register("scp_document", new ScpDocumentItem(new Item.Settings().maxCount(16)));

    public static final Item KEYCARD_LEVEL_1 = register("keycard_level_1", new KeycardItem(1, new Item.Settings().maxCount(1)));
    public static final Item KEYCARD_LEVEL_2 = register("keycard_level_2", new KeycardItem(2, new Item.Settings().maxCount(1)));
    public static final Item KEYCARD_LEVEL_3 = register("keycard_level_3", new KeycardItem(3, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item KEYCARD_LEVEL_4 = register("keycard_level_4", new KeycardItem(4, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item KEYCARD_LEVEL_5 = register("keycard_level_5", new KeycardItem(5, new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item KEYCARD_O5 = register("keycard_o5", new KeycardItem(6, new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Item SCP_500 = register("scp_500", new Scp500Item(new Item.Settings().maxCount(16).rarity(Rarity.EPIC)));
    public static final Item SCP_207 = register("scp_207", new Scp207Item(new Item.Settings().maxCount(8).rarity(Rarity.EPIC)));
    public static final Item SCP_018 = register("scp_018", new Scp018Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item SCP_005 = register("scp_005", new Scp005Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item SCP_427 = register("scp_427", new Scp427Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item SCP_714 = register("scp_714", new Scp714Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item SCP_268 = register("scp_268", new Scp268Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item SCP_063 = register("scp_063", new Scp063Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item SCP_294_CUP = register("scp_294_cup", new Scp294CupItem(new Item.Settings().maxCount(16)));
    public static final Item SCP_330_CANDY = register("scp_330_candy", new CandyItem(new Item.Settings().food(
            new FoodComponent.Builder().hunger(2).saturationModifier(0.3f).alwaysEdible().snack().build())));
    public static final Item SCP_458_PIZZA = register("scp_458_pizza", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(7).saturationModifier(0.7f)
                    .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0), 0.25f).build())));

    public static final Item SCP_173_SPAWN_EGG = egg("scp_173", ModEntities.SCP_173, 0x949492, 0x96552A);
    public static final Item SCP_096_SPAWN_EGG = egg("scp_096", ModEntities.SCP_096, 0xE2DCD6, 0x1A0505);
    public static final Item SCP_049_SPAWN_EGG = egg("scp_049", ModEntities.SCP_049, 0x1C1A20, 0xCDC3AC);
    public static final Item SCP_049_2_SPAWN_EGG = egg("scp_049_2", ModEntities.SCP_049_2, 0x7A8C76, 0x969FA5);
    public static final Item SCP_106_SPAWN_EGG = egg("scp_106", ModEntities.SCP_106, 0x261E18, 0x465028);
    public static final Item SCP_682_SPAWN_EGG = egg("scp_682", ModEntities.SCP_682, 0x5C5434, 0xDC1E14);
    public static final Item SCP_939_SPAWN_EGG = egg("scp_939", ModEntities.SCP_939, 0x7D1818, 0xF0EBDC);
    public static final Item SCP_999_SPAWN_EGG = egg("scp_999", ModEntities.SCP_999, 0xF28E1E, 0x781E0A);
    public static final Item SCP_131_SPAWN_EGG = egg("scp_131", ModEntities.SCP_131, 0xE6781E, 0x285FCD);
    public static final Item SCP_529_SPAWN_EGG = egg("scp_529", ModEntities.SCP_529, 0x808082, 0x6EC850);
    public static final Item SCP_1048_SPAWN_EGG = egg("scp_1048", ModEntities.SCP_1048, 0x8C5F37, 0xCDAF82);

    private ModItems() {}

    private static Item egg(String name, net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> type, int primary, int secondary) {
        return register(name + "_spawn_egg", new SpawnEggItem(type, primary, secondary, new Item.Settings()));
    }

    private static Item register(String name, Item item) {
        Registry.register(Registries.ITEM, ScpFoundation.id(name), item);
        ALL.add(item);
        return item;
    }

    public static void register() {
        // Static initialisation registers everything.
    }
}
