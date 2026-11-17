package com.thunderscience.changedtsc.registry;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.boss.BossType;
import com.thunderscience.changedtsc.item.AntiGooSyringeItem;
import com.thunderscience.changedtsc.item.BossCoreItem;
import com.thunderscience.changedtsc.item.BossSummonItem;
import com.thunderscience.changedtsc.item.FirstAidKitItem;
import com.thunderscience.changedtsc.item.GunItem;
import com.thunderscience.changedtsc.item.PurifierBladeItem;
import com.thunderscience.changedtsc.item.StunBatonItem;
import com.thunderscience.changedtsc.item.TSCArmorMaterials;
import com.thunderscience.changedtsc.item.TSCTiers;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ChangedTSC.MODID);

    // ---- guns + ammo
    public static final RegistryObject<Item> PISTOL = ITEMS.register("tsc_pistol",
            () -> new GunItem(GunItem.Kind.PISTOL, new Item.Properties().durability(500)));
    public static final RegistryObject<Item> RIFLE = ITEMS.register("tsc_assault_rifle",
            () -> new GunItem(GunItem.Kind.RIFLE, new Item.Properties().durability(900)));
    public static final RegistryObject<Item> SHOTGUN = ITEMS.register("tsc_shotgun",
            () -> new GunItem(GunItem.Kind.SHOTGUN, new Item.Properties().durability(350)));
    public static final RegistryObject<Item> BULLET = ITEMS.register("bullet",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // ---- melee
    public static final RegistryObject<Item> STUN_BATON = ITEMS.register("stun_baton",
            () -> new StunBatonItem(TSCTiers.TSC_STEEL, 3, -2.2F, new Item.Properties()));
    public static final RegistryObject<Item> PURIFIER_BLADE = ITEMS.register("purifier_blade",
            () -> new PurifierBladeItem(TSCTiers.THUNDER, 4, -2.4F, new Item.Properties().rarity(Rarity.RARE)));

    // ---- medical
    public static final RegistryObject<Item> FIRST_AID_KIT = ITEMS.register("first_aid_kit",
            () -> new FirstAidKitItem(false, new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
            () -> new FirstAidKitItem(true, new Item.Properties().stacksTo(32)));

    public static final RegistryObject<Item> ANTI_GOO_SYRINGE = ITEMS.register("anti_goo_syringe",
            () -> new AntiGooSyringeItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // ---- armor: black security set, white hazmat set
    public static final RegistryObject<Item> GUARD_HELMET = ITEMS.register("guard_helmet",
            () -> new ArmorItem(TSCArmorMaterials.GUARD, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> GUARD_CHESTPLATE = ITEMS.register("guard_chestplate",
            () -> new ArmorItem(TSCArmorMaterials.GUARD, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> GUARD_LEGGINGS = ITEMS.register("guard_leggings",
            () -> new ArmorItem(TSCArmorMaterials.GUARD, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> GUARD_BOOTS = ITEMS.register("guard_boots",
            () -> new ArmorItem(TSCArmorMaterials.GUARD, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet",
            () -> new ArmorItem(TSCArmorMaterials.HAZMAT, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_CHESTPLATE = ITEMS.register("hazmat_chestplate",
            () -> new ArmorItem(TSCArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_LEGGINGS = ITEMS.register("hazmat_leggings",
            () -> new ArmorItem(TSCArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots",
            () -> new ArmorItem(TSCArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS, new Item.Properties()));

    // ---- boss summons + boss drops
    public static final RegistryObject<Item> SQUID_DOG_SUMMON = ITEMS.register("squid_dog_summon",
            () -> new BossSummonItem(BossType.SQUID_DOG, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> TIGER_SHARK_SUMMON = ITEMS.register("tiger_shark_summon",
            () -> new BossSummonItem(BossType.TIGER_SHARK, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ELDER_ONE_SUMMON = ITEMS.register("elder_one_summon",
            () -> new BossSummonItem(BossType.ELDER_ONE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> WOLF_KING_SUMMON = ITEMS.register("wolf_king_summon",
            () -> new BossSummonItem(BossType.WOLF_KING, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SQUID_DOG_CORE = ITEMS.register("squid_dog_core",
            () -> new BossCoreItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> TIGER_SHARK_CORE = ITEMS.register("tiger_shark_core",
            () -> new BossCoreItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ELDER_ONE_CORE = ITEMS.register("elder_one_core",
            () -> new BossCoreItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> WOLF_KING_CORE = ITEMS.register("wolf_king_core",
            () -> new BossCoreItem(new Item.Properties().rarity(Rarity.EPIC)));

    // ---- spawn eggs
    public static final RegistryObject<Item> GUARD_SPAWN_EGG = ITEMS.register("tsc_guard_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GUARD, 0x1B1B1F, 0xF2C230, new Item.Properties()));
    public static final RegistryObject<Item> SCIENTIST_SPAWN_EGG = ITEMS.register("tsc_scientist_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SCIENTIST, 0xF4F4F4, 0x3A7BD5, new Item.Properties()));
    public static final RegistryObject<Item> STAFF_SPAWN_EGG = ITEMS.register("assimilation_staff_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STAFF, 0xDADADA, 0x2B2B2B, new Item.Properties()));

    // ---- block items
    public static final RegistryObject<Item> LAB_TILE_WHITE = ITEMS.register("lab_tile_white",
            () -> new BlockItem(ModBlocks.LAB_TILE_WHITE.get(), new Item.Properties()));
    public static final RegistryObject<Item> LAB_TILE_BLACK = ITEMS.register("lab_tile_black",
            () -> new BlockItem(ModBlocks.LAB_TILE_BLACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> TSC_EMBLEM_BLOCK = ITEMS.register("tsc_emblem_block",
            () -> new BlockItem(ModBlocks.TSC_EMBLEM_BLOCK.get(), new Item.Properties()));

    public static Item coreFor(BossType type) {
        return switch (type) {
            case SQUID_DOG -> SQUID_DOG_CORE.get();
            case TIGER_SHARK -> TIGER_SHARK_CORE.get();
            case ELDER_ONE -> ELDER_ONE_CORE.get();
            case WOLF_KING -> WOLF_KING_CORE.get();
        };
    }

    private ModItems() {}
}
