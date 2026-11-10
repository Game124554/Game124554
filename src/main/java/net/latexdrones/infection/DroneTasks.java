package net.latexdrones.infection;

import java.util.List;

import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModItems;
import net.latexdrones.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Life as a drone. Scientists give transformed players orders; finishing them earns rewards and drone ranks.
 * Drones sleep in standby mode on a charging pad (heals, refuels and lets the night pass).
 */
public final class DroneTasks {
    private DroneTasks() {
    }

    /** The orders a scientist can give. The ordinal is what gets saved and synced. */
    public enum Task {
        NONE(0, 0),
        COLLECT_LATEX(8, 16),
        COLLECT_IRON(4, 10),
        CONVERT(1, 3),
        HUNT(3, 6),
        RECHARGE(30, 30),
        GUARD(60, 90);

        public final int minGoal;
        public final int maxGoal;

        Task(int minGoal, int maxGoal) {
            this.minGoal = minGoal;
            this.maxGoal = maxGoal;
        }

        public String key() {
            return "task.latexdrones." + this.name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Collect orders are finished by handing the items to a scientist. */
        public boolean isDelivery() {
            return this == COLLECT_LATEX || this == COLLECT_IRON;
        }

        public static Task of(int ordinal) {
            Task[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    public static final String[] RANKS = {"drone", "loyal", "elite", "commander"};

    /** 0 = Drone, 1 = Loyal Drone (3 orders), 2 = Elite Drone (6), 3 = Commander Unit (10). */
    public static int rank(int tasksDone) {
        return tasksDone >= 10 ? 3 : (tasksDone >= 6 ? 2 : (tasksDone >= 3 ? 1 : 0));
    }

    private static final int GUARD_RADIUS = 10;
    private static final int ORDER_COOLDOWN = 400;
    private static final int STANDBY_SKIP_NIGHT = 100;

    public static void reset(InfectionData.Entry e) {
        e.task = 0;
        e.taskProgress = 0;
        e.taskGoal = 0;
        e.standby = false;
        e.standbyTicks = 0;
        e.orderCooldown = 0;
    }

    private static Component unit(InfectionData.Entry e) {
        return Component.literal("#" + e.unit);
    }

    private static Component taskText(InfectionData.Entry e) {
        return Component.translatable(Task.of(e.task).key(), e.taskGoal);
    }

    private static void say(ServerPlayer player, LatexDroneEntity scientist, String key, Object... args) {
        Component line = Component.translatable(key, args);
        player.sendSystemMessage(Component.empty()
                .append(scientist.getDisplayName().copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                .append(line.copy().withStyle(ChatFormatting.WHITE)));
    }

    // ------------------------------------------------------------------ scientists
    /** A drone player right-clicked a scientist. */
    public static void talk(ServerPlayer player, LatexDroneEntity scientist) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        scientist.getLookControl().setLookAt(player, 30.0F, 30.0F);
        Task task = Task.of(e.task);
        if (task == Task.NONE) {
            giveOrder(player, scientist, e);
            return;
        }
        if (task.isDelivery()) {
            Item item = task == Task.COLLECT_LATEX ? ModItems.RAW_LATEX : Items.IRON_INGOT;
            if (count(player.getInventory(), item) >= e.taskGoal) {
                take(player.getInventory(), item, e.taskGoal);
                e.taskProgress = e.taskGoal;
                complete(player, e, scientist);
                return;
            }
        }
        say(player, scientist, "order.latexdrones.remind", unit(e), taskText(e), e.taskProgress, e.taskGoal);
    }

    private static void giveOrder(ServerPlayer player, LatexDroneEntity scientist, InfectionData.Entry e) {
        Task[] pool = Task.values();
        Task task = pool[1 + player.getRandom().nextInt(pool.length - 1)];
        e.task = task.ordinal();
        e.taskGoal = task.minGoal + (task.maxGoal > task.minGoal ? player.getRandom().nextInt(task.maxGoal - task.minGoal + 1) : 0);
        e.taskProgress = 0;
        if (task == Task.GUARD) {
            e.guardX = player.getBlockX();
            e.guardZ = player.getBlockZ();
        }
        say(player, scientist, "order.latexdrones.give", unit(e), taskText(e));
        scientist.playSound(ModSounds.SCIENTIST_LAUGH, 1.0F, 1.0F);
        player.playNotifySound(ModSounds.HYPNOSIS, SoundSource.PLAYERS, 0.5F, 1.6F);
        InfectionManager.syncToAll(player);
        InfectionSavedData.markDirty(player.server);
    }

    /** Striking a scientist is disobedience: you get re-programmed (hypnosis + weakness). */
    public static void disobey(ServerPlayer player, LatexDroneEntity scientist) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        player.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        say(player, scientist, "order.latexdrones.disobey", unit(e));
        scientist.playSound(ModSounds.HYPNOSIS, 1.0F, 0.8F);
    }

    // ------------------------------------------------------------------ progress
    private static void progress(ServerPlayer player, InfectionData.Entry e, int amount) {
        Task task = Task.of(e.task);
        if (task == Task.NONE || e.taskProgress >= e.taskGoal) {
            return;
        }
        e.taskProgress = Math.min(e.taskGoal, e.taskProgress + amount);
        if (e.taskProgress >= e.taskGoal && !task.isDelivery()) {
            complete(player, e, null);
        } else {
            InfectionManager.syncToAll(player);
        }
    }

    /** A drone player turned a villager / illager / zombie into a drone. */
    public static void onConvert(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed && e.task == Task.CONVERT.ordinal()) {
            progress(player, e, 1);
        }
    }

    /** A drone player killed something. Drones assimilate what they kill. */
    public static void onKill(ServerPlayer player, ServerLevel level, LivingEntity victim) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (!(victim instanceof Player) && InfectionManager.isConvertible(victim)) {
            InfectionManager.spawnDroneFrom(level, victim, e.form);
            onConvert(player);
            return;
        }
        boolean hostile = victim instanceof Player other ? !InfectionData.isTransformed(other)
                : victim instanceof Monster && !(victim instanceof LatexDroneEntity);
        if (hostile && e.task == Task.HUNT.ordinal()) {
            progress(player, e, 1);
        }
    }

