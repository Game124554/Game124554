package com.thunderscience.changedtsc.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Trophy dropped by a defeated boss; used to craft the Thunder Purifier Blade. */
public class BossCoreItem extends Item {
    public BossCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
