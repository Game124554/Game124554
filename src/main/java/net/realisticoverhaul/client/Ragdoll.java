package net.realisticoverhaul.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.mixin.client.ModelPartAccessor;
import net.realisticoverhaul.mixin.client.QuadrupedEntityModelAccessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Limb ragdoll for corpses. Every dead body gets a small spring simulation per limb
 * (head, two arms / front legs, two legs, tail). Limbs flop into a random resting pose
 * when the body falls, and swing again when it is kicked, pushed or lands.
 */
public final class Ragdoll {
	private static final int HEAD = 0, R_ARM = 1, L_ARM = 2, R_LEG = 3, L_LEG = 4, TAIL = 5, LIMBS = 6;
	private static final float STIFFNESS = 0.10f;
	private static final float DAMPING = 0.28f;
	private static final float MAX_ANGLE = 2.6f;

	private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

	/** Parts changed for the current frame, restored after the entity is drawn. */
	private static final List<Saved> SAVED = new ArrayList<>();
	private static final List<Integer> SAVE_MARKS = new ArrayList<>();

	private Ragdoll() {}

	private static final class Limb {
		float pitch, roll, yaw;
		float prevPitch, prevRoll, prevYaw;
		float vPitch, vRoll, vYaw;
		float restPitch, restRoll, restYaw;
		float kickPitch, kickRoll;
	}

	private static final class State {
		final Limb[] limbs = new Limb[LIMBS];
		double lastX, lastY, lastZ;
		double velX, velY, velZ;
		boolean started;
	}

	private record Saved(ModelPart part, float pitch, float yaw, float roll) {}

	public static boolean active() {
		ROConfig c = ROConfig.get();
		return c.corpses && c.ragdollLimbs;
	}

	public static void clear() {
		STATES.clear();
		SAVED.clear();
		SAVE_MARKS.clear();
	}

	private static State create(LivingEntity e) {
		Random r = new Random(e.getUuid().getLeastSignificantBits() ^ e.getId());
		State s = new State();
		for (int i = 0; i < LIMBS; i++) s.limbs[i] = new Limb();
		Limb head = s.limbs[HEAD];
		head.restPitch = 0.25f + r.nextFloat() * 0.6f;
		head.restYaw = (r.nextFloat() - 0.5f) * 1.4f;
		head.restRoll = (r.nextFloat() - 0.5f) * 0.8f;
		for (int side = 0; side < 2; side++) {
			float sign = side == 0 ? 1f : -1f; // + = outward for the right side
			Limb arm = s.limbs[side == 0 ? R_ARM : L_ARM];
			arm.restPitch = -1.7f + r.nextFloat() * 2.1f;
			arm.restRoll = sign * (0.15f + r.nextFloat() * 1.1f);
			arm.restYaw = (r.nextFloat() - 0.5f) * 0.6f;
			Limb leg = s.limbs[side == 0 ? R_LEG : L_LEG];
			leg.restPitch = (r.nextFloat() - 0.5f) * 1.1f;
			leg.restRoll = sign * (0.05f + r.nextFloat() * 0.45f);
			leg.restYaw = (r.nextFloat() - 0.5f) * 0.4f;
		}
		Limb tail = s.limbs[TAIL];
		tail.restPitch = (r.nextFloat() - 0.5f) * 1.2f;
		tail.restYaw = (r.nextFloat() - 0.5f) * 1.2f;
		for (Limb l : s.limbs) {
			// Each limb reacts a bit differently to bumps.
			l.kickPitch = (r.nextFloat() - 0.5f) * 6f;
			l.kickRoll = (r.nextFloat() - 0.5f) * 6f;
		}
		return s;
	}

