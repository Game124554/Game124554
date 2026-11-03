package com.latexdrone.mixin;

import com.latexdrone.infection.LatexPlayerAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements LatexPlayerAccess {
    @Unique private float latexdrone$infection = 0f;
    @Unique private boolean latexdrone$transformed = false;
    @Unique private int latexdrone$form = 0;
    @Unique private int latexdrone$pendingForm = 0;
    @Unique private int latexdrone$transformTicks = 0;
    @Unique private int latexdrone$lastSent = -999;

    @Override public float latexdrone$getInfection() { return latexdrone$infection; }
    @Override public void latexdrone$setInfection(float v) { latexdrone$infection = Math.max(0f, Math.min(100f, v)); }
    @Override public boolean latexdrone$isTransformed() { return latexdrone$transformed; }
    @Override public void latexdrone$setTransformed(boolean v) { latexdrone$transformed = v; }
    @Override public int latexdrone$getForm() { return latexdrone$form; }
    @Override public void latexdrone$setForm(int f) { latexdrone$form = f; }
    @Override public int latexdrone$getPendingForm() { return latexdrone$pendingForm; }
    @Override public void latexdrone$setPendingForm(int f) { latexdrone$pendingForm = f; }
    @Override public int latexdrone$getTransformTicks() { return latexdrone$transformTicks; }
    @Override public void latexdrone$setTransformTicks(int t) { latexdrone$transformTicks = t; }
    @Override public int latexdrone$getLastSent() { return latexdrone$lastSent; }
    @Override public void latexdrone$setLastSent(int v) { latexdrone$lastSent = v; }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void latexdrone$write(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound tag = new NbtCompound();
        tag.putFloat("Infection", latexdrone$infection);
        tag.putBoolean("Transformed", latexdrone$transformed);
        tag.putInt("Form", latexdrone$form);
        tag.putInt("PendingForm", latexdrone$pendingForm);
        tag.putInt("TransformTicks", latexdrone$transformTicks);
        nbt.put("LatexDrone", tag);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void latexdrone$read(NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.contains("LatexDrone")) return;
        NbtCompound tag = nbt.getCompound("LatexDrone");
        latexdrone$infection = tag.getFloat("Infection");
        latexdrone$transformed = tag.getBoolean("Transformed");
        latexdrone$form = tag.getInt("Form");
        latexdrone$pendingForm = tag.getInt("PendingForm");
        latexdrone$transformTicks = tag.getInt("TransformTicks");
    }
}
