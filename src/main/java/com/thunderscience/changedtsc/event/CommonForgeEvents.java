package com.thunderscience.changedtsc.event;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.boss.BossManager;
import com.thunderscience.changedtsc.compat.FirstAidCompat;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.entity.TSCScientistEntity;
import com.thunderscience.changedtsc.entity.AbstractTSCHuman;
import com.thunderscience.changedtsc.entity.goal.FollowNearestPlayerGoal;
import com.thunderscience.changedtsc.infection.GooInfection;
import com.thunderscience.changedtsc.item.PurifierBladeItem;
import com.thunderscience.changedtsc.util.EntityResolver;
import com.thunderscience.changedtsc.util.LatexHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChangedTSC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonForgeEvents {
    private CommonForgeEvents() {}

    // ================================================================ joining the world

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();

        if (shouldRemoveVanilla(entity)) {
            if (event.loadedFromDisk()) {
                MinecraftServer server = entity.getServer();
                if (server != null) server.tell(new TickTask(server.getTickCount(), entity::discard));
            } else {
                event.setCanceled(true);
            }
            return;
        }

        if (entity instanceof PathfinderMob mob && !(entity instanceof AbstractTSCHuman)) {
            if (LatexHelper.isFriendlyLatex(mob)) {
                setupFriendly(mob);
            } else if (TSCConfig.LATEX_HUNT_HUMANS.get() && LatexHelper.isHostileLatex(mob)) {
                mob.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(mob, AbstractTSCHuman.class, true));
            }
        }
    }

    private static void setupFriendly(PathfinderMob mob) {
        mob.setPersistenceRequired();
        if (TSCConfig.FRIENDLY_FIGHT_HOSTILES.get()) {
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false,
                    LatexHelper::isFriendEnemy));
        }
        if (TSCConfig.FRIENDLY_FOLLOW_PLAYERS.get()) {
            mob.goalSelector.addGoal(6, new FollowNearestPlayerGoal(mob, 1.1D, 7.0F, 3.0F));
        }
    }

    private static boolean shouldRemoveVanilla(Entity e) {
        if (!TSCConfig.REMOVE_VANILLA_HOSTILES.get() || !(e instanceof Mob)) return false;
        ResourceLocation id = EntityType.getKey(e.getType());
        if (!"minecraft".equals(id.getNamespace())) return false;
        if (e.hasCustomName() || BossManager.isBoss(e) || e.getTags().contains(BossManager.MINION_TAG)) return false;
        if (TSCConfig.VANILLA_WHITELIST.get().contains(id.toString())) return false;

        boolean vanillaBoss = e instanceof EnderDragon || e instanceof WitherBoss
                || e.getType() == EntityType.ELDER_GUARDIAN || e.getType() == EntityType.WARDEN;
        if (vanillaBoss) return TSCConfig.REMOVE_VANILLA_BOSSES.get();
        return e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER;
    }

    // ================================================================ targeting: friendly Puro + stealth

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null || attacker.level().isClientSide()) return;

        boolean provoked = attacker.getLastHurtByMob() == target;

        // Friendly Puro never go after players, TSC humans or other friendlies
        if (LatexHelper.isFriendlyLatex(attacker)
                && (target instanceof Player || target instanceof AbstractTSCHuman || LatexHelper.isFriendlyLatex(target))) {
            if (!(provoked && TSCConfig.FRIENDLY_RETALIATE.get())) {
                event.setCanceled(true);
                return;
            }
        }

        // TSC humans never attack friendly Puro unless the Puro hit them first
        if (attacker instanceof AbstractTSCHuman && LatexHelper.isFriendlyLatex(target) && !provoked) {
            event.setCanceled(true);
            return;
        }

        // Stealth: sneaking players are not noticed by latex unless close
        if (target instanceof Player player && isHiddenFrom(attacker, player, 1.0)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!TSCConfig.STEALTH_ENABLED.get()) return;
        if (event.getEntity() instanceof Player player && player.isCrouching()
                && event.getLookingEntity() != null && LatexHelper.isLatex(event.getLookingEntity())) {
            if (BossManager.isBoss(event.getLookingEntity()) && !TSCConfig.STEALTH_VS_BOSSES.get()) return;
            event.modifyVisibility(TSCConfig.SNEAK_VISIBILITY.get());
        }
    }

    /** True if this latex should not notice this sneaking player. */
    private static boolean isHiddenFrom(LivingEntity looker, Player player, double rangeScale) {
        if (!TSCConfig.STEALTH_ENABLED.get() || !player.isCrouching() || !LatexHelper.isLatex(looker)) return false;
        if (BossManager.isBoss(looker) && !TSCConfig.STEALTH_VS_BOSSES.get()) return false;
        if (looker.getLastHurtByMob() == player) return false;
        double range = TSCConfig.SNEAK_NOTICE_RANGE.get() * rangeScale;
        if (player.level().getMaxLocalRawBrightness(player.blockPosition()) < 7) {
            range *= TSCConfig.DARKNESS_RANGE_MULTIPLIER.get();
        }
        return looker.distanceToSqr(player) > range * range;
    }

    // ================================================================ ticking

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity.tickCount % 20 == 0 && GooInfection.isHost(entity)) GooInfection.tickSecond(entity);
        if (!(entity instanceof Mob mob)) return;

        if (BossManager.isBoss(mob)) BossManager.tickBoss(mob);
        else if (mob.tickCount % 20 == 0 && mob.getTags().contains(BossManager.MINION_TAG)) BossManager.tickMinion(mob);

        // latex chasing a sneaking player lose track of them once they get far enough away
        if (mob.tickCount % 10 == 0 && mob.getTarget() instanceof Player player && isHiddenFrom(mob, player, 1.75)) {
            mob.setTarget(null);
        }
    }

    // ================================================================ damage

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source == null) return;

        // boss fight rules: shielded, protected by the pack, worn out...
        if (BossManager.isBoss(victim)) {
            event.setAmount(event.getAmount() * BossManager.damageTakenMultiplier(victim));
        }

        if (source instanceof LivingEntity attacker && event.getSource().getDirectEntity() == attacker
                && attacker.getMainHandItem().getItem() instanceof PurifierBladeItem && LatexHelper.isLatex(victim)) {
            event.setAmount(event.getAmount() * TSCConfig.PURIFIER_LATEX_MULTIPLIER.get().floatValue());
        }

        if (LatexHelper.isLatex(source) && LatexHelper.hasFullHazmat(victim)) {
            event.setAmount(event.getAmount() * (1.0F - TSCConfig.HAZMAT_LATEX_REDUCTION.get().floatValue()));
        }
    }

    /** Hostile latex that actually deal damage put goo inside the victim's body. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (victim.level().isClientSide() || source == null || event.getAmount() <= 0.0F) return;
        if (!LatexHelper.isHostileLatex(source) || !GooInfection.canBeInfected(victim)) return;
        float amount = TSCConfig.GOO_PER_HIT.get().floatValue();
        if (BossManager.isBoss(source)) amount *= TSCConfig.GOO_BOSS_MULTIPLIER.get().floatValue();
        if (LatexHelper.hasFullHazmat(victim)) amount *= 1.0F - TSCConfig.HAZMAT_LATEX_REDUCTION.get().floatValue();
        GooInfection.infect(victim, source, amount);
    }

    // ---- keep every client's goo overlay in sync
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity target
                && GooInfection.isHost(target)) {
            GooInfection.syncTo(viewer, target);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) GooInfection.syncSelf(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) GooInfection.syncSelf(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) GooInfection.syncSelf(p);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && BossManager.isBoss(event.getEntity())) {
            BossManager.onBossDeath(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) BossManager.removeBar(event.getEntity());
    }

    // ================================================================ First Aid mod: loot and drops

    /** Adds the First Aid mod's bandages / plasters / morphine to every TSC structure chest. */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (name == null || !ChangedTSC.MODID.equals(name.getNamespace()) || !name.getPath().startsWith("chests/")) return;
        try {
            FirstAidCompat.addToChestLoot(event.getTable());
        } catch (Exception e) {
            ChangedTSC.LOGGER.warn("Could not add First Aid mod items to {}", name, e);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) return;
        int rolls = BossManager.isBoss(dead) ? 4 : dead instanceof TSCScientistEntity ? 1 : 0;
        for (int i = 0; i < rolls; i++) {
            ItemStack stack = FirstAidCompat.randomDrop(dead.getRandom());
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), stack));
            }
        }
    }

    // ================================================================ server lifecycle

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TSCCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BossManager.clearAll();
        EntityResolver.invalidate();
    }
}
