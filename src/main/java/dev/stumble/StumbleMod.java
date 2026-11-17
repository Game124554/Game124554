package dev.stumble;

import dev.stumble.server.ServerRagdolls;
import net.fabricmc.api.ModInitializer;

/** Runs on both sides: the server half of pushing and of knocked-down mobs. */
public final class StumbleMod implements ModInitializer {
    public static final String MOD_ID = "stumble";

    @Override
    public void onInitialize() {
        StumbleConfig.load();
        ServerRagdolls.register();
    }
}
