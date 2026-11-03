package net.scpfoundation.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModEntities;
import net.scpfoundation.registry.ModItems;

/** SCP-018: bounces off every surface, getting faster each time. Hurts whatever it hits; shatters glass when fast. */
public class Scp018Entity extends ThrownItemEntity {
    private static final double MAX_SPEED = 3.0;
    private int bounces;

    public Scp018Entity(EntityType<? extends Scp018Entity> type, World world) {
        super(type, world);
    }

    public Scp018Entity(World world, LivingEntity owner) {
        super(ModEntities.SCP_018, owner, world);
    }

    @Override
    public Item getDefaultItem() {
        return ModItems.SCP_018;
    }

    @Override
    public float getGravity() {
        return 0.02f;
    }

    @Override
    public void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        Direction side = hit.getSide();
        Vec3d v = getVelocity();
        v = switch (side.getAxis()) {
            case X -> new Vec3d(-v.x, v.y, v.z);
            case Y -> new Vec3d(v.x, -v.y, v.z);
            case Z -> new Vec3d(v.x, v.y, -v.z);
        };
        double speed = Math.min(Math.max(v.length(), 0.3) * 1.1, MAX_SPEED);
        v = v.lengthSquared() > 1.0E-6 ? v.normalize().multiply(speed) : new Vec3d(0.0, speed, 0.0);
        setVelocity(v);
        Vec3d pos = hit.getPos();
        setPosition(pos.x + side.getOffsetX() * 0.15, pos.y + side.getOffsetY() * 0.15, pos.z + side.getOffsetZ() * 0.15);
        bounces++;
        World world = getWorld();
        if (!world.isClient) {
            playSound(SoundEvents.ENTITY_SLIME_JUMP_SMALL, 0.8f, (float) (0.8 + speed * 0.4));
            BlockPos bp = hit.getBlockPos();
            BlockState state = world.getBlockState(bp);
            if (speed > 1.8 && world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)
                    && (state.isIn(BlockTags.IMPERMEABLE) || state.getHardness(world, bp) <= 0.6f) && state.getHardness(world, bp) >= 0.0f) {
                world.breakBlock(bp, true, getOwner());
            }
        }
    }

    @Override
    public void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        float damage = (float) Math.min(30.0, 2.0 + getVelocity().length() * 7.0);
        hit.getEntity().damage(getDamageSources().thrown(this, getOwner()), damage);
        setVelocity(getVelocity().multiply(-0.7));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && (this.age > 600 || (bounces > 3 && getVelocity().lengthSquared() < 0.01))) {
            dropStack(new ItemStack(ModItems.SCP_018));
            discard();
        }
    }
}