    private static void complete(ServerPlayer player, InfectionData.Entry e, LatexDroneEntity scientist) {
        int oldRank = rank(e.tasksDone);
        Task task = Task.of(e.task);
        e.tasksDone++;
        e.task = 0;
        e.taskProgress = 0;
        e.taskGoal = 0;
        e.orderCooldown = ORDER_COOLDOWN;
        // reward
        player.giveExperiencePoints(15 + 5 * rank(e.tasksDone));
        give(player, new ItemStack(Items.EMERALD, 1 + player.getRandom().nextInt(3)));
        if (player.getRandom().nextInt(3) == 0) {
            give(player, new ItemStack(ModItems.DRONE_CORE));
        }
        if (task == Task.HUNT || task == Task.GUARD) {
            give(player, new ItemStack(ModItems.LATEX_DART, 4));
        }
        if (scientist != null) {
            say(player, scientist, "order.latexdrones.done", unit(e));
            scientist.playSound(ModSounds.SCIENTIST_LAUGH, 1.0F, 1.2F);
        } else {
            player.displayClientMessage(Component.translatable("order.latexdrones.complete").withStyle(ChatFormatting.AQUA), false);
        }
        player.playNotifySound(ModSounds.TRANSFORM_COMPLETE, SoundSource.PLAYERS, 0.6F, 1.5F);
        int newRank = rank(e.tasksDone);
        if (newRank > oldRank) {
            player.displayClientMessage(Component.translatable("order.latexdrones.promoted",
                    Component.translatable("rank.latexdrones." + RANKS[newRank])).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
            InfectionManager.upkeepForm(player);
        }
        InfectionManager.syncToAll(player);
        InfectionSavedData.markDirty(player.server);
    }

    // ------------------------------------------------------------------ standby (sleep)
    public static void enterStandby(ServerPlayer player, BlockPos pad) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.standby || !e.transformed) {
            return;
        }
        e.standby = true;
        e.standbyTicks = 0;
        e.standbyPos = pad.asLong();
        player.teleportTo(pad.getX() + 0.5D, pad.getY() + 1.0D, pad.getZ() + 0.5D);
        player.displayClientMessage(Component.translatable("standby.latexdrones.enter"), true);
        player.serverLevel().playSound(null, pad, ModSounds.DRONE_DEATH, SoundSource.PLAYERS, 0.6F, 1.6F);
        InfectionManager.syncToAll(player);
    }

