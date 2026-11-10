package net.latexdrones.drone;

import java.util.List;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.latexdrones.block.GooBlock;
import net.latexdrones.block.LatexPlantBlock;
import net.latexdrones.block.PuddleBlock;
import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.latexdrones.infection.InfectionSavedData;
import net.latexdrones.infection.ModNetworking;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModItems;
import net.latexdrones.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Life as a drone (only for transformed players):
 * <ul>
 *   <li><b>Orders</b> - scientists near you (or a Drone Terminal) hand out tasks with a time limit.</li>
 *   <li><b>Tasks</b> - clean spills, bring latex / iron, patrol, guard, hunt intruders, convert villagers.</li>
 *   <li><b>Obedience</b> - finishing tasks raises it; failing, refusing or attacking a scientist is
 *       disobedience: you are "corrected" (shock + hypnosis) and scientists hunt you for a while.
 *       Below 25% you are a rogue unit and every drone attacks you; from 75% drones defend you.</li>
 *   <li><b>Energy / standby</b> - your battery drains; sleep in a Drone Sleep Pod or on a Charging Pad to recharge.</li>
 * </ul>
 */
public final class DroneLife {
    private DroneLife() {
    }

    public static final int ACTION_REQUEST = 0;
    public static final int ACTION_REPORT = 1;
    public static final int ACTION_REFUSE = 2;
    public static final int ACTION_STANDBY = 3;

    /** Ranks earned by finished orders: Drone, Loyal Drone (3), Elite Drone (6), Commander Unit (10). */
    public static final String[] RANKS = {"drone", "loyal", "elite", "commander"};
    /** Ticks of standby every sleeping drone needs before the night can be skipped. */
    private static final int STANDBY_SKIP_NIGHT = 100;

