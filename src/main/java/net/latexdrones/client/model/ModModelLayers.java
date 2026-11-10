package net.latexdrones.client.model;

import net.latexdrones.LatexDrones;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
    private ModModelLayers() {
    }

    public static final ModelLayerLocation ANTHRO = new ModelLayerLocation(LatexDrones.id("anthro"), "main");
    public static final ModelLayerLocation BEAST = new ModelLayerLocation(LatexDrones.id("beast"), "main");
    public static final ModelLayerLocation BLOB = new ModelLayerLocation(LatexDrones.id("blob"), "main");
    public static final ModelLayerLocation HOVER = new ModelLayerLocation(LatexDrones.id("hover"), "main");
}
