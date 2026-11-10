package net.latexdrones.item;

import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.DroneTasks;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Syringes. Right-click to inject yourself, right-click a creature to inject it.
 * <ul>
 *   <li>LATEX - +35% infection (creative players are forced straight into the transformation); converts villagers &amp; co.</li>
 *   <li>ANTIDOTE - -50% infection and one minute of inoculation; useless once transformed.</li>
 *   <li>CURE - the only way back for a transformed player; also wipes any infection.</li>
 * </ul>
 */
public class SyringeItem extends Item {
    public enum Kind {
        LATEX, ANTIDOTE, CURE
    }

    private final Kind kind;

    public SyringeItem(Kind kind, Item.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            String form = InfectionData.isTransformed(player) ? InfectionData.server(player.getUUID()).form : "drone_unit";
            if (!this.inject(serverPlayer, serverPlayer, form)) {
                return InteractionResultHolder.fail(stack);
            }
            this.finish(level, player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        String form = InfectionData.isTransformed(player) ? InfectionData.server(player.getUUID()).form : "drone_unit";
        boolean used = false;
        if (target instanceof ServerPlayer other) {
            used = this.inject(player, other, form);
        } else if (this.kind == Kind.LATEX && !(target instanceof LatexDroneEntity) && InfectionManager.isConvertible(target)
                && level instanceof ServerLevel server) {
            InfectionManager.spawnDroneFrom(server, target, form);
            target.discard();
            if (player instanceof ServerPlayer converter) {
                DroneTasks.onConvert(converter);
            }
            used = true;
        } else if (this.kind != Kind.LATEX && target instanceof LatexDroneEntity drone && !drone.def().scientist) {
            drone.hurt(level.damageSources().magic(), this.kind == Kind.CURE ? 30.0F : 12.0F);
            used = true;
        }
        if (!used) {
            return InteractionResult.PASS;
        }
        this.finish(level, player, stack);
        return InteractionResult.SUCCESS;
    }

    /** @return true if the syringe did something and should be used up */
    private boolean inject(Player user, ServerPlayer patient, String form) {
        switch (this.kind) {
            case LATEX:
                if (InfectionData.isTransformed(patient) || InfectionData.isTransforming(patient)) {
                    return false;
                }
                if (patient.isCreative()) {
                    InfectionManager.forceTransform(patient, form);
                } else {
                    InfectionManager.addInfection(patient, 350, form);
                }
                return true;
            case ANTIDOTE:
                if (InfectionData.isTransformed(patient)) {
                    user.displayClientMessage(Component.translatable("message.latexdrones.antidote_late"), true);
                    return false;
                }
                boolean had = InfectionManager.reduce(patient, 500);
                patient.addEffect(new MobEffectInstance(ModEffects.INOCULATED, 1200, 0));
                patient.displayClientMessage(Component.translatable(had ? "message.latexdrones.antidote" : "message.latexdrones.not_infected"), true);
                return true;
            case CURE:
                if (!InfectionManager.cure(patient)) {
                    user.displayClientMessage(Component.translatable("message.latexdrones.not_infected"), true);
                    return false;
                }
                patient.addEffect(new MobEffectInstance(ModEffects.INOCULATED, 2400, 0));
                return true;
            default:
                return false;
        }
    }

    private void finish(Level level, Player player, ItemStack stack) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.INJECT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
