package com.mandelahorror.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Right-click: opens a screen where you can draw and write on the paper. */
public class StrangePaperItem extends Item {
    /** Set by the client initializer; opens the paper screen. Does nothing on a dedicated server. */
    public static Consumer<InteractionHand> clientOpener = hand -> { };

    public StrangePaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            clientOpener.accept(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<PaperData.TextEntry> texts = PaperData.readTexts(stack);
        for (int i = 0; i < Math.min(3, texts.size()); i++) {
            PaperData.TextEntry e = texts.get(i);
            tooltip.add(Component.literal("\"" + e.text + "\"")
                    .withStyle(e.color == PaperData.RED ? ChatFormatting.DARK_RED : ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (texts.size() > 3) tooltip.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
        if (PaperData.hasDrawing(stack)) {
            tooltip.add(Component.translatable("tooltip.mandelahorror.paper.drawing").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.mandelahorror.paper.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
