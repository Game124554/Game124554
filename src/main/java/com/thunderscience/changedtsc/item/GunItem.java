package com.thunderscience.changedtsc.item;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.entity.BulletEntity;
import com.thunderscience.changedtsc.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Right-click to fire. Uses TSC bullets from your inventory (configurable). */
public class GunItem extends Item {
    public enum Kind { PISTOL, RIFLE, SHOTGUN }

    private final Kind kind;

    public GunItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /**
     * Client only: hold the gun up and aim it in third person (pistol = straight arms, long guns =
     * shouldered), instead of letting it hang at the side. The 3D gun models are built for this pose.
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return kind == Kind.PISTOL ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }

    private float damage() {
        return switch (kind) {
            case PISTOL -> TSCConfig.PISTOL_DAMAGE.get().floatValue();
            case RIFLE -> TSCConfig.RIFLE_DAMAGE.get().floatValue();
            case SHOTGUN -> TSCConfig.SHOTGUN_PELLET_DAMAGE.get().floatValue();
        };
    }

    private int cooldown() {
        return switch (kind) {
            case PISTOL -> TSCConfig.PISTOL_COOLDOWN.get();
            case RIFLE -> TSCConfig.RIFLE_COOLDOWN.get();
            case SHOTGUN -> TSCConfig.SHOTGUN_COOLDOWN.get();
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        boolean free = player.getAbilities().instabuild || !TSCConfig.GUNS_REQUIRE_AMMO.get();
        if (!free && !consumeAmmo(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.changed_tsc.no_ammo"), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            int pellets = kind == Kind.SHOTGUN ? 6 : 1;
            float spread = switch (kind) {
                case PISTOL -> 1.5F;
                case RIFLE -> 2.5F;
                case SHOTGUN -> 9.0F;
            };
            for (int i = 0; i < pellets; i++) {
                BulletEntity bullet = new BulletEntity(level, player);
                bullet.setDamage(damage());
                bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.6F, spread);
                level.addFreshEntity(bullet);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.PLAYERS, 1.0F, kind == Kind.SHOTGUN ? 0.9F : 1.7F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, cooldown());
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static boolean consumeAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.BULLET.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.changed_tsc.gun_damage", String.format("%.1f", damage()),
                kind == Kind.SHOTGUN ? " x6" : "").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.changed_tsc.gun_ammo").withStyle(ChatFormatting.DARK_GRAY));
    }
}
