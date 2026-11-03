package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.scpfoundation.data.ScpDatabase;
import net.scpfoundation.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A personnel file for one SCP number (NBT "ScpNumber"); without a number it is a classified, undecrypted document. */
public class ScpDocumentItem extends Item {
    public static final String KEY = "ScpNumber";

    public ScpDocumentItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(int number) {
        ItemStack stack = new ItemStack(ModItems.SCP_DOCUMENT);
        stack.getOrCreateNbt().putInt(KEY, number);
        return stack;
    }

    public static int numberOf(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(KEY) ? nbt.getInt(KEY) : 0;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            int n = numberOf(stack);
            if (n <= 0) {
                n = ScpDatabase.MIN + world.getRandom().nextInt(ScpDatabase.MAX);
                ItemStack decrypted = create(n);
                if (stack.getCount() == 1) {
                    user.setStackInHand(hand, decrypted);
                } else {
                    stack.decrement(1);
                    user.getInventory().offerOrDrop(decrypted);
                }
                user.sendMessage(Text.translatable("message.scpfoundation.decrypted", ScpDatabase.label(n)).formatted(Formatting.GOLD), true);
            }
            for (Text line : ScpDatabase.fileLines(n)) {
                user.sendMessage(line, false);
            }
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public Text getName(ItemStack stack) {
        int n = numberOf(stack);
        if (n > 0) return Text.translatable("item.scpfoundation.scp_document.numbered", ScpDatabase.label(n));
        return super.getName(stack);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        ScpDatabase.Entry e = ScpDatabase.get(numberOf(stack));
        if (e == null) return Rarity.COMMON;
        return switch (e.objectClass()) {
            case "Keter" -> Rarity.RARE;
            case "Thaumiel" -> Rarity.EPIC;
            default -> Rarity.UNCOMMON;
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int n = numberOf(stack);
        if (n <= 0) {
            tooltip.add(Text.translatable("tooltip.scpfoundation.document.classified").formatted(Formatting.GRAY));
            return;
        }
        ScpDatabase.Entry e = ScpDatabase.get(n);
        if (e != null) {
            tooltip.add(Text.literal(e.nickname()).formatted(Formatting.GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("tooltip.scpfoundation.object_class", ScpDatabase.classText(e.objectClass())).formatted(Formatting.GRAY));
            if (e.inGame()) tooltip.add(Text.translatable("tooltip.scpfoundation.in_game").formatted(Formatting.GOLD));
        } else {
            tooltip.add(Text.translatable("tooltip.scpfoundation.object_class",
                    Text.translatable("database.scpfoundation.redacted")).formatted(Formatting.DARK_GRAY));
        }
        tooltip.add(Text.translatable("tooltip.scpfoundation.document.read").formatted(Formatting.DARK_GRAY));
    }
}
