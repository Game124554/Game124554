package net.realisticoverhaul.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Dead mobs stay as corpses (lying on their side) for a configurable time.
 * On death they get thrown away from the hit (simple ragdoll knockback),
 * and you can push or kick corpses around.
 */
public final class Corpses {
	private static final ArrayDeque<LivingEntity> CORPSES = new ArrayDeque<>();

	private Corpses() {}

	public static boolean appliesTo(LivingEntity e) {
		return !(e instanceof PlayerEntity) && !(e instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity);
	}

	public static void onDeath(LivingEntity entity, DamageSource source) {
		ROConfig cfg = ROConfig.get();
		if (!cfg.corpses || !appliesTo(entity)) return;
		CORPSES.add(entity);

		Vec3d from = null;
		Entity attacker = source.getAttacker();
		if (attacker != null) from = attacker.getPos();
		else if (source.getPosition() != null) from = source.getPosition();
		if (from != null) {
			Vec3d dir = entity.getPos().subtract(from);
			Vec3d flat = new Vec3d(dir.x, 0, dir.z);
			if (flat.lengthSquared() > 1.0E-4) {
				flat = flat.normalize().multiply(cfg.ragdollImpulse);
				entity.addVelocity(flat.x, cfg.ragdollImpulse * 0.5, flat.z);
				entity.velocityModified = true;
			}
		}
	}

	/** Left-click a corpse to kick it. */
	public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity target, EntityHitResult hit) {
		if (!ROConfig.get().corpses) return ActionResult.PASS;
		if (!(target instanceof LivingEntity le) || !le.isDead() || !appliesTo(le)) return ActionResult.PASS;
		if (world.isClient) return ActionResult.PASS; // let the packet reach the server
		Vec3d look = player.getRotationVector();
		le.addVelocity(look.x * 0.7, 0.3, look.z * 0.7);
		le.velocityModified = true;
		world.playSound(null, le.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.7f, 0.8f);
		return ActionResult.SUCCESS;
	}

	/** Remove the oldest corpses when there are too many. */
	public static void tick() {
		Iterator<LivingEntity> it = CORPSES.iterator();
		while (it.hasNext()) {
			if (it.next().isRemoved()) it.remove();
		}
		int max = Math.max(1, ROConfig.get().maxCorpses);
		while (CORPSES.size() > max) {
			LivingEntity old = CORPSES.poll();
			if (old != null && !old.isRemoved()) {
				old.getWorld().sendEntityStatus(old, (byte) 60);
				old.remove(Entity.RemovalReason.KILLED);
			}
		}
	}

	public static void clear() {
		CORPSES.clear();
	}
}
