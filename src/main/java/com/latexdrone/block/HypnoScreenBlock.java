package com.latexdrone.block;

import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModEffects;
import com.latexdrone.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Staring into the screen hypnotizes you. */
public class HypnoScreenBlock extends FacingLabBlock {
    public HypnoScreenBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && !InfectionManager.isLatex(player)) {
            player.addStatusEffect(new StatusEffectInstance(ModEffects.HYPNOSIS, 300, 1));
            world.playSound(null, pos, ModSounds.HYPNO_HUM, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
        return ActionResult.success(world.isClient);
    }
}
