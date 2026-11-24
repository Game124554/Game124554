package com.mandelahorror.horror;

import com.mandelahorror.item.PaperData;
import com.mojang.brigadier.CommandDispatcher;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.IntroPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Test commands (op only):
 *  /mandela intro      - play the emergency broadcast screen
 *  /mandela fakejoin   - a fake player joins
 *  /mandela alternate  - spawn the Alternate behind you
 *  /mandela stab       - self-stab event now (needs a sword/pickaxe/axe in your hotbar, survival mode)
 *  /mandela glitch     - skin glitch for 10 seconds
 *  /mandela broadcast  - M.A.D. advisory
 *  /mandela voice      - play the Alternate's voice clip
 *  /mandela note       - get the note an Alternate drops when killed
 */
public final class HorrorCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mandela")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("intro").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ModNetwork.send(p, new IntroPacket(p.getGameProfile().getName()));
                    return 1;
                }))
                .then(Commands.literal("fakejoin").executes(ctx -> {
                    FakeJoins.start(ctx.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("alternate").executes(ctx -> {
                    boolean ok = HorrorDirector.spawnAlternateFor(ctx.getSource().getPlayerOrException());
                    feedback(ctx.getSource(), ok ? "It is behind you." : "No space to spawn here.");
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("stab").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    HorrorDirector.setInfluence(p, 35F);
                    if (!SelfStab.trigger(p, HorrorDirector.nearestAlternate(p, 32))) {
                        SelfStab.fallbackScare(p, HorrorDirector.nearestAlternate(p, 32));
                        feedback(ctx.getSource(), "No stab: you need survival mode and a sword/pickaxe/axe in your hotbar.");
                    }
                    return 1;
                }))
                .then(Commands.literal("glitch").executes(ctx -> {
                    ModNetwork.sendFx(ctx.getSource().getPlayerOrException(), FxPacket.SKIN_GLITCH, 200);
                    return 1;
                }))
                .then(Commands.literal("broadcast").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Speech.broadcast(p, Lines.night(p.getRandom()));
                    return 1;
                }))
                .then(Commands.literal("voice").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ALTERNATE_VOICE.get(),
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                    return 1;
                }))
                .then(Commands.literal("note").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ItemStack note = PaperData.alternateNote(p.getGameProfile().getName());
                    if (!p.getInventory().add(note)) p.drop(note, false);
                    return 1;
                })));
    }

    private static void feedback(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), false);
    }

    private HorrorCommands() {}
}
