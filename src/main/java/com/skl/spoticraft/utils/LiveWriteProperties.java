package com.skl.spoticraft.utils;

import com.skl.spoticraft.client.SpoticraftClient;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public class LiveWriteProperties extends Properties {

    private final Path pathToConfig = FMLPaths.CONFIGDIR.get().resolve(SpoticraftClient.MODID).resolve("spotify.cred");
    public boolean empty = true;

    public LiveWriteProperties() {
        // Ensure parent directory (config/spoticraft/) exists
        try {
            if (pathToConfig.getParent() != null && !Files.exists(pathToConfig.getParent())) {
                Files.createDirectories(pathToConfig.getParent());
            }
        } catch (IOException e) {
            SpoticraftClient.LOGGER.error("Failed to create spoticraft config directory", e);
        }

        // Migrate old configs if present
        Path legacyPath = FMLPaths.CONFIGDIR.get().resolve("spotify.cred");
        if (Files.exists(legacyPath)) {
            SpoticraftClient.LOGGER.warn("Found legacy spotify credentials, migrating to new location");
            try {
                Files.move(legacyPath, pathToConfig);
            } catch (IOException e) {
                SpoticraftClient.LOGGER.error("Failed to migrate legacy spotify credentials", e);
                try {
                    Files.deleteIfExists(legacyPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Unable to delete legacy config after IO error, please manually delete the file at " + legacyPath, ex);
                }
            }
        }

        if (Files.exists(pathToConfig)) {
            try (var stream = Files.newInputStream(pathToConfig)) {
                this.load(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (this.getProperty("client-secret") == null || !Objects.equals(this.getProperty("version"), SpoticraftClient.VERSION)) {
                SpoticraftClient.LOGGER.warn("Old configuration file! Removing.");
                try {
                    Files.delete(pathToConfig);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.clear();
                this.setProperty("version", SpoticraftClient.VERSION);
            } else {
                empty = false;
            }
        }
    }

    public void markDirty() throws IOException {
        if (pathToConfig.getParent() != null && !Files.exists(pathToConfig.getParent())) {
            Files.createDirectories(pathToConfig.getParent());
        }
        try (var os = Files.newOutputStream(pathToConfig)) {
            this.store(os, null);
        }
    }
}