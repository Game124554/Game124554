package com.mandelahorror.mixin;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.client.ClientHorror;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin randomizer glitch: while it lasts, every player is drawn with a random default skin. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void mandelahorror$glitchSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        if (ClientHorror.isSkinGlitching() && HorrorConfig.SKIN_GLITCH.get()) {
            cir.setReturnValue(ClientHorror.glitchSkin(player));
        }
    }
}
