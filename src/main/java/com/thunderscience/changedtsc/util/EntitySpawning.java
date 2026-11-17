package com.thunderscience.changedtsc.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import javax.annotation.Nullable;

public final class EntitySpawning {
    private EntitySpawning() {}

    /**
     * Creates, tags, finalizes and adds an entity. Tags are added BEFORE the entity joins the
     * world so that the "remove vanilla hostiles" option can recognise boss minions.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static Entity spawn(ServerLevel level, EntityType<?> type, BlockPos pos, MobSpawnType reason,
                               boolean persistent, String... tags) {
        Entity entity = type.create(level);
        if (entity == null) return null;
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        for (String tag : tags) entity.addTag(tag);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null, null);
            if (persistent) mob.setPersistenceRequired();
        }
        if (!level.addFreshEntity(entity)) return null;
        return entity;
    }
}
