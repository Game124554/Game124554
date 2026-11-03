package com.latexdrone.mixin.client;

import com.latexdrone.client.ClientLatexData;
import com.latexdrone.client.FormTextures;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin replacement: a latex player is drawn with their creature's skin (also in first person). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void latexdrone$latexSkin(CallbackInfoReturnable<Identifier> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        ClientLatexData.State s = ClientLatexData.get(self.getUuid());
        if (s != null && s.transformed) cir.setReturnValue(FormTextures.skin(s.form));
    }

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void latexdrone$wideArms(CallbackInfoReturnable<String> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        ClientLatexData.State s = ClientLatexData.get(self.getUuid());
        if (s != null && (s.transformed || s.transformTicks > 0)) cir.setReturnValue("default");
    }
}
