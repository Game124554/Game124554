package net.scpfoundation.registry;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.scpfoundation.world.ZombiePlayers;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.scpfoundation.data.ScpDatabase;
import net.scpfoundation.item.ScpDocumentItem;

/**
 * /scp info &lt;number&gt;  - print a database file
 * /scp list             - list the anomalies present in this world
 * /scp give &lt;number&gt;  - give yourself a personnel file (operators)
 */
public final class ModCommands {
    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("scp")
                        .then(CommandManager.literal("info")
                                .then(CommandManager.argument("number", IntegerArgumentType.integer(ScpDatabase.MIN, ScpDatabase.MAX))
                                        .executes(ctx -> {
                                            int n = IntegerArgumentType.getInteger(ctx, "number");
                                            ServerCommandSource src = ctx.getSource();
                                            for (Text line : ScpDatabase.fileLines(n)) {
                                                src.sendFeedback(() -> line, false);
                                            }
                                            return 1;
                                        })))
                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    MutableText list = Text.empty();
                                    boolean first = true;
                                    for (ScpDatabase.Entry e : ScpDatabase.inGame()) {
                                        if (!first) list.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                                        list.append(Text.literal("SCP-" + ScpDatabase.label(e.number())).formatted(Formatting.GOLD));
                                        first = false;
                                    }
                                    Text msg = Text.translatable("command.scpfoundation.list", list);
                                    ctx.getSource().sendFeedback(() -> msg, false);
                                    return 1;
                                }))
                        .then(CommandManager.literal("zombify")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    ZombiePlayers.zombify(ctx.getSource().getPlayerOrThrow(), true);
                                    return 1;
                                })
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ZombiePlayers.zombify(EntityArgumentType.getPlayer(ctx, "player"), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("cure")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                    p.removeStatusEffect(net.scpfoundation.registry.ModEffects.SCP_008_INFECTION);
                                    ZombiePlayers.cure(p);
                                    return 1;
                                })
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
                                            p.removeStatusEffect(net.scpfoundation.registry.ModEffects.SCP_008_INFECTION);
                                            ZombiePlayers.cure(p);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("give")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("number", IntegerArgumentType.integer(ScpDatabase.MIN, ScpDatabase.MAX))
                                        .executes(ctx -> {
                                            int n = IntegerArgumentType.getInteger(ctx, "number");
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            player.getInventory().offerOrDrop(ScpDocumentItem.create(n));
                                            return 1;
                                        })))));
    }
}
