package net.scpfoundation.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** SCP-330 candy: every piece has a different flavour - and a random short effect. */
public class CandyItem extends Item {
    private static final StatusEffect[] FLAVOURS = {
            StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.REGENERATION, StatusEffects.NIGHT_VISION,
            StatusEffects.HASTE, StatusEffects.LUCK, StatusEffects.FIRE_RESISTANCE, StatusEffects.GLOWING
    };

    public CandyItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            StatusEffect effect = FLAVOURS[world.getRandom().nextInt(FLAVOURS.length)];
            user.addStatusEffect(new StatusEffectInstance(effect, 20 * 30, 0));
        }
        return super.finishUsing(stack, world, user);
    }
}
