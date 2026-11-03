package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModEntities;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * SCP-1471-A: the figure that starts appearing around people who installed the "MalO ver1.0.0" app.
 * It only follows its owner, keeps its distance, and vanishes when stared at or attacked. It is never saved to disk.
 */
public class Scp1471Entity extends PathAwareEntity {
    public static final String INSTALLED_TAG = "scp1471_installed";

    @Nullable
    private UUID owner;
    private int life = 600;
    private int stared;

    public Scp1471Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (owner == null) {
            PlayerEntity nearest = getWorld().getClosestPlayer(this, 32.0);
            if (nearest != null) owner = nearest.getUuid();
        }
        PlayerEntity p = owner == null ? null : getWorld().getPlayerByUuid(owner);
        if (p == null || p.getWorld() != getWorld() || --life <= 0 || squaredDistanceTo(p) > 64.0 * 64.0) {
            vanish();
            return;
        }
        getLookControl().lookAt(p, 30.0f, 30.0f);
        double d = distanceTo(p);
        if (d > 10.0) getNavigation().startMovingTo(p, 1.0);
        else if (d < 5.0) getNavigation().stop();
        if (ScpUtil.isLookingAt(p, getEyePos(), 0.94)) {
            if (++stared > 30) vanish();
        } else if (stared > 0) {
            stared--;
        }
        if (this.age % 100 == 0) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, true, false, false));
        }
    }

    private void vanish() {
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 20, 0.3, 0.8, 0.3, 0.01);
            playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);
        }
        discard();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.damage(source, amount);
        if (!getWorld().isClient && source.getAttacker() instanceof PlayerEntity) vanish();
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public static boolean hasActiveFigure(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(64.0);
        return !player.getWorld().getEntitiesByClass(Scp1471Entity.class, box, e -> player.getUuid().equals(e.getOwner())).isEmpty();
    }

    /** Spawns a figure somewhere behind the player, on solid ground. */
    public static boolean spawnFor(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d back = player.getRotationVector().multiply(-1.0, 0.0, -1.0);
        if (back.lengthSquared() < 1.0E-4) back = new Vec3d(1.0, 0.0, 0.0);
        back = back.normalize();
        for (int attempt = 0; attempt < 12; attempt++) {
            double dist = 9.0 + player.getRandom().nextDouble() * 6.0;
            double spread = (player.getRandom().nextDouble() - 0.5) * 8.0;
            double x = player.getX() + back.x * dist - back.z * spread;
            double z = player.getZ() + back.z * dist + back.x * spread;
            BlockPos base = BlockPos.ofFloored(x, player.getY(), z);
            for (int dy = 4; dy >= -6; dy--) {
                BlockPos pos = base.up(dy);
                if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) && world.isAir(pos) && world.isAir(pos.up()) && world.isAir(pos.up(2))) {
                    Scp1471Entity figure = ModEntities.SCP_1471_A.create(world);
                    if (figure == null) return false;
                    figure.setOwner(player.getUuid());
                    figure.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), 0.0f);
                    world.spawnEntity(figure);
                    player.sendMessage(Text.translatable("message.scpfoundation.1471_image").formatted(Formatting.DARK_GRAY), false);
                    return true;
                }
            }
        }
        return false;
    }
}