	/** Called every client tick. */
	public static void tick(ClientWorld world) {
		if (!active()) {
			STATES.clear();
			return;
		}
		Iterator<Map.Entry<LivingEntity, State>> it = STATES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<LivingEntity, State> en = it.next();
			LivingEntity e = en.getKey();
			if (e == null || e.isRemoved() || !e.isDead() || e.getWorld() != world) {
				it.remove();
				continue;
			}
			step(en.getValue(), e);
		}
	}

	private static void step(State s, LivingEntity e) {
		double vx = e.getX() - s.lastX, vy = e.getY() - s.lastY, vz = e.getZ() - s.lastZ;
		if (!s.started) {
			vx = vy = vz = 0;
			s.started = true;
		}
		s.lastX = e.getX();
		s.lastY = e.getY();
		s.lastZ = e.getZ();
		float ay = (float) MathHelper.clamp(vy - s.velY, -1.0, 1.0);
		float ah = (float) MathHelper.clamp(Math.sqrt((vx - s.velX) * (vx - s.velX) + (vz - s.velZ) * (vz - s.velZ)), 0.0, 1.0);
		s.velX = vx;
		s.velY = vy;
		s.velZ = vz;

		for (Limb l : s.limbs) {
			l.prevPitch = l.pitch;
			l.prevRoll = l.roll;
			l.prevYaw = l.yaw;
			float bump = Math.abs(ay) + ah;
			l.vPitch += -STIFFNESS * (l.pitch - l.restPitch) - DAMPING * l.vPitch + bump * l.kickPitch * 0.25f;
			l.vRoll += -STIFFNESS * (l.roll - l.restRoll) - DAMPING * l.vRoll + bump * l.kickRoll * 0.25f;
			l.vYaw += -STIFFNESS * (l.yaw - l.restYaw) - DAMPING * l.vYaw;
			l.pitch = MathHelper.clamp(l.pitch + l.vPitch, -MAX_ANGLE, MAX_ANGLE);
			l.roll = MathHelper.clamp(l.roll + l.vRoll, -MAX_ANGLE, MAX_ANGLE);
			l.yaw = MathHelper.clamp(l.yaw + l.vYaw, -MAX_ANGLE, MAX_ANGLE);
		}
	}

	// ---------------------------------------------------------------- rendering

	/** Start of LivingEntityRenderer.render. */
	public static void beginRender() {
		SAVE_MARKS.add(SAVED.size());
	}

	/** End of LivingEntityRenderer.render: put the model back the way it was. */
	public static void endRender() {
		if (SAVE_MARKS.isEmpty()) return;
		int mark = SAVE_MARKS.remove(SAVE_MARKS.size() - 1);
		for (int i = SAVED.size() - 1; i >= mark; i--) {
			Saved sv = SAVED.remove(i);
			sv.part.pitch = sv.pitch;
			sv.part.yaw = sv.yaw;
			sv.part.roll = sv.roll;
		}
	}

	/** Right after the model's normal pose was set: bend the limbs of a corpse. */
	public static void pose(LivingEntity e, EntityModel<?> model, float tickDelta) {
		if (!e.isDead() || !active()) return;
		State s = STATES.computeIfAbsent(e, Ragdoll::create);

		if (model instanceof BipedEntityModel<?> biped) {
			set(biped.head, s.limbs[HEAD], tickDelta, false);
			set(biped.hat, s.limbs[HEAD], tickDelta, false);
			set(biped.rightArm, s.limbs[R_ARM], tickDelta, false);
			set(biped.leftArm, s.limbs[L_ARM], tickDelta, false);
			set(biped.rightLeg, s.limbs[R_LEG], tickDelta, false);
			set(biped.leftLeg, s.limbs[L_LEG], tickDelta, false);
			if (model instanceof PlayerEntityModel<?> pm) {
				copy(pm.rightSleeve, pm.rightArm);
				copy(pm.leftSleeve, pm.leftArm);
				copy(pm.rightPants, pm.rightLeg);
				copy(pm.leftPants, pm.leftLeg);
			}
		} else if (model instanceof QuadrupedEntityModel<?> && model instanceof QuadrupedEntityModelAccessor q) {
			set(q.realisticoverhaul$head(), s.limbs[HEAD], tickDelta, false);
			set(q.realisticoverhaul$rightFrontLeg(), s.limbs[R_ARM], tickDelta, false);
			set(q.realisticoverhaul$leftFrontLeg(), s.limbs[L_ARM], tickDelta, false);
			set(q.realisticoverhaul$rightHindLeg(), s.limbs[R_LEG], tickDelta, false);
			set(q.realisticoverhaul$leftHindLeg(), s.limbs[L_LEG], tickDelta, false);
		} else if (model instanceof SinglePartEntityModel<?> single) {
			walk(single.getPart(), s, tickDelta, 0);
		}
	}

	private static void walk(ModelPart part, State s, float tickDelta, int depth) {
		if (depth > 3) return;
		Map<String, ModelPart> children = ((ModelPartAccessor) (Object) part).realisticoverhaul$children();
		for (Map.Entry<String, ModelPart> en : children.entrySet()) {
			int limb = limbFor(en.getKey());
			if (limb >= 0) {
				set(en.getValue(), s.limbs[limb], tickDelta, true);
			} else {
				walk(en.getValue(), s, tickDelta, depth + 1);
			}
		}
	}

	private static int limbFor(String name) {
		String n = name.toLowerCase();
		if (n.equals("head")) return HEAD;
		if (n.contains("tail")) return TAIL;
		boolean right = n.contains("right");
		boolean left = n.contains("left");
		if (n.equals("arms")) return R_ARM;
		if (n.contains("arm") || n.contains("wing") || n.contains("front_leg")) return right ? R_ARM : left ? L_ARM : -1;
		if (n.contains("leg")) return right ? R_LEG : left ? L_LEG : -1;
		return -1;
	}

	private static void set(ModelPart part, Limb l, float tickDelta, boolean additive) {
		if (part == null) return;
		SAVED.add(new Saved(part, part.pitch, part.yaw, part.roll));
		float p = MathHelper.lerp(tickDelta, l.prevPitch, l.pitch);
		float r = MathHelper.lerp(tickDelta, l.prevRoll, l.roll);
		float y = MathHelper.lerp(tickDelta, l.prevYaw, l.yaw);
		if (additive) {
			part.pitch += p * 0.6f;
			part.roll += r * 0.6f;
			part.yaw += y * 0.6f;
		} else {
			part.pitch = p;
			part.roll = r;
			part.yaw = y;
		}
	}

	/** Remember a part's angles so they are restored after this entity is drawn. */
	public static void save(ModelPart part) {
		if (part != null) SAVED.add(new Saved(part, part.pitch, part.yaw, part.roll));
	}

	private static void copy(ModelPart to, ModelPart from) {
		if (to == null || from == null) return;
		SAVED.add(new Saved(to, to.pitch, to.yaw, to.roll));
		to.pitch = from.pitch;
		to.yaw = from.yaw;
		to.roll = from.roll;
	}
}
