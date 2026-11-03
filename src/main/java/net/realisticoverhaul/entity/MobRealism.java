package net.realisticoverhaul.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.realisticoverhaul.config.ROConfig;

import java.util.UUID;

/** Small "realistic mob" touches: hits splatter, badly hurt mobs limp (move slower). */
public final class MobRealism {
	private static final UUID WOUNDED_ID = UUID.fromString("6b1f3c2e-8f8a-4c1e-9d4a-2a7e5c9b1f01");
	private static final EntityAttributeModifier WOUNDED = new EntityAttributeModifier(
			WOUNDED_ID, "realisticoverhaul_wounded", -0.35, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

	private MobRealism() {}

	/** Fabric ALLOW_DAMAGE: never blocks damage, only adds the splatter. */
	public static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
		ROConfig cfg = ROConfig.get();
		if (!cfg.blood || amount <= 0f || !(entity.getWorld() instanceof ServerWorld world)) return true;
		if (entity.timeUntilRegen > 10 || entity.isInvulnerableTo(source)) return true;
		boolean physical = source.getAttacker() != null || source.isIn(DamageTypeTags.IS_PROJECTILE)
				|| source.isIn(DamageTypeTags.IS_FALL) || source.isIn(DamageTypeTags.IS_EXPLOSION);
		if (!physical) return true;

		BlockState splatter = splatterFor(entity);
		if (splatter == null) return true;
		int count = MathHelper.clamp(Math.round(amount * 3f), 4, 30) * Math.max(1, cfg.particleDensity) / 6;
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, splatter),
				entity.getX(), entity.getBodyY(0.6), entity.getZ(), Math.max(2, count),
				entity.getWidth() * 0.3, entity.getHeight() * 0.25, entity.getWidth() * 0.3, 0.15);
		return true;
	}

	private static BlockState splatterFor(LivingEntity e) {
		EntityType<?> t = e.getType();
		if (t.isIn(EntityTypeTags.SKELETONS) || t == EntityType.SKELETON_HORSE) return Blocks.BONE_BLOCK.getDefaultState();
		if (e instanceof MagmaCubeEntity) return Blocks.MAGMA_BLOCK.getDefaultState();
		if (e instanceof SlimeEntity) return Blocks.SLIME_BLOCK.getDefaultState();
		if (t == EntityType.IRON_GOLEM) return Blocks.IRON_BLOCK.getDefaultState();
		if (t == EntityType.SNOW_GOLEM) return Blocks.SNOW_BLOCK.getDefaultState();
		if (t == EntityType.BLAZE || t == EntityType.VEX || t == EntityType.ALLAY || t == EntityType.ARMOR_STAND) return null;
		if (t == EntityType.CREEPER) return Blocks.LIME_TERRACOTTA.getDefaultState();
		if (t == EntityType.ENDERMAN || t == EntityType.ENDERMITE || t == EntityType.SHULKER) return Blocks.PURPLE_TERRACOTTA.getDefaultState();
		return Blocks.REDSTONE_BLOCK.getDefaultState();
	}

	/** Every 10 ticks: mobs under 30% health move 35% slower. */
	public static void tickWounded(LivingEntity entity) {
		if (!(entity instanceof MobEntity) || entity.isDead()) return;
		EntityAttributeInstance speed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (speed == null) return;
		boolean wounded = ROConfig.get().woundedMobs && entity.getHealth() < entity.getMaxHealth() * 0.3f;
		boolean has = speed.getModifier(WOUNDED_ID) != null;
		if (wounded && !has) speed.addTemporaryModifier(WOUNDED);
		else if (!wounded && has) speed.removeModifier(WOUNDED_ID);
	}
}
