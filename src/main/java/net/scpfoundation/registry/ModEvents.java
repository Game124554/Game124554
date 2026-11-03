package net.scpfoundation.registry;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.scpfoundation.entity.Scp0492Entity;
import net.scpfoundation.entity.Scp049Entity;

public final class ModEvents {
    private ModEvents() {}

    public static void register() {
        // SCP-049's "cure": anything it kills rises again as SCP-049-2.
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (killer instanceof Scp049Entity && !(killed instanceof Scp0492Entity) && isCurable(killed)) {
                spawnInstance(world, killed);
            }
        });
    }

    private static boolean isCurable(LivingEntity killed) {
        return killed instanceof net.minecraft.entity.player.PlayerEntity
                || killed instanceof net.minecraft.entity.passive.MerchantEntity
                || killed instanceof net.minecraft.entity.mob.ZombieEntity
                || killed instanceof net.minecraft.entity.mob.AbstractPiglinEntity
                || killed instanceof net.minecraft.entity.mob.IllagerEntity
                || killed instanceof net.minecraft.entity.mob.WitchEntity;
    }

    private static void spawnInstance(ServerWorld world, LivingEntity killed) {
        Scp0492Entity zombie = ModEntities.SCP_049_2.create(world);
        if (zombie == null) return;
        zombie.refreshPositionAndAngles(killed.getX(), killed.getY(), killed.getZ(), killed.getYaw(), 0.0f);
        if (killed.hasCustomName()) zombie.setCustomName(killed.getCustomName());
        if (killed instanceof MobEntity mob && mob.isPersistent()) zombie.setPersistent();
        world.spawnEntity(zombie);
        world.spawnParticles(ParticleTypes.SOUL, killed.getX(), killed.getBodyY(0.5), killed.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        world.playSound(null, killed.getX(), killed.getY(), killed.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1.0f, 0.6f);
    }
}
