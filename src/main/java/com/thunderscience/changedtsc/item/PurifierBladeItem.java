package com.thunderscience.changedtsc.item;

import com.thunderscience.changedtsc.config.TSCConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Thunder Purifier Blade: extra damage against latex (the bonus is applied in CommonForgeEvents#onHurt). */
public class PurifierBladeItem extends SwordItem {
    public PurifierBladeItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.changed_tsc.purifier",
                String.format("%.1f", TSCConfig.PURIFIER_LATEX_MULTIPLIER.get())).withStyle(ChatFormatting.AQUA));
    }
}
