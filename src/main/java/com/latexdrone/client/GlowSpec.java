package com.latexdrone.client;

import net.minecraft.util.Identifier;

/** Emissive (glowing in the dark) eyes / visor layer. */
public record GlowSpec(Identifier texture, float strength) {
}
