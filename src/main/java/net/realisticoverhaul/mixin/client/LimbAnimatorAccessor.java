package net.realisticoverhaul.mixin.client;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {
	@Accessor("prevSpeed")
	float realisticoverhaul$getPrevSpeed();

	@Accessor("prevSpeed")
	void realisticoverhaul$setPrevSpeed(float value);

	@Accessor("speed")
	float realisticoverhaul$getSpeed();

	@Accessor("speed")
	void realisticoverhaul$setSpeed(float value);

	@Accessor("pos")
	float realisticoverhaul$getPos();

	@Accessor("pos")
	void realisticoverhaul$setPos(float value);
}
