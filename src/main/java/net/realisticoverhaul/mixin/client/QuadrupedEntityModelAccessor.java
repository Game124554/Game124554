package net.realisticoverhaul.mixin.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(QuadrupedEntityModel.class)
public interface QuadrupedEntityModelAccessor {
	@Accessor("head")
	ModelPart realisticoverhaul$head();

	@Accessor("rightHindLeg")
	ModelPart realisticoverhaul$rightHindLeg();

	@Accessor("leftHindLeg")
	ModelPart realisticoverhaul$leftHindLeg();

	@Accessor("rightFrontLeg")
	ModelPart realisticoverhaul$rightFrontLeg();

	@Accessor("leftFrontLeg")
	ModelPart realisticoverhaul$leftFrontLeg();
}
