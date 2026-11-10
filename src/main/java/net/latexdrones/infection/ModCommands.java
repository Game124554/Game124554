package net.latexdrones.infection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.latexdrones.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator commands (permission level 2):
 * <pre>
 * /latex infect &lt;player&gt; &lt;0-100&gt;      set the infection meter
 * /latex transform &lt;player&gt; [form]    transform right away (form = any mob id of the mod)
 * /latex cure &lt;player&gt;                remove infection / turn human again
 * </pre>
 */
public final class ModCommands {
    private ModCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("latex").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("infect")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                                .executes(ModCommands::infect))))
                        .then(Commands.literal("transform")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> transform(ctx, "drone_unit"))
                                        .then(Commands.argument("form", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ModEntities.BY_NAME.keySet(), builder))
                                                .executes(ctx -> transform(ctx, StringArgumentType.getString(ctx, "form"))))))
                        .then(Commands.literal("cure")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ModCommands::cure)))));
    }

    private static int infect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int percent = IntegerArgumentType.getInteger(ctx, "percent");
        InfectionManager.setInfection(target, percent * 10);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.latexdrones.infect", target.getDisplayName(), percent), true);
        return 1;
    }

    private static int transform(CommandContext<CommandSourceStack> ctx, String form) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        if (!ModEntities.BY_NAME.containsKey(form)) {
            ctx.getSource().sendFailure(Component.translatable("commands.latexdrones.unknown_form", form));
            return 0;
        }
        InfectionManager.forceTransform(target, form);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.latexdrones.transform", target.getDisplayName(),
                Component.translatable("form.latexdrones." + form)), true);
        return 1;
    }

    private static int cure(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        InfectionManager.cure(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.latexdrones.cure", target.getDisplayName()), true);
        return 1;
    }
}
