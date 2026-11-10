package net.latexdrones.content;

/** Body plan of a mob - decides which model / renderer it uses. */
public enum Archetype {
    /** Two-legged latex drone / anthro creature (player-like model with optional ears, tail, visor ...). */
    HUMANOID,
    /** Four-legged feral beast. */
    BEAST,
    /** Wobbling goo blob. */
    BLOB,
    /** Mechanical hovering drone. */
    HOVER
}
