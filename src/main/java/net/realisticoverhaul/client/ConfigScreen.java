package net.realisticoverhaul.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.realisticoverhaul.config.PerformancePreset;
import net.realisticoverhaul.client.smooth.SmoothTerrain;
import net.realisticoverhaul.config.ROConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** In-game settings (press O). */
public class ConfigScreen extends Screen {
	private final Screen parent;
	private final boolean smoothAtOpen;
	private int scroll;
	private int maxScroll;

	public ConfigScreen(Screen parent) {
		super(Text.translatable("realisticoverhaul.config.title"));
		this.parent = parent;
		this.smoothAtOpen = ROConfig.get().smoothTerrain;
	}

	@Override
	protected void init() {
		ROConfig c = ROConfig.get();
		List<ClickableWidget> w = new ArrayList<>();

		w.add(CyclingButtonWidget.<PerformancePreset>builder(p -> Text.literal(p.name()))
				.values(PerformancePreset.values())
				.initially(c.performancePreset)
				.build(0, 0, 150, 20, Text.literal("Performance"), (b, v) -> {
					c.performancePreset = v;
					c.applyPreset();
				}));
		w.add(toggle("Smooth terrain (NoCubes)", c.smoothTerrain, v -> c.smoothTerrain = v));
		w.add(toggle("Walk up 1-block steps", c.stepAssist, v -> c.stepAssist = v));
		w.add(toggle("Smooth camera on steps", c.smoothStepCamera, v -> c.smoothStepCamera = v));
		w.add(toggle("Block collapse physics", c.blockPhysics, v -> c.blockPhysics = v));
		w.add(toggle("Trees fall", c.treesFall, v -> c.treesFall = v));
		w.add(toggle("Glass breaking", c.glassBreaking, v -> c.glassBreaking = v));
		w.add(toggle("Camera shake", c.cameraShake, v -> c.cameraShake = v));
		w.add(new IntSlider("Shake strength", "%", 0, 200, c.cameraShakeStrength, v -> c.cameraShakeStrength = v));
		w.add(toggle("Corpses / ragdoll", c.corpses, v -> c.corpses = v));
		w.add(new IntSlider("Corpse time", "s", 5, 600, c.corpseSeconds, v -> c.corpseSeconds = v));
		w.add(toggle("Ragdoll limbs", c.ragdollLimbs, v -> c.ragdollLimbs = v));
		w.add(new IntSlider("Max corpses", "", 1, 128, c.maxCorpses, v -> c.maxCorpses = v));
		w.add(toggle("Blood splatter", c.blood, v -> c.blood = v));
		w.add(toggle("Hurt mobs limp", c.woundedMobs, v -> c.woundedMobs = v));
		w.add(toggle("Rain puddles", c.puddles, v -> c.puddles = v));
		w.add(toggle("Snow build-up", c.snowAccumulation, v -> c.snowAccumulation = v));
		w.add(new IntSlider("Max snow layers", "", 1, 8, c.maxSnowLayers, v -> c.maxSnowLayers = v));
		w.add(toggle("Fast lava", c.fastLava, v -> c.fastLava = v));
		w.add(toggle("Steam (water+lava)", c.steamParticles, v -> c.steamParticles = v));
		w.add(toggle("Bedrock breakable", c.bedrockBreakable, v -> c.bedrockBreakable = v));
		w.add(toggle("Remove bedrock (new chunks)", c.removeBedrockWorldgen, v -> c.removeBedrockWorldgen = v));
		w.add(toggle("Hell underground/Nether", c.hellVisuals, v -> c.hellVisuals = v));
		w.add(toggle("Death sends to Nether", c.deathToNether, v -> c.deathToNether = v));
		w.add(toggle("Become Piglin in Nether", c.piglinForm, v -> c.piglinForm = v));

		int cols = Math.max(1, Math.min(3, (this.width - 20) / 154));
		int colW = Math.min(150, (this.width - 20) / cols - 4);
		int rows = (w.size() + cols - 1) / cols;
		int totalW = cols * colW + (cols - 1) * 4;
		int left = (this.width - totalW) / 2;
		int top = 30;
		int bottom = this.height - 32;
		maxScroll = Math.max(0, top + rows * 22 - bottom);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		for (int i = 0; i < w.size(); i++) {
			ClickableWidget widget = w.get(i);
			int col = i / rows;
			int row = i % rows;
			int y = top + row * 22 - scroll;
			if (y < top - 2 || y + 20 > bottom + 2) continue; // scrolled out of view
			widget.setX(left + col * (colW + 4));
			widget.setY(y);
			widget.setWidth(colW);
			addDrawableChild(widget);
		}

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
				.dimensions(this.width / 2 - 100, this.height - 26, 200, 20)
				.build());
	}

	private static ClickableWidget toggle(String name, boolean value, Consumer<Boolean> setter) {
		return CyclingButtonWidget.onOffBuilder(value).build(0, 0, 150, 20, Text.literal(name), (b, v) -> setter.accept(v));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (maxScroll > 0) {
			int old = scroll;
			scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (amount * 22)));
			if (scroll != old) clearAndInit();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
		if (maxScroll > 0) {
			context.drawTextWithShadow(this.textRenderer, Text.literal("Scroll for more"), 6, this.height - 20, 0xA0A0A0);
		}
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		ROConfig.save();
		SmoothTerrain.reload();
		if (ROConfig.get().smoothTerrain != smoothAtOpen) SmoothTerrain.rebuildChunks();
		if (this.client != null) this.client.setScreen(parent);
	}

	private static class IntSlider extends SliderWidget {
		private final String label;
		private final String suffix;
		private final int min;
		private final int max;
		private final IntConsumer setter;

		IntSlider(String label, String suffix, int min, int max, int value, IntConsumer setter) {
			super(0, 0, 150, 20, Text.empty(), (value - min) / (double) (max - min));
			this.label = label;
			this.suffix = suffix;
			this.min = min;
			this.max = max;
			this.setter = setter;
			updateMessage();
		}

		private int current() {
			return min + (int) Math.round(this.value * (max - min));
		}

		@Override
		protected void updateMessage() {
			if (label == null) return;
			setMessage(Text.literal(label + ": " + current() + suffix));
		}

		@Override
		protected void applyValue() {
			setter.accept(current());
		}
	}
}
