package com.latexdrone.infection;

/** Duck interface mixed into PlayerEntity; holds the infection state (saved in player NBT). */
public interface LatexPlayerAccess {
    float latexdrone$getInfection();
    void latexdrone$setInfection(float value);
    boolean latexdrone$isTransformed();
    void latexdrone$setTransformed(boolean value);
    int latexdrone$getForm();
    void latexdrone$setForm(int form);
    int latexdrone$getPendingForm();
    void latexdrone$setPendingForm(int form);
    int latexdrone$getTransformTicks();
    void latexdrone$setTransformTicks(int ticks);
    int latexdrone$getLastSent();
    void latexdrone$setLastSent(int value);
}
