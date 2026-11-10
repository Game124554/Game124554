package com.teardownvoxel.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.network.ModNetworking;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.registry.ModTags;
import com.teardownvoxel.world.DebrisSpawner;
import com.teardownvoxel.world.StructuralIntegrity;
import com.teardownvoxel.world.VoxelDamage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Heavy two-handed hammer. Right-click a block to smash a 3x3x1 slab of it (perpendicular to the face
 * you hit), throwing fragments away from you along your look direction. Blocks harder than the
 * configured limit take progressive damage instead.
 */
public class SledgehammerItem extends Item {
    private final Multimap<EntityAttribute, EntityAttributeModifier> attributes;

    public SledgehammerItem(Settings settings) {
        super(settings);
        this.attributes = ImmutableMultimap.<EntityAttribute, EntityAttributeModifier>builder()
                .put(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Sledgehammer damage", 9.0, EntityAttributeModifier.Operation.ADDITION))
                .put(EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Sledgehammer speed", -3.4, EntityAttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributes : super.getAttributeModifiers(slot);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        Vec3d push = attacker.getRotationVector().multiply(0.6);
        target.addVelocity(push.x, 0.25, push.z);
        return true;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;
        if (player.getItemCooldownManager().isCoolingDown(this)) return ActionResult.FAIL;
        if (world.isClient) return ActionResult.SUCCESS;

        ServerWorld sw = (ServerWorld) world;
        TVConfig cfg = TVConfig.get();
        BlockPos center = context.getBlockPos();
        Direction face = context.getSide();
        Vec3d look = player.getRotationVector();

        // Two axes spanning the plane perpendicular to the face that was hit.
        Direction.Axis axis = face.getAxis();
        Direction a = axis == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction b = axis == Direction.Axis.X ? Direction.SOUTH : (axis == Direction.Axis.Y ? Direction.SOUTH : Direction.EAST);

        List<BlockPos> smashed = new ArrayList<>();
        int chipped = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = center.offset(a, i).offset(b, j);
                BlockState state = world.getBlockState(pos);
                // Skips air, fluids and plants (no collision shape).
                if (state.isAir() || state.getCollisionShape(world, pos).isEmpty()) continue;
                if (state.isIn(ModTags.INDESTRUCTIBLE) || !world.canPlayerModifyAt(player, pos)) continue;
                float hardness = state.getHardness(world, pos);
                if (hardness < 0) continue;

                Vec3d impulse = look.multiply(0.45 + world.random.nextDouble() * 0.25).add(0, 0.12, 0);
                if (hardness <= cfg.sledgehammerMaxHardness) {
                    if (cfg.sledgehammerDropsItems && !player.isCreative()) Block.dropStacks(state, world, pos, world.getBlockEntity(pos), player, context.getStack());
                    world.breakBlock(pos, false, player);
                    DebrisSpawner.shatter(sw, pos, state, impulse, cfg.debrisPerBlockMin + world.random.nextInt(cfg.debrisPerBlockMax - cfg.debrisPerBlockMin + 1), 0.12);
                    VoxelDamage.clearAt(sw, pos);
                    smashed.add(pos.toImmutable());
                } else {
                    // Too hard to smash outright: crack it progressively.
                    if (VoxelDamage.damage(sw, pos, 6.0f, impulse)) {
                        smashed.add(pos.toImmutable());
                    } else {
                        chipped++;
                    }
                }
            }
        }

        Vec3d hit = context.getHitPos();
        world.playSound(null, hit.x, hit.y, hit.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5f, 0.55f + world.random.nextFloat() * 0.1f);
        if (!smashed.isEmpty()) {
            world.playSound(null, hit.x, hit.y, hit.z, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.8f, 0.8f);
        }
        sw.spawnParticles(ModParticles.DUST_CLOUD, hit.x, hit.y, hit.z, 3, 0.5, 0.5, 0.5, 0.02);
        sw.spawnParticles(ModParticles.PEBBLE, hit.x, hit.y, hit.z, 12, 0.6, 0.6, 0.6, 0.0);
        if (chipped > 0) sw.spawnParticles(ModParticles.SPARK, hit.x, hit.y, hit.z, 6, 0.1, 0.1, 0.1, 0.25);

        ModNetworking.sendShake(sw, hit, 1.2f, 8.0, 6);
        player.getItemCooldownManager().set(this, cfg.sledgehammerCooldownTicks);
        player.addExhaustion(0.4f);
        int wear = Math.max(1, smashed.size() + chipped / 2);
        context.getStack().damage(wear, player, p -> p.sendToolBreakStatus(context.getHand()));

        StructuralIntegrity.onBlocksDestroyed(sw, smashed);
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.teardown_voxel.sledgehammer.tooltip").formatted(Formatting.GRAY));
    }
}
