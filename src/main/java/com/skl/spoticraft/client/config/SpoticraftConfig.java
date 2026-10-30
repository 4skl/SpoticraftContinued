package com.skl.spoticraft.client.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class SpoticraftConfig {

    public static final ForgeConfigSpec SPEC;
    public static final SpoticraftConfig CLIENT;

    public final ForgeConfigSpec.BooleanValue autoMuteIngameMusic;
    public final ForgeConfigSpec.BooleanValue resetSpotifyCredentials;

    static {
        Pair<SpoticraftConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(SpoticraftConfig::new);
        CLIENT = pair.getLeft();
        SPEC = pair.getRight();
    }

    private SpoticraftConfig(ForgeConfigSpec.Builder builder) {
        builder.push("general");

        autoMuteIngameMusic = builder
                .comment("Automatically mute in-game music when Spotify is playing")
                .define("autoMuteIngameMusic", false);

        resetSpotifyCredentials = builder
                .comment("Reset Spotify credentials on the next startup")
                .define("resetSpotifyCredentials", false);

        builder.pop();
    }
}