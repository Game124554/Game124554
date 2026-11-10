package net.latexdrones.item;

import net.latexdrones.entity.DartEntity;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.registry.ModItems;
import net.latexdrones.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The three dart-firing weapons. Right-click fires one round of ammo from the inventory
 * (the off-hand is checked first, so you can choose between latex and antidote darts).
 */
public class GunItem extends Item {
    public enum Kind {
        SYRINGE_GUN, TRANQ_RIFLE, GOO_LAUNCHER
    }

    private final Kind kind;

    public GunItem(Kind kind, Item.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    private Item[] ammoTypes() {
        switch (this.kind) {
            case TRANQ_RIFLE:
                return new Item[] {ModItems.TRANQ_DART};
            case GOO_LAUNCHER:
                return new Item[] {ModItems.GOO_CANISTER};
            default:
                return new Item[] {ModItems.LATEX_DART, ModItems.ANTIDOTE_DART};
        }
    }

    private static ItemStack findAmmo(Player player, Item[] types) {
        ItemStack off = player.getOffhandItem();
        for (Item type : types) {
            if (off.is(type)) {
                return off;
            }
        }
        Inventory inventory = player.getInventory();
        for (Item type : types) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(type)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        Item[] types = this.ammoTypes();
        ItemStack ammo = findAmmo(player, types);
        boolean creative = player.getAbilities().instabuild;
        if (ammo.isEmpty() && !creative) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.latexdrones.no_ammo"), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            return InteractionResultHolder.fail(gun);
        }
        Item ammoItem = ammo.isEmpty() ? types[0] : ammo.getItem();
        float velocity = this.kind == Kind.TRANQ_RIFLE ? 3.0F : (this.kind == Kind.GOO_LAUNCHER ? 1.3F : 2.2F);
        float spread = this.kind == Kind.TRANQ_RIFLE ? 0.2F : 1.5F;
        int cooldown = this.kind == Kind.TRANQ_RIFLE ? 30 : (this.kind == Kind.GOO_LAUNCHER ? 25 : 12);
        if (!level.isClientSide) {
            DartEntity dart = new DartEntity(level, player);
            dart.setItem(new ItemStack(ammoItem));
            // a transformed player's darts pass on their own form
            dart.setSource(InfectionData.isTransformed(player) ? InfectionData.server(player.getUUID()).form : "drone_unit", 0);
            dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, spread);
            level.addFreshEntity(dart);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    this.kind == Kind.GOO_LAUNCHER ? ModSounds.GOO_SPLAT : ModSounds.DART_SHOOT, SoundSource.PLAYERS, 1.0F,
                    this.kind == Kind.TRANQ_RIFLE ? 0.8F : 1.1F);
            gun.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            if (!creative) {
                ammo.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, cooldown);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(gun, level.isClientSide());
    }
}
