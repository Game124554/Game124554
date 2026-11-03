package net.realisticoverhaul.config;

/** Performance limits. Lower presets do less work per tick. */
public enum PerformancePreset {
	LOW(8, 8, 2, 1),
	MEDIUM(32, 16, 6, 3),
	HIGH(64, 32, 12, 6),
	ULTRA(256, 64, 24, 12);

	public final int physicsUpdatesPerTick;
	public final int maxCorpses;
	public final int particleDensity;
	public final int weatherAttemptsPerTick;

	PerformancePreset(int physics, int corpses, int particles, int weather) {
		this.physicsUpdatesPerTick = physics;
		this.maxCorpses = corpses;
		this.particleDensity = particles;
		this.weatherAttemptsPerTick = weather;
	}

	public void applyTo(ROConfig c) {
		c.physicsUpdatesPerTick = physicsUpdatesPerTick;
		c.maxCorpses = maxCorpses;
		c.particleDensity = particleDensity;
		c.weatherAttemptsPerTick = weatherAttemptsPerTick;
	}
}