    public static int rank(int tasksDone) {
        return tasksDone >= 10 ? 3 : (tasksDone >= 6 ? 2 : (tasksDone >= 3 ? 1 : 0));
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.TERMINAL_ACTION, (server, player, handler, buf, responseSender) -> {
            int action = buf.readVarInt();
            server.execute(() -> handleAction(player, action));
        });
        // cleaning: breaking spills counts
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer sp && isSpill(state.getBlock())) {
                credit(sp, DroneTask.CLEAN, 1);
            }
        });
        // taking damage wakes you up; loyal units are defended by nearby drones
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && InfectionData.isTransformed(player)) {
                InfectionData.Entry e = InfectionData.server(player.getUUID());
                if (e.standby) {
                    wake(player, e, false);
                }
                Entity attacker = source.getEntity();
                if (e.obedience >= InfectionData.LOYAL && attacker instanceof LivingEntity living
                        && !(attacker instanceof LatexDroneEntity) && attacker != player) {
                    for (LatexDroneEntity drone : player.serverLevel().getEntitiesOfClass(LatexDroneEntity.class,
                            player.getBoundingBox().inflate(16.0D), d -> !d.def().scientist)) {
                        drone.setTarget(living);
                    }
                }
            }
            return true;
        });
        // hitting a scientist as a drone is disobedience
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide && player instanceof ServerPlayer sp && entity instanceof LatexDroneEntity drone
                    && drone.def().scientist && InfectionData.isTransformed(sp)) {
                InfectionData.Entry e = InfectionData.server(sp.getUUID());
                if (e.huntedTicks <= 0) {
                    sp.displayClientMessage(Component.translatable("message.latexdrones.attack_scientist").withStyle(ChatFormatting.RED), false);
                    disobey(sp, e, 20);
                }
                e.huntedTicks = Math.max(e.huntedTicks, 600);
            }
            return InteractionResult.PASS;
        });
    }

    // ------------------------------------------------------------------ state helpers
    public static boolean isSpill(Block block) {
        return block instanceof PuddleBlock || block instanceof GooBlock || block instanceof LatexPlantBlock;
    }

    /** Should a drone (non-scientist) attack this transformed player? */
    public static boolean dronesHostileTo(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.SERVER.get(player.getUUID());
        return e != null && e.transformed && e.obedience < InfectionData.ROGUE;
    }

    /** Should a scientist attack this transformed player? */
    public static boolean scientistsHostileTo(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.SERVER.get(player.getUUID());
        return e != null && e.transformed && (e.obedience < InfectionData.ROGUE || e.huntedTicks > 0);
    }

    /** Fresh drone: full battery, average obedience, no task. */
    public static void resetDrone(InfectionData.Entry e) {
        e.energy = 100;
        e.obedience = 60;
        clear(e);
        e.orderCooldown = 600;
    }

    /** Clears task and standby (cure / reset). */
    public static void clear(InfectionData.Entry e) {
        e.task = 0;
        e.taskGoal = 0;
        e.taskProgress = 0;
        e.taskTicks = 0;
        e.taskGiver = "";
        e.standby = false;
        e.standbyTicks = 0;
        e.huntedTicks = 0;
    }

    private static void sync(ServerPlayer player) {
        ModNetworking.send(player, player.getUUID(), InfectionData.server(player.getUUID()));
    }

    /** Everyone needs to know about standby / rank changes (other players see them). */
    private static void syncAll(ServerPlayer player) {
        InfectionManager.syncToAll(player);
    }

    private static Component giverName(String giver) {
        return giver == null || giver.isEmpty() || "hive".equals(giver)
                ? Component.translatable("giver.latexdrones.hive")
                : Component.translatable("entity.latexdrones." + giver);
    }

    public static Component describe(InfectionData.Entry e) {
        DroneTask task = DroneTask.byId(e.task);
        if (task == DroneTask.PATROL) {
            return Component.translatable("task.latexdrones.patrol", e.taskX + ", " + e.taskZ);
        }
        return Component.translatable("task.latexdrones." + task.key, e.taskGoal);
    }

    public static String formatTime(int ticks) {
        int sec = Math.max(0, ticks / 20);
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    // ------------------------------------------------------------------ ticking (called every tick for transformed players)
    public static void tick(ServerPlayer player, ServerLevel level, InfectionData.Entry e) {
        if (e.standby) {
            tickStandby(player, level, e);
            return;
        }
        if (e.standbyCooldown > 0) {
            e.standbyCooldown--;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (e.orderCooldown > 0) {
            e.orderCooldown -= 20;
        }
        if (e.huntedTicks > 0) {
            e.huntedTicks -= 20;
        }

        // battery: -1% every 30 seconds (about 50 minutes for a full charge)
        if (!player.isCreative() && ++e.drainTimer >= 30) {
            e.drainTimer = 0;
            int before = e.energy;
            e.energy = Math.max(0, e.energy - 1);
            if (before >= 20 && e.energy < 20) {
                player.displayClientMessage(Component.translatable("message.latexdrones.low_power").withStyle(ChatFormatting.GOLD), false);
            }
        }
        if (e.energy < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, e.energy <= 0 ? 2 : 0, true, false, true));
            if (e.energy <= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, true, false, true));
            }
        }

        DroneTask task = DroneTask.byId(e.task);
        if (task != DroneTask.NONE) {
            tickTask(player, level, e, task);
        } else if (e.orderCooldown <= 0 && e.obedience >= InfectionData.ROGUE && !player.isCreative()) {
            // a scientist who sees an idle drone gives it something to do
            LatexDroneEntity scientist = nearestScientist(player, 8.0D);
            if (scientist != null && scientist.hasLineOfSight(player)) {
                assign(player, e, scientist.def().name);
            }
        }
        sync(player);
    }

    private static void tickTask(ServerPlayer player, ServerLevel level, InfectionData.Entry e, DroneTask task) {
        e.taskTicks -= 20;
        if (task == DroneTask.PATROL) {
            double dx = player.getX() - (e.taskX + 0.5D);
            double dz = player.getZ() - (e.taskZ + 0.5D);
            if (dx * dx + dz * dz < 16.0D) {
                e.taskProgress = 1;
            } else if (dx * dx + dz * dz < 160.0D * 160.0D) {
                // beacon column only this player sees
                for (int i = 0; i < 6; i++) {
                    level.sendParticles(player, ParticleTypes.END_ROD, true, e.taskX + 0.5D, e.taskY + 1.0D + i * 1.5D,
                            e.taskZ + 0.5D, 2, 0.1D, 0.3D, 0.1D, 0.0D);
                }
            }
        } else if (task == DroneTask.GUARD) {
            double d = player.distanceToSqr(e.taskX + 0.5D, e.taskY, e.taskZ + 0.5D);
            if (d < 64.0D) {
                e.taskProgress++;
            } else if (player.tickCount % 100 == 0) {
                level.sendParticles(player, ParticleTypes.END_ROD, true, e.taskX + 0.5D, e.taskY + 1.5D, e.taskZ + 0.5D,
                        8, 0.2D, 1.0D, 0.2D, 0.0D);
            }
        }
        if (task.autoComplete && e.taskProgress >= e.taskGoal) {
            complete(player, e);
            return;
        }
        if (e.taskTicks <= 0) {
            player.displayClientMessage(Component.translatable("message.latexdrones.task_failed", giverName(e.taskGiver),
                    String.valueOf(e.unit)).withStyle(ChatFormatting.RED), false);
            clearTask(e);
            disobey(player, e, 15);
        }
    }

    // ------------------------------------------------------------------ tasks
    /** Gives a new task (giver = scientist mob name or "hive"). Returns false if one is already running. */
    public static boolean assign(ServerPlayer player, InfectionData.Entry e, String giver) {
        if (e.task != 0) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        boolean villagerNear = !level.getEntitiesOfClass(AbstractVillager.class, player.getBoundingBox().inflate(48.0D)).isEmpty();
        DroneTask task;
        do {
            DroneTask[] options = {DroneTask.CLEAN, DroneTask.CLEAN, DroneTask.COLLECT, DroneTask.DELIVER, DroneTask.PATROL,
                    DroneTask.PATROL, DroneTask.GUARD, DroneTask.HUNT, DroneTask.CONVERT};
            task = options[random.nextInt(options.length)];
        } while (task == DroneTask.CONVERT && !villagerNear);

        e.task = task.ordinal();
        e.taskGoal = Mth.nextInt(random, task.minGoal, task.maxGoal);
        e.taskProgress = 0;
        e.taskTicks = task.seconds * 20;
        e.taskGiver = giver;
        BlockPos here = player.blockPosition();
        e.taskX = here.getX();
        e.taskY = here.getY();
        e.taskZ = here.getZ();
        if (task == DroneTask.PATROL) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = 24.0D + random.nextDouble() * 20.0D;
            e.taskX = here.getX() + (int) (Math.cos(angle) * dist);
            e.taskZ = here.getZ() + (int) (Math.sin(angle) * dist);
            e.taskY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, e.taskX, e.taskZ);
        } else if (task == DroneTask.CLEAN) {
            spawnSpills(level, here, e.taskGoal + 2, random);
        }
        player.displayClientMessage(Component.translatable("message.latexdrones.order", giverName(giver), String.valueOf(e.unit),
                describe(e), formatTime(e.taskTicks)).withStyle(ChatFormatting.AQUA), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TERMINAL_BEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
        sync(player);
        InfectionSavedData.markDirty(player.server);
        return true;
    }

    /** Somebody "spilled" latex nearby - this is what a cleaning task cleans up. */
    private static void spawnSpills(ServerLevel level, BlockPos center, int count, RandomSource random) {
        BlockState puddle = ModBlocks.BLACK_LATEX_PUDDLE.defaultBlockState();
        int placed = 0;
        for (int attempt = 0; attempt < count * 12 && placed < count; attempt++) {
            BlockPos column = center.offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = column.above(dy);
                BlockPos below = pos.below();
                if (level.getBlockState(pos).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlockAndUpdate(pos, puddle);
                    placed++;
                    break;
                }
            }
        }
    }

    /** Adds progress to the running task if it is of the given type. */
    public static void credit(ServerPlayer player, DroneTask type, int amount) {
        InfectionData.Entry e = InfectionData.SERVER.get(player.getUUID());
        if (e == null || !e.transformed || e.task != type.ordinal() || amount <= 0) {
            return;
        }
        e.taskProgress = Math.min(e.taskGoal, e.taskProgress + amount);
        player.displayClientMessage(Component.translatable("message.latexdrones.task_progress", e.taskProgress, e.taskGoal), true);
        if (type.autoComplete && e.taskProgress >= e.taskGoal) {
            complete(player, e);
        } else {
            sync(player);
        }
    }

    /** Report button / talking to a scientist: hands in items or shows progress. */
    public static void report(ServerPlayer player, InfectionData.Entry e) {
        DroneTask task = DroneTask.byId(e.task);
        if (task == DroneTask.NONE) {
            player.displayClientMessage(Component.translatable("message.latexdrones.no_task"), true);
            return;
        }
        if (task == DroneTask.COLLECT || task == DroneTask.DELIVER) {
            Item wanted = task == DroneTask.COLLECT ? ModItems.RAW_LATEX : Items.IRON_INGOT;
            Inventory inventory = player.getInventory();
            int have = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(wanted)) {
                    have += stack.getCount();
                }
            }
            if (have < e.taskGoal) {
                player.displayClientMessage(Component.translatable("message.latexdrones.task_missing_items", have, e.taskGoal), true);
                return;
            }
            int left = e.taskGoal;
            for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(wanted)) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
            e.taskProgress = e.taskGoal;
            complete(player, e);
            return;
        }
        if (e.taskProgress >= e.taskGoal) {
            complete(player, e);
        } else {
            player.displayClientMessage(Component.translatable("message.latexdrones.task_progress", e.taskProgress, e.taskGoal), true);
        }
    }

    private static void complete(ServerPlayer player, InfectionData.Entry e) {
        ServerLevel level = player.serverLevel();
        boolean wasRogue = e.obedience < InfectionData.ROGUE;
        boolean wasLoyal = e.obedience >= InfectionData.LOYAL;
        e.obedience = Math.min(100, e.obedience + 12);
        e.energy = Math.min(100, e.energy + 15);
        int oldRank = rank(e.tasksDone);
        e.tasksDone++;
        int newRank = rank(e.tasksDone);
        if (newRank > oldRank) {
            player.displayClientMessage(Component.translatable("order.latexdrones.promoted",
                    Component.translatable("rank.latexdrones." + RANKS[newRank])).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
            InfectionManager.upkeepForm(player); // higher ranks get extra hearts
        }
        player.displayClientMessage(Component.translatable("message.latexdrones.task_done", giverName(e.taskGiver)).withStyle(ChatFormatting.GREEN), false);
        if (!wasLoyal && e.obedience >= InfectionData.LOYAL) {
            player.displayClientMessage(Component.translatable("message.latexdrones.loyal").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        if (wasRogue && e.obedience >= InfectionData.ROGUE) {
            e.huntedTicks = 0;
        }
        // reward
        RandomSource random = player.getRandom();
        ItemStack reward;
        int roll = random.nextInt(10);
        if (roll < 4) {
            reward = new ItemStack(ModItems.RAW_LATEX, 2 + random.nextInt(3));
        } else if (roll < 7) {
            reward = new ItemStack(Items.EMERALD, 1 + random.nextInt(3));
        } else if (roll < 9) {
            reward = new ItemStack(ModItems.LATEX_DART, 3 + random.nextInt(4));
        } else {
            reward = new ItemStack(ModItems.DRONE_CORE);
        }
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.giveExperiencePoints(12);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TASK_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 10, 0.4D, 0.5D, 0.4D, 0.0D);
        clearTask(e);
        e.orderCooldown = 900;
        syncAll(player);
        InfectionSavedData.markDirty(player.server);
    }

    private static void clearTask(InfectionData.Entry e) {
        e.task = 0;
        e.taskGoal = 0;
        e.taskProgress = 0;
        e.taskTicks = 0;
        e.taskGiver = "";
    }

    // ------------------------------------------------------------------ disobedience
    /** Lowers obedience and punishes the unit ("correction"). */
    public static void disobey(ServerPlayer player, InfectionData.Entry e, int amount) {
        boolean wasRogue = e.obedience < InfectionData.ROGUE;
        e.obedience = Math.max(0, e.obedience - amount);
        e.huntedTicks = Math.max(e.huntedTicks, 400);
        e.orderCooldown = 1200;
        ServerLevel level = player.serverLevel();
        player.displayClientMessage(Component.translatable("message.latexdrones.correction", String.valueOf(e.unit))
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        player.hurt(level.damageSources().magic(), 2.0F);
        player.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CORRECTION, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.3D, 0.6D, 0.3D, 0.1D);
        if (!wasRogue && e.obedience < InfectionData.ROGUE) {
            player.displayClientMessage(Component.translatable("message.latexdrones.rogue").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
        }
        sync(player);
        InfectionSavedData.markDirty(player.server);
    }

    /** Sets obedience directly (/latex obedience). */
    public static void setObedience(ServerPlayer player, int value) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        e.obedience = Mth.clamp(value, 0, 100);
        if (e.obedience >= InfectionData.ROGUE) {
            e.huntedTicks = 0;
        }
        sync(player);
        InfectionSavedData.markDirty(player.server);
    }

    // ------------------------------------------------------------------ standby (sleep)
    private static boolean nearRestSpot(ServerPlayer player) {
        BlockPos c = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-3, -2, -3), c.offset(3, 2, 3))) {
            BlockState state = player.level().getBlockState(pos);
            if (state.is(ModBlocks.DRONE_POD) || state.is(ModBlocks.DRONE_CHARGING_PAD)) {
                return true;
            }
        }
        return false;
    }

    /** Puts the drone to sleep. Needs a pod or charging pad within 3 blocks unless {@code force}. */
    public static void enterStandby(ServerPlayer player, boolean force) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (!e.transformed || e.standby || e.standbyCooldown > 0) {
            return;
        }
        if (!force && !nearRestSpot(player)) {
            player.displayClientMessage(Component.translatable("message.latexdrones.standby_need_pad"), true);
            return;
        }
        e.standby = true;
        e.standbyTicks = 0;
        player.displayClientMessage(Component.translatable("message.latexdrones.standby_start"), true);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.STANDBY, SoundSource.PLAYERS, 1.0F, 1.0F);
        syncAll(player);
    }

    /** Right-clicked a charging pad or sleep pod: step onto it and power down. */
    public static void enterStandbyAt(ServerPlayer player, BlockPos pos) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (!e.transformed || e.standby) {
            return;
        }
        e.standbyCooldown = 0;
        BlockState state = player.level().getBlockState(pos);
        double y = state.is(ModBlocks.DRONE_POD) ? pos.getY() : pos.getY() + 1.0D;
        player.teleportTo(pos.getX() + 0.5D, y, pos.getZ() + 0.5D);
        enterStandby(player, true);
    }

    private static void tickStandby(ServerPlayer player, ServerLevel level, InfectionData.Entry e) {
        e.standbyTicks++;
        if (e.standbyTicks % 10 == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 128, false, false, false));
        }
        if (e.standbyTicks % 8 == 0) {
            e.energy = Math.min(100, e.energy + 1);
        }
        if (e.standbyTicks % 40 == 0) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
            player.getFoodData().eat(1, 0.5F);
        }
        if (e.standbyTicks % 20 == 0) {
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)); // no phantoms for well-rested drones
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.3D, player.getZ(), 3, 0.3D, 0.2D, 0.3D, 0.0D);
            sync(player);
        }
        // full battery wakes you up - except at night, when drones sleep until morning (or until you sneak)
        boolean charged = e.energy >= 100 && e.standbyTicks > 100 && !level.isNight();
        if (charged || (e.standbyTicks > 30 && player.isShiftKeyDown())) {
            wake(player, e, true);
        }
    }

    private static void wake(ServerPlayer player, InfectionData.Entry e, boolean rested) {
        e.standby = false;
        e.standbyTicks = 0;
        e.standbyCooldown = 100;
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        if (rested) {
            player.displayClientMessage(Component.translatable("message.latexdrones.standby_end"), true);
        }
        syncAll(player);
        InfectionSavedData.markDirty(player.server);
    }

    // ------------------------------------------------------------------ kills & night
    /** A drone player killed something: convertible victims rise as drones, intruders count for hunts. */
    public static void onKill(ServerPlayer player, ServerLevel level, LivingEntity victim) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (!(victim instanceof Player) && InfectionManager.isConvertible(victim)) {
            InfectionManager.spawnDroneFrom(level, victim, e.form);
            credit(player, DroneTask.CONVERT, 1);
            return;
        }
        boolean intruder = victim instanceof Player other ? !InfectionData.isTransformed(other)
                : victim instanceof Enemy && !(victim instanceof LatexDroneEntity);
        if (intruder) {
            credit(player, DroneTask.HUNT, 1);
        }
    }

    /** Lets the night pass when every player in a world rests (bed or standby) and at least one drone is in standby. */
    public static void tickWorlds(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.isNight()) {
                continue;
            }
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) {
                continue;
            }
            boolean anyStandby = false;
            boolean everyoneResting = true;
            for (ServerPlayer p : players) {
                if (p.isSpectator()) {
                    continue;
                }
                InfectionData.Entry e = InfectionData.SERVER.get(p.getUUID());
                boolean standby = e != null && e.standby && e.standbyTicks >= STANDBY_SKIP_NIGHT;
                anyStandby |= standby;
                if (!standby && !p.isSleepingLongEnough()) {
                    everyoneResting = false;
                    break;
                }
            }
            if (anyStandby && everyoneResting) {
                long next = level.getDayTime() + 24000L;
                level.setDayTime(next - next % 24000L);
                level.setWeatherParameters(0, 0, false, false);
                for (ServerPlayer p : players) {
                    p.displayClientMessage(Component.translatable("standby.latexdrones.morning").withStyle(ChatFormatting.AQUA), true);
                }
            }
        }
    }

    // ------------------------------------------------------------------ terminal & scientists
    public static LatexDroneEntity nearestScientist(ServerPlayer player, double range) {
        List<LatexDroneEntity> list = player.serverLevel().getEntitiesOfClass(LatexDroneEntity.class,
                player.getBoundingBox().inflate(range), d -> d.isAlive() && d.def().scientist);
        LatexDroneEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LatexDroneEntity d : list) {
            double dist = d.distanceToSqr(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }

    private static boolean nearTerminal(ServerPlayer player) {
        BlockPos c = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
            if (player.level().getBlockState(pos).is(ModBlocks.DRONE_TERMINAL)) {
                return true;
            }
        }
        return false;
    }

    /** Opens the terminal screen for a drone; humans are refused. */
    public static void openTerminal(ServerPlayer player) {
        if (!InfectionData.isTransformed(player)) {
            player.displayClientMessage(Component.translatable("message.latexdrones.access_denied").withStyle(ChatFormatting.RED), true);
            return;
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TERMINAL_BEEP, SoundSource.BLOCKS, 0.6F, 1.2F);
        sync(player);
        ModNetworking.openTerminal(player);
    }

    /** A drone player right-clicked a scientist. */
    public static void talkToScientist(ServerPlayer player, LatexDroneEntity scientist) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.task == 0 && e.obedience >= InfectionData.ROGUE && e.huntedTicks <= 0) {
            assign(player, e, scientist.def().name);
        } else if (e.task != 0) {
            report(player, e);
        }
        openTerminal(player);
    }

    private static void handleAction(ServerPlayer player, int action) {
        InfectionData.Entry e = InfectionData.SERVER.get(player.getUUID());
        if (e == null || !e.transformed || !player.isAlive()) {
            return;
        }
        LatexDroneEntity scientist = nearestScientist(player, 6.0D);
        if (scientist == null && !nearTerminal(player)) {
            return; // buttons only work at a terminal or next to a scientist
        }
        String giver = scientist != null ? scientist.def().name : "hive";
        switch (action) {
            case ACTION_REQUEST:
                if (e.task != 0) {
                    player.displayClientMessage(Component.translatable("message.latexdrones.task_busy"), true);
                } else if (e.obedience < InfectionData.ROGUE) {
                    player.displayClientMessage(Component.translatable("message.latexdrones.access_denied").withStyle(ChatFormatting.RED), true);
                } else {
                    assign(player, e, giver);
                }
                break;
            case ACTION_REPORT:
                report(player, e);
                break;
            case ACTION_REFUSE:
                if (e.task != 0) {
                    player.displayClientMessage(Component.translatable("message.latexdrones.task_refused", giverName(e.taskGiver))
                            .withStyle(ChatFormatting.RED), false);
                    clearTask(e);
                    disobey(player, e, 10);
                }
                break;
            case ACTION_STANDBY:
                enterStandby(player, false);
                break;
            default:
                break;
        }
        sync(player);
    }
}
