package net.realisticoverhaul.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.mixin.client.ModelPartAccessor;

import java.util.Map;

/**
 * Creepier, more alive mobs (drawing only, no gameplay change):
 * zombies hunch forward, limp and twitch; skeletons rattle; endermen are taller and thinner
 * with a crooked, jerking head; creepers breathe and lean; spiders' legs fidget.
 */
public final class MobAnimations {
	private static final float ZOMBIE_LEAN = 14f;

	private MobAnimations() {}

	private static boolean on(LivingEntity e) {
		return ROConfig.get().creepyMobs && !e.isDead();
	}

	/** End of setupTransforms (whole-body lean and proportions). */
	public static void transforms(LivingEntity e, MatrixStack m, float tickDelta) {
		if (!on(e) || e.isSleeping() || e.hasVehicle()) return;
		float t = e.age + tickDelta;
		if (e instanceof ZombieEntity && !e.isSwimming()) {
			m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ZOMBIE_LEAN));
		} else if (e instanceof CreeperEntity) {
			float breathe = 1f + 0.035f * MathHelper.sin(t * 0.11f);
			m.scale(breathe, 1f, breathe);
			m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-5f));
		} else if (e instanceof EndermanEntity) {
			m.scale(0.86f, 1.07f, 0.86f);
		}
	}

	/** A sudden jerk every few seconds, different for each mob. */
	private static float twitch(LivingEntity e, float t, float strength) {
		float cycle = 55f + (e.getId() % 7) * 9f;
		float phase = (t + e.getId() * 13f) % cycle;
		if (phase > 4f) return 0f;
		return MathHelper.sin(phase / 4f * MathHelper.PI) * strength * ((e.getId() & 1) == 0 ? 1 : -1);
	}

	/** Right after the model's normal pose was set. */
	public static void pose(LivingEntity e, EntityModel<?> model, float tickDelta) {
		if (!on(e)) return;
		float t = e.age + tickDelta;
		float walk = e.limbAnimator.getPos(tickDelta);
		float speed = Math.min(1f, e.limbAnimator.getSpeed(tickDelta));

		if (e instanceof ZombieEntity && model instanceof BipedEntityModel<?> b && !e.isSwimming()) {
			float lean = ZOMBIE_LEAN * MathHelper.RADIANS_PER_DEGREE;
			save(b.head, b.hat, b.body, b.rightArm, b.leftArm, b.rightLeg, b.leftLeg);
			// Limp: the left leg drags, the right leg does the work.
			b.leftLeg.pitch *= 0.45f;
			b.rightLeg.pitch *= 1.15f;
			// Legs stay under the body while the upper body hunches forward.
			b.rightLeg.pitch -= lean;
			b.leftLeg.pitch -= lean;
			b.leftLeg.roll -= 0.08f;
			b.body.roll = MathHelper.sin(walk * 0.6662f) * 0.09f * speed;
			// Head juts forward, lolls to one side and twitches.
			b.head.pitch -= lean * 0.75f;
			b.head.roll += 0.18f + 0.06f * MathHelper.sin(t * 0.045f) + twitch(e, t, 0.45f);
			b.head.yaw += twitch(e, t + 20f, 0.3f);
			b.hat.copyTransform(b.head);
			// Arms hang heavy and sway out of step.
			b.rightArm.pitch += 0.2f + 0.12f * MathHelper.sin(t * 0.067f);
			b.leftArm.pitch += 0.32f + 0.12f * MathHelper.sin(t * 0.067f + 1.7f);
			b.rightArm.roll += 0.06f;
			b.leftArm.roll -= 0.14f;
		} else if (e instanceof AbstractSkeletonEntity && model instanceof BipedEntityModel<?> b) {
			save(b.head, b.hat, b.body);
			b.head.roll += 0.03f * MathHelper.sin(t * 1.9f) * (1f - speed) + twitch(e, t, 0.35f);
			b.head.yaw += twitch(e, t + 31f, 0.5f);
			b.hat.copyTransform(b.head);
			b.body.yaw += 0.02f * MathHelper.sin(t * 2.3f);
		} else if (e instanceof EndermanEntity && model instanceof BipedEntityModel<?> b) {
			save(b.head, b.hat);
			b.head.roll += 0.35f * MathHelper.sin(t * 0.017f + e.getId()) + twitch(e, t, 0.6f);
			b.hat.copyTransform(b.head);
		} else if (e instanceof CreeperEntity && model instanceof SinglePartEntityModel<?> single) {
			ModelPart head = child(single.getPart(), "head");
			if (head != null) {
				save(head);
				head.pitch += 0.14f + 0.05f * MathHelper.sin(t * 0.06f);
				head.roll += twitch(e, t, 0.2f);
			}
		} else if (e instanceof SpiderEntity && model instanceof SinglePartEntityModel<?> single) {
			float idle = 1f - speed;
			Map<String, ModelPart> parts = ((ModelPartAccessor) (Object) single.getPart()).realisticoverhaul$children();
			int i = 0;
			for (Map.Entry<String, ModelPart> en : parts.entrySet()) {
				if (!en.getKey().contains("leg")) continue;
				ModelPart leg = en.getValue();
				save(leg);
				leg.roll += 0.07f * idle * MathHelper.sin(t * 0.9f + i * 1.7f);
				leg.yaw += 0.05f * idle * MathHelper.sin(t * 0.6f + i * 2.3f);
				i++;
			}
		}
	}

	private static ModelPart child(ModelPart root, String name) {
		Map<String, ModelPart> c = ((ModelPartAccessor) (Object) root).realisticoverhaul$children();
		return c.get(name);
	}

	private static void save(ModelPart... parts) {
		for (ModelPart p : parts) Ragdoll.save(p);
	}

	/** Mob types that get glowing eyes (they use the standard humanoid head). */
	public static boolean hasGlowEyes(EntityType<?> type) {
		return type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED
				|| type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON;
	}
}
