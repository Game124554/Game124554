package net.latexdrones.infection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.latexdrones.content.Archetype;
import net.latexdrones.content.MobDef;
import net.latexdrones.drone.DroneLife;
import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModEntities;
import net.latexdrones.registry.ModItems;
import net.latexdrones.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side infection logic: the infection meter, the transformation sequence, life as a drone, cures,
 * and turning villagers &amp; co. into new drones.
 */
public final class InfectionManager {
    private InfectionManager() {
    }

    private static final UUID FORM_HEALTH = UUID.fromString("b1a7e0d2-51c4-4f0e-9c1b-6d1f00000001");
    private static final UUID FORM_DAMAGE = UUID.fromString("b1a7e0d2-51c4-4f0e-9c1b-6d1f00000002");
    private static final UUID FORM_SPEED = UUID.fromString("b1a7e0d2-51c4-4f0e-9c1b-6d1f00000003");
    private static final UUID FORM_RANK = UUID.fromString("b1a7e0d2-51c4-4f0e-9c1b-6d1f00000006");
    /** Players who were killed by the hive and should be told so when they respawn. */
    private static final Set<UUID> ASSIMILATED = ConcurrentHashMap.newKeySet();

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            InfectionData.SERVER.clear();
            InfectionSavedData.get(server); // loads the saved state into InfectionData.SERVER
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> InfectionData.SERVER.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
            if (server.getTickCount() % 20 == 0) {
                DroneLife.tickWorlds(server);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player, server));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onRespawn(newPlayer));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                onPlayerDeath(player, source);
            }
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(InfectionManager::onKill);
        // a transformed player spreads the infection with bare-handed (or any) hits on other players
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide && entity instanceof ServerPlayer victim && InfectionData.isTransformed(player)) {
                addInfection(victim, 100, InfectionData.server(player.getUUID()).form);
            }
            return InteractionResult.PASS;
        });
    }

    // ------------------------------------------------------------------ public API
    /**
     * Adds infection to a player.
     *
     * @param amount 0..1000 (1000 = 100%)
     * @param form   mob the player will turn into if this pushes them over the edge ("" = keep current)
     */
    public static void addInfection(ServerPlayer player, int amount, String form) {
        if (amount <= 0 || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed || e.transformTicks > 0) {
            return;
        }
        if (player.hasEffect(ModEffects.INOCULATED)) {
            amount /= 4;
            if (amount <= 0) {
                return;
            }
        }
        boolean first = e.infection <= 0;
        e.infection = Math.min(InfectionData.MAX, e.infection + amount);
        if (form != null && !form.isEmpty() && ModEntities.BY_NAME.containsKey(form)) {
            e.form = form;
        }
        if (first) {
            player.displayClientMessage(Component.translatable("message.latexdrones.infected").withStyle(ChatFormatting.DARK_PURPLE), false);
            player.playNotifySound(ModSounds.HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (e.infection >= InfectionData.MAX) {
            startTransformation(player);
        } else {
            syncSelf(player);
        }
        InfectionSavedData.markDirty(player.server);
    }

    /** Sets the meter directly (used by /latex infect); ignores game mode and immunity. */
    public static void setInfection(ServerPlayer player, int value) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed || e.transformTicks > 0) {
            return;
        }
        e.infection = Math.max(0, Math.min(InfectionData.MAX, value));
        if (e.infection <= 0) {
            player.removeEffect(ModEffects.LATEX_INFECTION);
        }
        if (e.infection >= InfectionData.MAX) {
            startTransformation(player);
        } else {
            syncSelf(player);
        }
        InfectionSavedData.markDirty(player.server);
    }

    /** Lowers the meter of a not-yet-transformed player. Returns false if there was nothing to treat. */
    public static boolean reduce(ServerPlayer player, int amount) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed || e.transformTicks > 0 || e.infection <= 0) {
            return false;
        }
        e.infection = Math.max(0, e.infection - amount);
        if (e.infection <= 0) {
            player.removeEffect(ModEffects.LATEX_INFECTION);
        }
        syncSelf(player);
        InfectionSavedData.markDirty(player.server);
        return true;
    }

    /** Starts the transformation right away (conversion chamber, /latex transform). */
    public static void forceTransform(ServerPlayer player, String form) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed || e.transformTicks > 0) {
            return;
        }
        if (form != null && !form.isEmpty() && ModEntities.BY_NAME.containsKey(form)) {
            e.form = form;
        }
        startTransformation(player);
        InfectionSavedData.markDirty(player.server);
    }

    /** Full cure: removes the infection and turns a transformed player human again. */
    public static boolean cure(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (!e.transformed && e.infection <= 0 && e.transformTicks <= 0) {
            return false;
        }
        boolean wasTransformed = e.transformed;
        e.transformed = false;
        e.transformTicks = 0;
        e.infection = 0;
        e.form = "";
        DroneLife.clear(e);
        removeFormModifiers(player);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        player.removeEffect(ModEffects.LATEX_INFECTION);
        player.removeEffect(ModEffects.HYPNOSIS);
        if (wasTransformed) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            player.displayClientMessage(Component.translatable("message.latexdrones.cured").withStyle(ChatFormatting.AQUA), false);
        }
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CURE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.0D);
        syncToAll(player);
        InfectionSavedData.markDirty(player.server);
        return true;
    }

    /** Called by infectious blocks: at most one dose per second, sneaking keeps you safe. */
    public static void touch(Entity entity, Level level, int power) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && level.getGameTime() % 20L == 0L
                && !player.isSteppingCarefully()) {
            addInfection(player, power, "");
        }
    }

    /** Villagers, illagers, witches and zombies can be turned into drones. */
    public static boolean isConvertible(LivingEntity entity) {
        return entity instanceof AbstractVillager || entity instanceof Raider || entity instanceof Zombie;
    }

    /** Spawns a drone where {@code victim} stands. Non-humanoid or unknown forms fall back to the basic unit. */
    public static void spawnDroneFrom(ServerLevel level, LivingEntity victim, String form) {
        MobDef def = ModEntities.BY_NAME.get(form == null ? "" : form);
        if (def == null || def.scientist || def.archetype != Archetype.HUMANOID || def.isBig()) {
            def = ModEntities.form("drone_unit");
        }
        LatexDroneEntity drone = def.type.create(level);
        if (drone == null) {
            return;
        }
        drone.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
        if (victim.hasCustomName()) {
            drone.setCustomName(victim.getCustomName());
        }
        drone.setPersistenceRequired();
        level.addFreshEntity(drone);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.TRANSFORM_COMPLETE, SoundSource.HOSTILE, 0.8F, 1.2F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.BLACK_LATEX_BLOCK.defaultBlockState()),
                victim.getX(), victim.getY() + 1.0D, victim.getZ(), 30, 0.3D, 0.6D, 0.3D, 0.05D);
    }

    // ------------------------------------------------------------------ transformation
    private static void startTransformation(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed || e.transformTicks > 0) {
            return;
        }
        if (!ModEntities.BY_NAME.containsKey(e.form)) {
            e.form = "drone_unit";
        }
        e.infection = InfectionData.MAX;
        e.transformTicks = 1;
        player.displayClientMessage(Component.translatable("message.latexdrones.transform_start").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSFORM_START, SoundSource.PLAYERS, 1.2F, 1.0F);
        syncToAll(player);
    }

    private static void tickTransformation(ServerPlayer player, ServerLevel level, InfectionData.Entry e) {
        e.transformTicks++;
        // rooted in place while the latex takes over
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false, false));
        float progress = e.transformTicks / (float) InfectionData.TRANSFORM_TICKS;
        if (e.transformTicks % 3 == 0) {
            BlockState goo = (ModEntities.form(e.form).color & 0xFF) > 0xB0
                    ? ModBlocks.WHITE_LATEX_BLOCK.defaultBlockState() : ModBlocks.BLACK_LATEX_BLOCK.defaultBlockState();
            // the goo climbs from the feet to the head
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, goo), player.getX(),
                    player.getY() + player.getBbHeight() * Math.min(1.0F, progress * 1.3F), player.getZ(), 8, 0.3D, 0.1D, 0.3D, 0.02D);
        }
        if (e.transformTicks % 30 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GOO_SPLAT, SoundSource.PLAYERS, 0.8F, 0.8F + progress * 0.4F);
        }
        if (e.transformTicks >= InfectionData.TRANSFORM_TICKS) {
            completeTransformation(player, level, e);
        }
    }

    private static void completeTransformation(ServerPlayer player, ServerLevel level, InfectionData.Entry e) {
        e.transformTicks = 0;
        e.transformed = true;
        e.infection = InfectionData.MAX;
        if (e.unit <= 0) {
            e.unit = 1000 + player.getRandom().nextInt(9000);
        }
        DroneLife.resetDrone(e);
        player.removeEffect(ModEffects.LATEX_INFECTION);
        player.removeEffect(ModEffects.HYPNOSIS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        upkeepForm(player);
        player.setHealth(player.getMaxHealth());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSFORM_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.0F);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.05D);
        player.displayClientMessage(Component.translatable("message.latexdrones.transformed",
                Component.translatable("form.latexdrones." + e.form)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        syncToAll(player);
        InfectionSavedData.markDirty(player.server);
    }

    /** Keeps the perks of the latex form applied (stronger, faster, tougher, sees in the dark). */
    public static void upkeepForm(ServerPlayer player) {
        ensureModifier(player, Attributes.MAX_HEALTH, FORM_HEALTH, 8.0D, AttributeModifier.Operation.ADDITION);
        ensureModifier(player, Attributes.ATTACK_DAMAGE, FORM_DAMAGE, 2.0D, AttributeModifier.Operation.ADDITION);
        ensureModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED, 0.12D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // higher drone ranks get one more heart each
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double rankBonus = 2.0D * DroneLife.rank(InfectionData.server(player.getUUID()).tasksDone);
        if (health != null) {
            AttributeModifier current = health.getModifier(FORM_RANK);
            if (current == null || current.getAmount() != rankBonus) {
                if (current != null) {
                    health.removeModifier(FORM_RANK);
                }
                if (rankBonus > 0.0D) {
                    health.addTransientModifier(new AttributeModifier(FORM_RANK, "Drone rank", rankBonus, AttributeModifier.Operation.ADDITION));
                }
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
    }

    private static void ensureModifier(ServerPlayer player, Attribute attribute, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null && inst.getModifier(id) == null) {
            inst.addTransientModifier(new AttributeModifier(id, "Latex form", amount, op));
        }
    }

    private static void removeFormModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, FORM_HEALTH);
        removeModifier(player, Attributes.ATTACK_DAMAGE, FORM_DAMAGE);
        removeModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED);
        removeModifier(player, Attributes.MAX_HEALTH, FORM_RANK);
    }

    private static void removeModifier(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null && inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
    }

    // ------------------------------------------------------------------ ticking
    private static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (e.transformTicks > 0) {
            tickTransformation(player, level, e);
            return;
        }
        if (e.transformed) {
            if (player.tickCount % 40 == 0) {
                upkeepForm(player);
            }
            DroneLife.tick(player, level, e);
            return;
        }
        if (player.tickCount % 20 != 0 || player.isCreative() || player.isSpectator()) {
            return;
        }
        scanSurroundings(player, level);
        if (e.infection <= 0) {
            return;
        }
        // the infection slowly grows on its own: about 0.4% per second
        int before = e.infection;
        if (!player.hasEffect(ModEffects.INOCULATED)) {
            e.infection = Math.min(InfectionData.MAX, e.infection + 4);
        }
        int stage = Math.min(3, e.infection / 250);
        player.addEffect(new MobEffectInstance(ModEffects.LATEX_INFECTION, 60, stage, true, false, true));
        if (before < 250 && e.infection >= 250) {
            player.displayClientMessage(Component.translatable("message.latexdrones.stage1").withStyle(ChatFormatting.GRAY), false);
        } else if (before < 500 && e.infection >= 500) {
            player.displayClientMessage(Component.translatable("message.latexdrones.stage2").withStyle(ChatFormatting.GRAY), false);
        } else if (before < 750 && e.infection >= 750) {
            player.displayClientMessage(Component.translatable("message.latexdrones.stage3").withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        if (stage >= 2 && player.getRandom().nextInt(12) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, false));
        }
        if (stage >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, false));
        }
        if (e.infection >= InfectionData.MAX) {
            startTransformation(player);
        } else {
            syncSelf(player);
        }
        if (e.infection % 40 < 4) {
            InfectionSavedData.markDirty(player.server);
        }
    }

    /** Hypno screens and gas vents affect players who linger near them. */
    private static void scanSurroundings(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        boolean screen = false;
        int vents = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 3, 5))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.HYPNO_SCREEN) || state.is(ModBlocks.HYPNO_PYLON)) {
                screen = true;
            } else if (state.is(ModBlocks.LATEX_GAS_VENT) && pos.distSqr(center) <= 12.0D) {
                vents++;
            }
        }
        if (screen) {
            player.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, 70, 0, true, true, true));
        }
        if (vents > 0) {
            addInfection(player, Math.min(60, 25 * vents), "goo_walker_green");
        }
    }

    // ------------------------------------------------------------------ events
    private static void onJoin(ServerPlayer player, MinecraftServer server) {
        InfectionData.Entry own = InfectionData.server(player.getUUID());
        if (own.transformTicks > 0) {
            own.transformTicks = 1; // logged out mid-transformation: start the sequence again
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            ModNetworking.send(player, other.getUUID(), InfectionData.server(other.getUUID()));
            if (other != player) {
                ModNetworking.send(other, player.getUUID(), own);
            }
        }
        ModNetworking.send(player, player.getUUID(), own);
        if (own.transformed) {
            upkeepForm(player);
        }
    }

    private static void onRespawn(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        if (e.transformed) {
            upkeepForm(player);
            player.setHealth(player.getMaxHealth());
            if (ASSIMILATED.remove(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.latexdrones.assimilated_death").withStyle(ChatFormatting.DARK_PURPLE), false);
                player.displayClientMessage(Component.translatable("message.latexdrones.transformed",
                        Component.translatable("form.latexdrones." + e.form)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        syncToAll(player);
    }

    private static void onPlayerDeath(ServerPlayer player, DamageSource source) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        e.standby = false;
        if (e.transformed) {
            e.energy = Math.max(e.energy, 50);
            return; // once a drone, always a drone (until cured)
        }
        Entity killer = source.getEntity();
        if (killer instanceof LatexDroneEntity drone) {
            // killed by the hive: you come back as one of them
            MobDef def = drone.def();
            e.form = def.scientist ? (ModEntities.BY_NAME.containsKey(e.form) ? e.form : "drone_unit") : def.name;
            e.transformed = true;
            e.transformTicks = 0;
            e.infection = InfectionData.MAX;
            if (e.unit <= 0) {
                e.unit = 1000 + player.getRandom().nextInt(9000);
            }
            DroneLife.resetDrone(e);
            ASSIMILATED.add(player.getUUID());
        } else {
            e.infection = 0;
            e.transformTicks = 0;
        }
        InfectionSavedData.markDirty(player.server);
    }

    private static void onKill(ServerLevel level, Entity killer, LivingEntity victim) {
        if (killer instanceof ServerPlayer drone && InfectionData.isTransformed(drone)) {
            DroneLife.onKill(drone, level, victim);
            return;
        }
        if (victim instanceof Player || !isConvertible(victim)) {
            return;
        }
        if (killer instanceof LatexDroneEntity drone && !drone.def().scientist) {
            spawnDroneFrom(level, victim, drone.def().name);
        } else if (killer instanceof Player player && player.getMainHandItem().is(ModItems.LATEX_BLADE)) {
            spawnDroneFrom(level, victim, "drone_unit");
        }
    }

    // ------------------------------------------------------------------ sync
    private static void syncSelf(ServerPlayer player) {
        ModNetworking.send(player, player.getUUID(), InfectionData.server(player.getUUID()));
    }

    public static void syncToAll(ServerPlayer player) {
        InfectionData.Entry e = InfectionData.server(player.getUUID());
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            ModNetworking.send(other, player.getUUID(), e);
        }
        if (!player.server.getPlayerList().getPlayers().contains(player)) {
            ModNetworking.send(player, player.getUUID(), e);
        }
    }
}
