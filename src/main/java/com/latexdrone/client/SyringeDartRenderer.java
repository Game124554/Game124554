package com.latexdrone.client;

import com.latexdrone.entity.SyringeDartEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

public class SyringeDartRenderer extends ProjectileEntityRenderer<SyringeDartEntity> {
    public SyringeDartRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SyringeDartEntity entity) {
        return FormTextures.DART;
    }
}
