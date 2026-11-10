package net.latexdrones.client;

import net.latexdrones.content.MobDef;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.registry.ModEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Client helpers used by the two mixins: the pose a player strikes while turning into a drone, and
 * the replacement skin once they have.
 */
public final class TransformAnimation {
    private TransformAnimation() {
    }

    /** Replacement skin for a transformed player, or null to keep their own. */
    public static ResourceLocation skinFor(AbstractClientPlayer player) {
        InfectionData.Entry e = InfectionData.CLIENT.get(player.getUUID());
        if (e == null || !e.transformed) {
            return null;
        }
        MobDef def = ModEntities.form(e.form);
        return def == null ? null : def.playerTexture;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /**
     * Called at the end of PlayerModel#setupAnim. Three acts over the transformation:
     * clutching the head and shaking (0-50%), arms thrown wide with the head tipped back (50-85%),
     * then snapping to a rigid drone stance (85-100%).
     */
    public static void apply(PlayerModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!(entity instanceof Player)) {
            return;
        }
        InfectionData.Entry e = InfectionData.CLIENT.get(entity.getUUID());
        if (e == null || e.transformTicks <= 0) {
            return;
        }
        float t = Mth.clamp(e.transformTicks / (float) InfectionData.TRANSFORM_TICKS, 0.0F, 1.0F);
        float clutch = t < 0.5F ? smooth(t / 0.12F) : 1.0F - smooth((t - 0.5F) / 0.1F);
        float spread = t < 0.5F ? 0.0F : (t < 0.85F ? smooth((t - 0.5F) / 0.1F) : 1.0F - smooth((t - 0.85F) / 0.08F));
        float rigid = t < 0.85F ? 0.0F : smooth((t - 0.85F) / 0.08F);
        float weight = Mth.clamp(clutch + spread + rigid, 0.0F, 1.0F);
        if (weight <= 0.001F) {
            return;
        }
        float shake = Mth.sin(ageInTicks * 2.4F) * 0.10F * (clutch + spread);
        // target angles: weighted mix of the three poses (rigid = everything at zero)
        float armX = (clutch * -2.7F) / weight;
        float armZ = (clutch * 0.55F + spread * 1.9F) / weight;
        float headX = (clutch * 0.55F + spread * -0.7F) / weight;

        model.rightArm.xRot = Mth.lerp(weight, model.rightArm.xRot, armX + shake);
        model.rightArm.yRot = Mth.lerp(weight, model.rightArm.yRot, 0.0F);
        model.rightArm.zRot = Mth.lerp(weight, model.rightArm.zRot, armZ);
        model.leftArm.xRot = Mth.lerp(weight, model.leftArm.xRot, armX - shake);
        model.leftArm.yRot = Mth.lerp(weight, model.leftArm.yRot, 0.0F);
        model.leftArm.zRot = Mth.lerp(weight, model.leftArm.zRot, -armZ);
        model.head.xRot = Mth.lerp(weight, model.head.xRot, headX);
        model.head.yRot = Mth.lerp(rigid, model.head.yRot + shake * 2.0F, 0.0F);
        model.rightLeg.xRot = Mth.lerp(weight, model.rightLeg.xRot, 0.0F);
        model.leftLeg.xRot = Mth.lerp(weight, model.leftLeg.xRot, 0.0F);

        // keep the second skin layer glued to the limbs we just moved
        model.hat.copyFrom(model.head);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }
}
