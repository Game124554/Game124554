package net.latexdrones.item;

import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModSounds;
import net.latexdrones.util.Aim;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Hypno ray: hypnotises the creature you are looking at (16 blocks). Hypnotised mobs forget their target
 * and shuffle about; hypnotised players are slowed and their screen swirls.
 */
public class HypnoRayItem extends Item {
    public HypnoRayItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            LivingEntity target = Aim.pick(player, 16.0D, 0.97D);
            Vec3 from = player.getEyePosition();
            Vec3 end = target != null ? target.getBoundingBox().getCenter() : from.add(player.getLookAngle().scale(8.0D));
            Vec3 step = end.subtract(from).scale(1.0D / 14.0D);
            for (int i = 1; i <= 14; i++) {
                Vec3 p = from.add(step.scale(i));
                server.sendParticles(ParticleTypes.WITCH, p.x, p.y - 0.1D, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
            if (target != null) {
                target.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, 200, 0));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HYPNOSIS, SoundSource.PLAYERS, 0.8F, 1.3F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 40);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
