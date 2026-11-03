package com.latexdrone.item;

import com.latexdrone.content.FormData;
import com.latexdrone.content.LatexForm;
import com.latexdrone.infection.InfectionManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Inject yourself to become one of them. If the injector was filled from a creature
 * (Empty Syringe on a latex mob) you become that creature, otherwise a random form.
 */
public class LatexInjectorItem extends Item {
    public LatexInjectorItem(Settings settings) {
        super(settings);
    }

    public static ItemStack filledWith(Item item, LatexForm form) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateNbt().putString("Form", form.id());
        return stack;
    }

    @Nullable
    private static LatexForm storedForm(ItemStack stack) {
        if (stack.hasNbt() && stack.getNbt().contains("Form")) return LatexForm.byId(stack.getNbt().getString("Form"));
        return null;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (InfectionManager.isLatex(user)) return TypedActionResult.fail(stack);
        if (user instanceof ServerPlayerEntity sp) {
            LatexForm form = storedForm(stack);
            if (form == null) form = FormData.FORMS.get(world.random.nextInt(FormData.FORMS.size()));
            InfectionManager.startTransformation(sp, form.index());
            if (!sp.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return storedForm(stack) != null;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        LatexForm form = storedForm(stack);
        if (form != null) {
            tooltip.add(Text.translatable("tooltip.latexdrone.injector_form", Text.translatable("entity.latexdrone." + form.id())).formatted(Formatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Text.translatable("tooltip.latexdrone.injector_random").formatted(Formatting.GRAY));
        }
    }
}
