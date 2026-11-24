package com.mandelahorror.client;

import com.mandelahorror.entity.AlternateEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** GeckoLib renderer. The white eyes glow in the dark (alternate_glowmask.png). */
public class AlternateRenderer extends GeoEntityRenderer<AlternateEntity> {

    public AlternateRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AlternateModel());
        this.shadowRadius = 0.6F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
