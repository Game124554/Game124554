package net.latexdrones.effect;

import java.util.List;

import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Hypnosis: the victim is slowed, hits weakly and is pulled towards the nearest hypnotist
 * (a scientist or any mob flagged {@code hypno}). Hypnotised mobs forget their target.
 * The swirling screen overlay and the forced camera turn are done on the client (ClientHooks / HudOverlay).
 */
public class HypnosisEffect extends MobEffect {
    public HypnosisEffect() {
        super(MobEffectCategory.HARMFUL, 0xC04DFF);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7c2d9a41-3e5b-4d8a-a1f6-0b5e00000004", -0.35D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, "7c2d9a41-3e5b-4d8a-a1f6-0b5e00000005", -0.5D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            return;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        LatexDroneEntity source = nearestHypnotist(entity, 16.0D);
        if (source == null) {
            return;
        }
        Vec3 to = source.position().subtract(entity.position());
        if (to.lengthSqr() > 6.25D) {
            Vec3 pull = new Vec3(to.x, 0.0D, to.z).normalize().scale(0.16D + 0.05D * amplifier);
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull.x, 0.0D, pull.z));
            entity.hurtMarked = true; // sends the new velocity to the player's client
        }
    }

    /** Nearest living scientist / hypnotic mob within range, or null. Works on both sides. */
    public static LatexDroneEntity nearestHypnotist(LivingEntity entity, double range) {
        List<LatexDroneEntity> list = entity.level().getEntitiesOfClass(LatexDroneEntity.class,
                entity.getBoundingBox().inflate(range), e -> e.isAlive() && e.def().hypno);
        LatexDroneEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LatexDroneEntity e : list) {
            double d = e.distanceToSqr(entity);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }
}
