package net.realisticoverhaul.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.ItemStack;
import net.realisticoverhaul.mixin.client.LimbAnimatorAccessor;
import net.realisticoverhaul.config.ROConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Draws cursed players as Piglins by copying their pose onto a fake Piglin. */
public final class ClientPiglin {
	private static final Set<UUID> PIGLINS = new HashSet<>();
	private static final Map<UUID, PiglinEntity> DUMMIES = new HashMap<>();

	private ClientPiglin() {}

	public static void set(Set<UUID> uuids) {
		PIGLINS.clear();
		PIGLINS.addAll(uuids);
		DUMMIES.keySet().retainAll(uuids);
	}

	public static void clear() {
		PIGLINS.clear();
		DUMMIES.clear();
	}

	/** @return true if the player was drawn as a Piglin (so the normal player model is skipped). */
	public static boolean render(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
								 VertexConsumerProvider vertexConsumers, int light) {
		if (!ROConfig.get().piglinForm || !PIGLINS.contains(player.getUuid())) return false;

		PiglinEntity d = DUMMIES.get(player.getUuid());
		if (d == null || d.getWorld() != player.getWorld()) {
			d = EntityType.PIGLIN.create(player.getWorld());
			if (d == null) return false;
			d.setImmuneToZombification(true);
			DUMMIES.put(player.getUuid(), d);
		}
		copyPose(player, d);

		EntityRenderer<? super PiglinEntity> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(d);
		renderer.render(d, yaw, tickDelta, matrices, vertexConsumers, light);
		return true;
	}

	private static void copyPose(AbstractClientPlayerEntity p, PiglinEntity d) {
		d.setPos(p.getX(), p.getY(), p.getZ());
		d.prevX = p.prevX;
		d.prevY = p.prevY;
		d.prevZ = p.prevZ;
		d.setYaw(p.getYaw());
		d.prevYaw = p.prevYaw;
		d.setPitch(p.getPitch());
		d.prevPitch = p.prevPitch;
		d.bodyYaw = p.bodyYaw;
		d.prevBodyYaw = p.prevBodyYaw;
		d.headYaw = p.headYaw;
		d.prevHeadYaw = p.prevHeadYaw;
		d.handSwingProgress = p.handSwingProgress;
		d.lastHandSwingProgress = p.lastHandSwingProgress;
		d.handSwinging = p.handSwinging;
		d.preferredHand = p.preferredHand;
		d.hurtTime = p.hurtTime;
		d.maxHurtTime = p.maxHurtTime;
		d.deathTime = p.deathTime;
		d.age = p.age;
		d.setOnGround(p.isOnGround());
		d.setSneaking(p.isSneaking());
		d.setPose(p.getPose());
		d.setInvisible(p.isInvisible());

		LimbAnimatorAccessor src = (LimbAnimatorAccessor) p.limbAnimator;
		LimbAnimatorAccessor dst = (LimbAnimatorAccessor) d.limbAnimator;
		dst.realisticoverhaul$setPrevSpeed(src.realisticoverhaul$getPrevSpeed());
		dst.realisticoverhaul$setSpeed(src.realisticoverhaul$getSpeed());
		dst.realisticoverhaul$setPos(src.realisticoverhaul$getPos());

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack want = p.getEquippedStack(slot);
			if (d.getEquippedStack(slot) != want) d.equipStack(slot, want);
		}
	}
}
