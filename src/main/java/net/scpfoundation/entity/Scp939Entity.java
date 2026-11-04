package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * SCP-939. Blind; it hunts by sound, so it tracks anyone who is not sneaking (no line of sight needed).
 * It lures prey by imitating the voices of players nearby.
 */
public class Scp939Entity extends HostileEntity {
    private int mimicCooldown = 200;

    public Scp939Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 30;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.7));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 5, false, false,
                living -> !living.isSneaking() || living.squaredDistanceTo(this) < 9.0));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (getTarget() instanceof PlayerEntity p && p.isSneaking() && p.squaredDistanceTo(this) > 9.0 && this.random.nextInt(40) == 0) {
            setTarget(null); // it lost the sound
        }
        if (--mimicCooldown <= 0) {
            mimicCooldown = 400 + this.random.nextInt(600);
            mimic();
        }
    }

    private void mimic() {
        List<PlayerEntity> near = getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(28.0), p -> !p.isSpectator());
        if (near.isEmpty()) return;
        List<? extends PlayerEntity> all = getWorld().getPlayers();
        PlayerEntity voice = all.get(this.random.nextInt(all.size()));
        Text line = Text.literal("<" + voice.getName().getString() + "> ")
                .append(Text.translatable("message.scpfoundation.939." + this.random.nextInt(4)));
        for (PlayerEntity listener : near) {
            if (listener != voice && !net.scpfoundation.item.Scp148Item.isShielded(listener)) listener.sendMessage(line.copy().formatted(Formatting.WHITE), false);
        }
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WOLF_GROWL;
    }

    @Override
    public float getSoundPitch() {
        return 0.5f;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WOLF_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WOLF_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
