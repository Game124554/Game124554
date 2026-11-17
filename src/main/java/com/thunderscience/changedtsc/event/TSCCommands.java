package com.thunderscience.changedtsc.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.thunderscience.changedtsc.boss.BossManager;
import com.thunderscience.changedtsc.boss.BossType;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.infection.GooInfection;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import com.thunderscience.changedtsc.util.EntityResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import com.thunderscience.changedtsc.util.EntitySpawning;
import com.thunderscience.changedtsc.util.LatexHelper;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * /tsc boss &lt;type&gt;  – summon a boss where you stand
 * /tsc puro          – spawn a friendly Puro where you stand
 * /tsc resolve       – show which Changed entities were found (friendly Puro, bosses, structure latex)
 * /tsc reload        – re-scan entities after changing the config
 */
public final class TSCCommands {
    private TSCCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tsc")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("boss")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(BossType.values()).map(BossType::id), builder))
                                .executes(TSCCommands::summonBoss)))
                .then(Commands.literal("resolve").executes(TSCCommands::resolve))
                .then(Commands.literal("puro").executes(TSCCommands::summonPuro))
                .then(Commands.literal("goo")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> {
                                    for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                                        if (e instanceof LivingEntity le && GooInfection.isHost(le)) {
                                            float v = GooInfection.get(le);
                                            ctx.getSource().sendSuccess(() -> Component.literal("[TSC] " + e.getName().getString()
                                                    + " goo: " + Math.round(v) + "% (stage " + GooInfection.stageFor(v) + ")"), false);
                                        }
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("percent", FloatArgumentType.floatArg(0.0F, 100.0F))
                                        .executes(ctx -> {
                                            float pct = FloatArgumentType.getFloat(ctx, "percent");
                                            int n = 0;
                                            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                                                if (e instanceof LivingEntity le && GooInfection.isHost(le)) {
                                                    GooInfection.set(le, pct);
                                                    n++;
                                                }
                                            }
                                            int count = n;
                                            ctx.getSource().sendSuccess(() -> Component.literal("[TSC] Set goo to " + pct + "% on " + count + " target(s)."), true);
                                            return count;
                                        }))))
                .then(Commands.literal("reload").executes(ctx -> {
                    EntityResolver.invalidate();
                    ctx.getSource().sendSuccess(() -> Component.literal("[TSC] Entity lookups cleared."), true);
                    return 1;
                })));
    }

    private static int summonBoss(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "type");
        Optional<BossType> type = BossType.byId(id);
        if (type.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("[TSC] Unknown boss: " + id));
            return 0;
        }
        BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
        Mob boss = BossManager.spawnBoss(ctx.getSource().getLevel(), pos, type.get());
        if (boss == null) {
            ctx.getSource().sendFailure(Component.literal("[TSC] Could not spawn " + id + " - check /tsc resolve and the config."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("[TSC] Summoned " + id + " (" + EntityType.getKey(boss.getType()) + ")"), true);
        return 1;
    }

    private static int summonPuro(CommandContext<CommandSourceStack> ctx) {
        Optional<EntityType<?>> type = EntityResolver.resolve(TSCConfig.FRIENDLY_SPAWN_ENTITY.get());
        if (type.isEmpty() || !TSCConfig.FRIENDLY_PURO_ENABLED.get()) {
            ctx.getSource().sendFailure(Component.literal("[TSC] No entity found for Puro - check friendlyPuro in the config."));
            return 0;
        }
        BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
        Entity puro = EntitySpawning.spawn(ctx.getSource().getLevel(), type.get(), pos, MobSpawnType.COMMAND, true, LatexHelper.FRIENDLY_TAG);
        if (puro == null) {
            ctx.getSource().sendFailure(Component.literal("[TSC] Could not spawn Puro here."));
            return 0;
        }
        LatexHelper.namePuro(puro);
        ctx.getSource().sendSuccess(() -> Component.literal("[TSC] Friendly Puro spawned (" + EntityType.getKey(puro.getType()) + ")"), true);
        return 1;
    }

    private static int resolve(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String friendly = EntityResolver.friendlyTypes().stream()
                .map(t -> EntityType.getKey(t).toString()).collect(Collectors.joining(", "));
        src.sendSuccess(() -> Component.literal("[TSC] Latex namespaces: " + EntityResolver.latexNamespaces()), false);
        src.sendSuccess(() -> Component.literal("[TSC] Friendly types: " + (friendly.isEmpty() ? "(none - Puro is made friendly by tag)" : friendly)), false);
        String puro = EntityResolver.resolve(TSCConfig.FRIENDLY_SPAWN_ENTITY.get())
                .map(t -> EntityType.getKey(t).toString()).orElse("(none found)");
        src.sendSuccess(() -> Component.literal("[TSC] Puro spawns as: " + puro), false);
        src.sendSuccess(() -> Component.literal("[TSC] First Aid mod: " + (com.thunderscience.changedtsc.compat.FirstAidCompat.isLoaded()
                ? "installed, items found: " + com.thunderscience.changedtsc.compat.FirstAidCompat.lootItems().size() : "not installed")), false);
        for (BossType type : BossType.values()) {
            String found = EntityResolver.resolve(TSCConfig.BOSS_ENTITY.get(type).get())
                    .map(t -> EntityType.getKey(t).toString()).orElse("(none found)");
            src.sendSuccess(() -> Component.literal("[TSC] Boss " + type.id() + " -> " + found), false);
        }
        StringBuilder furniture = new StringBuilder();
        for (String kind : com.thunderscience.changedtsc.util.FurniturePlacer.specs().keySet()) {
            String found = com.thunderscience.changedtsc.util.FurniturePlacer.resolve(kind)
                    .map(block -> String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block)))
                    .orElse("vanilla");
            if (furniture.length() > 0) furniture.append(", ");
            furniture.append(kind).append(" -> ").append(found);
        }
        src.sendSuccess(() -> Component.literal("[TSC] Furniture: " + furniture), false);
        int pool = EntityResolver.latexPool().size();
        src.sendSuccess(() -> Component.literal("[TSC] Structure latex pool: " + pool + " entity types"), false);
        return 1;
    }
}
