package com.latexdrone.item;

import com.latexdrone.registry.ModItems;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class LatexToolMaterial implements ToolMaterial {
    public static final LatexToolMaterial INSTANCE = new LatexToolMaterial();

    @Override public int getDurability() { return 900; }
    @Override public float getMiningSpeedMultiplier() { return 6.0f; }
    @Override public float getAttackDamage() { return 3.0f; }
    @Override public int getMiningLevel() { return 2; }
    @Override public int getEnchantability() { return 15; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.ofItems(ModItems.LATEX_GOO); }
}
