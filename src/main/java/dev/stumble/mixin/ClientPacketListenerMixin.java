package dev.stumble.mixin;

import dev.stumble.client.RagdollManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the ragdolls where explosions happen, so bodies are thrown away from them. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // TAIL, not HEAD: the first call arrives on the network thread and is re-run on the client thread,
    // and only the client-thread run gets to the end of the method.
    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void stumble$onExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        RagdollManager.onExplosion(packet.getX(), packet.getY(), packet.getZ(), packet.getPower());
    }
}
