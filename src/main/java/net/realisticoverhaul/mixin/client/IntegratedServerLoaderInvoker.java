package net.realisticoverhaul.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Used only by the development self-test to open a world without prompts. */
@Mixin(IntegratedServerLoader.class)
public interface IntegratedServerLoaderInvoker {
	@Invoker("start")
	void realisticoverhaul$start(Screen parent, String levelName, boolean safeMode, boolean canShowBackupPrompt);
}
