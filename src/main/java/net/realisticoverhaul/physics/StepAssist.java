package net.realisticoverhaul.physics;

import net.realisticoverhaul.config.ROConfig;

/**
 * Step assist must match on both sides, or the server pulls the player back.
 * The server uses its own config; clients use the value the server sent them.
 */
public final class StepAssist {
	/** Set on the client from the server's settings packet. */
	public static volatile boolean serverAllows = false;

	private StepAssist() {}

	public static boolean active(boolean clientSide) {
		return clientSide ? serverAllows : ROConfig.get().stepAssist;
	}
}
