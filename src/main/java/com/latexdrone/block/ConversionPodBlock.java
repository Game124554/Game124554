package com.latexdrone.block;

import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Step in, press the button, become a Dark Latex Drone. */
public class ConversionPodBlock extends FacingLabBlock {
    private final int formIndex;

    public ConversionPodBlock(Settings settings, int formIndex) {
        super(settings);
        this.formIndex = formIndex;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            if (InfectionManager.isLatex(sp)) {
                sp.sendMessage(Text.translatable("message.latexdrone.already_latex").formatted(Formatting.GRAY), true);
            } else {
                world.playSound(null, pos, ModSounds.POD_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                InfectionManager.startTransformation(sp, formIndex);
            }
        }
        return ActionResult.success(world.isClient);
    }
}
