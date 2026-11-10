package net.latexdrones.mixin.client;

import net.latexdrones.client.TransformAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skin replacement: a transformed player renders with the texture of their latex form
 * (third person, first-person arm, inventory doll). Cosmetic only, hence require = 0.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true, require = 0)
    private void latexdrones$replaceSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation skin = TransformAnimation.skinFor((AbstractClientPlayer) (Object) this);
        if (skin != null) {
            cir.setReturnValue(skin);
        }
    }

    /** The form textures use the classic (4 pixel arm) layout, so force the wide-arm player model. */
    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true, require = 0)
    private void latexdrones$wideArms(CallbackInfoReturnable<String> cir) {
        if (TransformAnimation.skinFor((AbstractClientPlayer) (Object) this) != null) {
            cir.setReturnValue("default");
        }
    }
}
