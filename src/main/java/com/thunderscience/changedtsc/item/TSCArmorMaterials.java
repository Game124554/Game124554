package com.thunderscience.changedtsc.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Black security armor (protection) and white hazmat suit (latex resistance when fully worn). */
public enum TSCArmorMaterials implements ArmorMaterial {
    GUARD("changed_tsc:guard", 25, new int[]{3, 6, 7, 3}, 12, 1.0F, 0.05F, true),
    HAZMAT("changed_tsc:hazmat", 18, new int[]{1, 4, 5, 2}, 15, 0.0F, 0.0F, false);

    private final String name;
    private final int durabilityMultiplier;
    /** boots, leggings, chestplate, helmet */
    private final int[] protection;
    private final int enchantability;
    private final float toughness;
    private final float knockbackResistance;
    private final boolean metal;

    TSCArmorMaterials(String name, int durabilityMultiplier, int[] protection, int enchantability,
                      float toughness, float knockbackResistance, boolean metal) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protection = protection;
        this.enchantability = enchantability;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.metal = metal;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        int base = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        return base * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> protection[0];
            case LEGGINGS -> protection[1];
            case CHESTPLATE -> protection[2];
            case HELMET -> protection[3];
        };
    }

    @Override public int getEnchantmentValue() { return enchantability; }

    @Override
    public SoundEvent getEquipSound() {
        return metal ? SoundEvents.ARMOR_EQUIP_IRON : SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return metal ? Ingredient.of(Items.IRON_INGOT) : Ingredient.of(Items.WHITE_WOOL);
    }

    @Override public String getName() { return name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
