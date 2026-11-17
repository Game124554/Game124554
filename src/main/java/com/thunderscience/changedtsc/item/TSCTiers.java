package com.thunderscience.changedtsc.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum TSCTiers implements Tier {
    TSC_STEEL(2, 600, 6.0F, 2.0F, 12),
    THUNDER(3, 1400, 8.0F, 3.0F, 16);

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantability;

    TSCTiers(int level, int uses, float speed, float damage, int enchantability) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantability = enchantability;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
}
