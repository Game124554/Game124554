package com.mandelahorror.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets us load a post-processing shader (the black & white VHS filter). */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("loadEffect")
    void mandelahorror$loadEffect(ResourceLocation id);
}
