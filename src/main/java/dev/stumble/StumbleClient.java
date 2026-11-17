package dev.stumble;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stumble.client.ClientNet;
import dev.stumble.client.PushAnimation;
import dev.stumble.client.Pushing;
import dev.stumble.client.RagdollManager;
import dev.stumble.common.Downed;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class StumbleClient implements ClientModInitializer {
    public static KeyMapping pushKey;

    @Override
    public void onInitializeClient() {
        pushKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.stumble.push", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.stumble"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (pushKey.consumeClick()) {
                Pushing.push(mc);
            }
            Pushing.tick();
            Pushing.bumps(mc);
            PushAnimation.tick();
            RagdollManager.tick(mc);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(RagdollManager::render);
        ClientNet.register();
        Downed.client = RagdollManager::isLocalDown;

        // no fighting, building or eating while you are a ragdoll
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> blocked(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, dir) -> blocked(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> blocked(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> blocked(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) -> blocked(player)
                ? InteractionResultHolder.fail(player.getItemInHand(hand))
                : InteractionResultHolder.pass(ItemStack.EMPTY));
    }

    private static boolean blocked(Player player) {
        return player.level().isClientSide && RagdollManager.isLocalDown(player);
    }
}
