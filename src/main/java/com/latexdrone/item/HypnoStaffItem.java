package com.latexdrone.item;

import com.latexdrone.registry.ModEffects;
import com.latexdrone.registry.ModSounds;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Weapon 3: projects a hypnotic wave. Everything in front of you is hypnotized (mobs freeze and forget you). */
public class HypnoStaffItem extends Item {
    public HypnoStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0f);
        if (world instanceof ServerWorld sw) {
            List<Entity> hits = world.getOtherEntities(user, user.getBoundingBox().stretch(look.multiply(12.0)).expand(2.0),
                    e -> e instanceof LivingEntity && e.isAlive());
            for (Entity e : hits) {
                Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(eye);
                if (to.length() > 12.0 || to.normalize().dotProduct(look) < 0.85) continue;
                ((LivingEntity) e).addStatusEffect(new StatusEffectInstance(ModEffects.HYPNOSIS, 200, 1));
                sw.spawnParticles(ParticleTypes.WITCH, e.getX(), e.getBodyY(0.8), e.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
            }
            for (int i = 1; i <= 12; i++) {
                Vec3d p = eye.add(look.multiply(i));
                sw.spawnParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 4, 0.15, 0.15, 0.15, 0.2);
            }
            stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.HYPNO_HUM, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(this, 50);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.latexdrone.hypno_staff").formatted(Formatting.GRAY));
    }
}
