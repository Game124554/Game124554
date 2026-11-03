package com.latexdrone.client;

/** Which extra body parts to draw (ears, tail, wings...), their colours and how grown-in they are (0..1). */
public record PartsSpec(int flags, int color, int color2, float grow) {
}
