package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.scpfoundation.entity.Scp1471Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A phone with SCP-1471 ("MalO ver1.0.0") on it. Use it once to install the app; after that a figure
 * (SCP-1471-A) starts turning up behind you. Use the phone to take a picture; sneak + use tries to uninstall.
 * Only SCP-500 gets rid of it.
 */
public class Scp1471Item extends Item {
    public Scp1471Item(Settings settings) {
        super(settings);
    }

    public static boolean isInstalled(PlayerEntity player) {
        return player.getCommandTags().contains(Scp1471Entity.INSTALLED_TAG);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) return TypedActionResult.success(stack, true);
        user.getItemCooldownManager().set(this, 40);
        if (!isInstalled(player)) {
            player.addCommandTag(Scp1471Entity.INSTALLED_TAG);
            player.sendMessage(Text.translatable("message.scpfoundation.1471_installed").formatted(Formatting.GREEN), false);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.8f, 1.6f);
            return TypedActionResult.success(stack, false);
        }
        if (player.isSneaking()) {
            player.sendMessage(Text.translatable("message.scpfoundation.1471_uninstall").formatted(Formatting.RED), false);
            return TypedActionResult.success(stack, false);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.PLAYERS, 1.0f, 1.4f);
        if (Scp1471Entity.hasActiveFigure(player)) {
            player.sendMessage(Text.translatable("message.scpfoundation.1471_photo_figure").formatted(Formatting.DARK_RED), false);
        } else if (world.getRandom().nextInt(4) == 0 && Scp1471Entity.spawnFor(player)) {
            player.sendMessage(Text.translatable("message.scpfoundation.1471_photo_figure").formatted(Formatting.DARK_RED), false);
        } else {
            player.sendMessage(Text.translatable("message.scpfoundation.1471_photo_clear").formatted(Formatting.GRAY), false);
        }
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_1471").formatted(Formatting.GRAY));
    }
}
