package com.thunderscience.changedtsc.item;

import com.thunderscience.changedtsc.boss.BossManager;
import com.thunderscience.changedtsc.boss.BossType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Use on a block to summon a boss there. */
public class BossSummonItem extends Item {
    private final BossType type;

    public BossSummonItem(BossType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Mob boss = BossManager.spawnBoss(server, pos, type);
        Player player = context.getPlayer();
        if (boss == null) {
            if (player != null) player.displayClientMessage(Component.translatable("message.changed_tsc.boss_failed"), true);
            return InteractionResult.FAIL;
        }
        if (player != null && !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.changed_tsc.summon", type.displayName()).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
