package net.scpfoundation.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * SCP-073 "Cain": a calm, polite man. Any harm done to him is reflected back onto the attacker,
 * and plant life withers wherever he walks.
 */
public class Scp073Entity extends PathAwareEntity {
    public Scp073Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.7));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (this.age % 30 != 0 || !getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) return;
        BlockPos pos = getBlockPos().add(this.random.nextInt(7) - 3, this.random.nextInt(3) - 1, this.random.nextInt(7) - 3);
        BlockState s = getWorld().getBlockState(pos);
        if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.MOSS_BLOCK)) {
            getWorld().setBlockState(pos, Blocks.COARSE_DIRT.getDefaultState());
        } else if (s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.CROPS) || s.isIn(BlockTags.SAPLINGS)
                || s.isOf(Blocks.GRASS) || s.isOf(Blocks.TALL_GRASS) || s.isOf(Blocks.FERN)) {
            getWorld().setBlockState(pos, Blocks.DEAD_BUSH.getDefaultState().canPlaceAt(getWorld(), pos)
                    ? Blocks.DEAD_BUSH.getDefaultState() : Blocks.AIR.getDefaultState());
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.damage(source, amount);
        if (!getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker && attacker != this) {
            attacker.damage(getDamageSources().thorns(this), amount);
            return false;
        }
        return super.damage(source, amount * 0.25f);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            player.sendMessage(Text.literal("SCP-073: ").formatted(Formatting.DARK_GRAY)
                    .append(Text.translatable("message.scpfoundation.073." + this.random.nextInt(3)).formatted(Formatting.GRAY, Formatting.ITALIC)), false);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
