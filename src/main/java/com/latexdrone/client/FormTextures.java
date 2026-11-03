package com.latexdrone.client;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.content.FormData;
import com.latexdrone.content.ScientistData;
import net.minecraft.util.Identifier;

public final class FormTextures {
    private static final Identifier[] SKIN = new Identifier[FormData.FORMS.size()];
    private static final Identifier[] GLOW = new Identifier[FormData.FORMS.size()];
    private static final Identifier[] SCI_SKIN = new Identifier[ScientistData.SCIENTISTS.size()];
    private static final Identifier[] SCI_GLOW = new Identifier[ScientistData.SCIENTISTS.size()];
    public static final Identifier PARTS = LatexDroneMod.id("textures/entity/latex_parts.png");
    public static final Identifier SPIRAL = LatexDroneMod.id("textures/gui/hypno_spiral.png");
    public static final Identifier DART = LatexDroneMod.id("textures/entity/syringe_dart.png");

    static {
        for (int i = 0; i < SKIN.length; i++) {
            String id = FormData.FORMS.get(i).id();
            SKIN[i] = LatexDroneMod.id("textures/entity/latex/" + id + ".png");
            GLOW[i] = LatexDroneMod.id("textures/entity/latex/" + id + "_glow.png");
        }
        for (int i = 0; i < SCI_SKIN.length; i++) {
            String id = ScientistData.SCIENTISTS.get(i).id();
            SCI_SKIN[i] = LatexDroneMod.id("textures/entity/scientist/" + id + ".png");
            SCI_GLOW[i] = LatexDroneMod.id("textures/entity/scientist/" + id + "_glow.png");
        }
    }

    private FormTextures() {}

    private static int clamp(int i, int len) {
        return Math.max(0, Math.min(i, len - 1));
    }

    public static Identifier skin(int form) { return SKIN[clamp(form, SKIN.length)]; }
    public static Identifier glow(int form) { return GLOW[clamp(form, GLOW.length)]; }
    public static Identifier scientistSkin(int i) { return SCI_SKIN[clamp(i, SCI_SKIN.length)]; }
    public static Identifier scientistGlow(int i) { return SCI_GLOW[clamp(i, SCI_GLOW.length)]; }
}