    private static void exitStandby(ServerPlayer player, InfectionData.Entry e, String reason) {
        e.standby = false;
        e.standbyTicks = 0;
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        player.displayClientMessage(Component.translatable(reason), true);
        player.playNotifySound(ModSounds.DRONE_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.2F);
        InfectionManager.syncToAll(player);
    }

    /** Called every tick for every transformed player. */
    public static void tick(ServerPlayer player, ServerLevel level, InfectionData.Entry e) {
        if (e.orderCooldown > 0) {
            e.orderCooldown--;
        }
        if (e.standby) {
            BlockPos pad = BlockPos.of(e.standbyPos);
            if (!level.getBlockState(pad).is(ModBlocks.DRONE_CHARGING_PAD)) {
                exitStandby(player, e, "standby.latexdrones.lost_pad");
                return;
            }
            if (player.isShiftKeyDown() || player.hurtTime > 0
                    || player.distanceToSqr(pad.getX() + 0.5D, pad.getY() + 1.0D, pad.getZ() + 0.5D) > 0.8D) {
                exitStandby(player, e, player.hurtTime > 0 ? "standby.latexdrones.interrupted" : "standby.latexdrones.wake");
                return;
            }
            e.standbyTicks++;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false, false));
            if (e.standbyTicks % 20 == 0) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(1.0F);
                }
                if (e.standbyTicks % 40 == 0) {
                    player.getFoodData().eat(1, 0.5F);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(), 4, 0.3D, 0.6D, 0.3D, 0.01D);
                if (e.task == Task.RECHARGE.ordinal()) {
                    progress(player, e, 1);
                }
                if (e.standbyTicks % 200 == 0) {
                    InfectionManager.syncToAll(player);
                }
            }
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        Task task = Task.of(e.task);
        if (task == Task.GUARD) {
            int dx = player.getBlockX() - e.guardX;
            int dz = player.getBlockZ() - e.guardZ;
            if (dx * dx + dz * dz <= GUARD_RADIUS * GUARD_RADIUS) {
                progress(player, e, 1);
            } else if (player.tickCount % 100 == 0) {
                player.displayClientMessage(Component.translatable("order.latexdrones.return_post"), true);
            }
        } else if (task.isDelivery()) {
            Item item = task == Task.COLLECT_LATEX ? ModItems.RAW_LATEX : Items.IRON_INGOT;
            int have = Math.min(e.taskGoal, count(player.getInventory(), item));
            if (have != e.taskProgress) {
                e.taskProgress = have;
                InfectionManager.syncToAll(player);
            }
        } else if (task == Task.NONE && e.orderCooldown <= 0 && player.tickCount % 100 == 0) {
            // scientists nearby call idle drones over and hand out work
            List<LatexDroneEntity> scientists = level.getEntitiesOfClass(LatexDroneEntity.class,
                    player.getBoundingBox().inflate(8.0D), s -> s.isAlive() && s.def().scientist && s.getTarget() == null);
            if (!scientists.isEmpty()) {
                giveOrder(player, scientists.get(0), e);
            }
        }
    }

    /** Lets the night pass when every player in a world is asleep in a bed or in standby (at least one in standby). */
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

    // ------------------------------------------------------------------ inventory helpers
    private static int count(Inventory inventory, Item item) {
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static void take(Inventory inventory, Item item, int amount) {
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int n = Math.min(amount, stack.getCount());
                stack.shrink(n);
                amount -= n;
            }
        }
        inventory.setChanged();
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
