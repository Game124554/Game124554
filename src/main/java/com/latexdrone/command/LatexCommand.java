package com.latexdrone.command;

import com.latexdrone.content.FormData;
import com.latexdrone.content.LatexForm;
import com.latexdrone.infection.InfectionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /latex transform <form>   become a latex creature (with animation)
 * /latex cure               back to human
 * /latex infect <amount>    add infection %
 * /latex forms              list all forms
 */
public final class LatexCommand {
    private LatexCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("latex")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("transform")
                        .then(CommandManager.argument("form", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(FormData.FORMS.stream().map(LatexForm::id), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    LatexForm form = LatexForm.byId(StringArgumentType.getString(ctx, "form"));
                                    if (form == null) {
                                        ctx.getSource().sendError(Text.literal("Unknown latex form"));
                                        return 0;
                                    }
                                    InfectionManager.cure(player);
                                    InfectionManager.startTransformation(player, form.index());
                                    return 1;
                                })))
                .then(CommandManager.literal("cure").executes(ctx -> {
                    InfectionManager.cure(ctx.getSource().getPlayerOrThrow());
                    ctx.getSource().sendFeedback(() -> Text.literal("Cured."), false);
                    return 1;
                }))
                .then(CommandManager.literal("infect")
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0, 100)).executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            float amount = FloatArgumentType.getFloat(ctx, "amount");
                            InfectionManager.data(player).latexdrone$setInfection(InfectionManager.data(player).latexdrone$getInfection() + amount);
                            InfectionManager.sync(player);
                            return 1;
                        })))
                .then(CommandManager.literal("forms").executes(ctx -> {
                    StringBuilder sb = new StringBuilder("Latex forms: ");
                    for (LatexForm f : FormData.FORMS) sb.append(f.id()).append(' ');
                    ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                    return 1;
                })));
    }
}
