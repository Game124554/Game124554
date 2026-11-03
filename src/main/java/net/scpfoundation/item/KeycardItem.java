package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KeycardItem extends Item {
    private final int level;

    public KeycardItem(int level, Settings settings) {
        super(settings);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /** Clearance granted by a stack: keycards give their level, SCP-005 opens everything. */
    public static int levelOf(ItemStack stack) {
        if (stack.getItem() instanceof KeycardItem card) return card.level;
        if (stack.isOf(ModItems.SCP_005)) return 6;
        return 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.keycard", level >= 6 ? "O5" : String.valueOf(level)).formatted(Formatting.GRAY));
    }
}
