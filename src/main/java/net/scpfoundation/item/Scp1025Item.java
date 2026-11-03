package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-1025: read a random entry and you develop the disease's symptoms. SCP-500 or milk will cure them. */
public class Scp1025Item extends Item {
    private record Disease(String key, StatusEffect[] effects, int seconds) {}

    private static final Disease[] DISEASES = {
            new Disease("cold", new StatusEffect[]{StatusEffects.SLOWNESS, StatusEffects.WEAKNESS}, 60),
            new Disease("flu", new StatusEffect[]{StatusEffects.WEAKNESS, StatusEffects.HUNGER, StatusEffects.MINING_FATIGUE}, 90),
            new Disease("food_poisoning", new StatusEffect[]{StatusEffects.NAUSEA, StatusEffects.POISON, StatusEffects.HUNGER}, 30),
            new Disease("malaria", new StatusEffect[]{StatusEffects.WEAKNESS, StatusEffects.NAUSEA, StatusEffects.SLOWNESS}, 90),
            new Disease("vertigo", new StatusEffect[]{StatusEffects.NAUSEA}, 45),
            new Disease("night_blindness", new StatusEffect[]{StatusEffects.BLINDNESS}, 20),
            new Disease("necrosis", new StatusEffect[]{StatusEffects.WITHER}, 15),
            new Disease("insomnia", new StatusEffect[]{StatusEffects.DARKNESS, StatusEffects.SLOWNESS}, 60),
    };

    public Scp1025Item(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            Disease d = DISEASES[world.getRandom().nextInt(DISEASES.length)];
            for (StatusEffect effect : d.effects()) {
                user.addStatusEffect(new StatusEffectInstance(effect, d.seconds() * 20, 0));
            }
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.8f);
            user.sendMessage(Text.translatable("message.scpfoundation.1025_read",
                    Text.translatable("disease.scpfoundation." + d.key())).formatted(Formatting.DARK_GREEN), false);
            user.getItemCooldownManager().set(this, 100);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_1025").formatted(Formatting.GRAY));
    }
}
