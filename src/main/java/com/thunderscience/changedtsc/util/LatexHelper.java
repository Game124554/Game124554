package com.thunderscience.changedtsc.util;

import com.thunderscience.changedtsc.boss.BossManager;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.entity.AbstractTSCHuman;
import com.thunderscience.changedtsc.item.TSCArmorMaterials;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Small helpers that decide who is latex, who is friendly and who the humans should fight. */
public final class LatexHelper {
    /** Scoreboard tag that makes one single mob friendly (used for Puro). Works with /tag too. */
    public static final String FRIENDLY_TAG = "changed_tsc_friendly";

    private LatexHelper() {}

    /** Give a freshly spawned friendly the name Puro (unless its entity type already is a Puro). */
    public static void namePuro(Entity e) {
        if (!EntityType.getKey(e.getType()).getPath().contains("puro") && !e.hasCustomName()) {
            e.setCustomName(net.minecraft.network.chat.Component.literal("Puro"));
        }
    }

    /** Any living mob from a latex namespace (Changed and its add-ons). */
    public static boolean isLatex(Entity e) {
        if (!(e instanceof LivingEntity) || e instanceof Player) return false;
        return EntityResolver.latexNamespaces().contains(EntityType.getKey(e.getType()).getNamespace());
    }

    public static boolean isLatexType(EntityType<?> type) {
        return EntityResolver.latexNamespaces().contains(EntityType.getKey(type).getNamespace())
                && !EntityResolver.friendlyTypes().contains(type);
    }

    /** Friendly Puro (or anything else listed as friendly). Bosses are never friendly. */
    public static boolean isFriendlyLatex(Entity e) {
        if (e == null || e instanceof Player || BossManager.isBoss(e)) return false;
        if (!TSCConfig.FRIENDLY_PURO_ENABLED.get()) return false;
        return e.getTags().contains(FRIENDLY_TAG) || EntityResolver.friendlyTypes().contains(e.getType());
    }

    public static boolean isHostileLatex(Entity e) {
        if (!isLatex(e) || isFriendlyLatex(e)) return false;
        return e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER || BossManager.isBoss(e);
    }

    /** Targets for TSC guards / staff. */
    public static boolean isHumanEnemy(LivingEntity e) {
        if (e == null || !e.isAlive() || e instanceof Player || e instanceof AbstractTSCHuman) return false;
        if (isFriendlyLatex(e)) return false;
        if (isHostileLatex(e) || BossManager.isBoss(e)) return true;
        return TSCConfig.NPCS_FIGHT_VANILLA_MONSTERS.get() && e instanceof Enemy && !(e instanceof Creeper) && !isLatex(e);
    }

    /** Targets for friendly Puro. */
    public static boolean isFriendEnemy(LivingEntity e) {
        if (e == null || !e.isAlive() || e instanceof Player || e instanceof AbstractTSCHuman || isFriendlyLatex(e)) return false;
        return isHostileLatex(e) || BossManager.isBoss(e) || (e instanceof Enemy && !(e instanceof Creeper));
    }

    public static boolean hasFullHazmat(LivingEntity e) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = e.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getMaterial() != TSCArmorMaterials.HAZMAT) return false;
        }
        return true;
    }
}
