package com.skl.spoticraft.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skl.spoticraft.client.ui.widget.SpotifyPlaylistItemWidget;
import com.skl.spoticraft.spotify.SpotifyHandler;
import com.skl.spoticraft.spotify.SpotifyUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.enums.ProductType;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

import java.io.IOException;

public class SpotifyScreen extends Screen {
    private final boolean connected;
    private final Screen parent;
    public float percentageDone;
    public float progress;

    public SpotifyScreen(Screen parent) {
        super(Component.literal("Spotify"));
        this.parent = parent;
        this.connected = SpotifyHandler.SPOTIFY_API != null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    // In Minecraft 1.20.1, renderBackground accepts ONLY GuiGraphics
    @Override
    public void renderBackground(GuiGraphics context) {
        context.fill(0, 0, this.width, this.height, 0xFF121212);
    }

    public void triggerManualPoll() {
        new SpotifyHandler.PollingThread().run();
    }

    @Override
    protected void init() {
        super.init();

        // Back button
        this.addRenderableWidget(Button.builder(Component.literal("⟵ Back"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }).bounds(2, this.height - 22, 46, 20).build());

        // Album artwork & track title widget (centered horizontally at width / 2)
        this.addRenderableWidget(new SpotifyPlaylistItemWidget((this.width / 2) - 100, (this.height / 3) - 32));

        // Playback progress and Spotify status checks
        var currentlyPlaying = SpotifyUtils.NOW_PLAYING;
        if (currentlyPlaying != null && currentlyPlaying.getItem() != null) {
            this.progress = ((float) 0.5 * currentlyPlaying.getProgress_ms()) / ((float) 0.5 * currentlyPlaying.getItem().getDurationMs());
        }

        boolean isPremium = false;
        try {
            if (SpotifyHandler.SPOTIFY_API != null) {
                var profile = SpotifyHandler.SPOTIFY_API.getCurrentUsersProfile().build().execute();
                if (profile.getProduct() == ProductType.PREMIUM) {
                    isPremium = true;
                }
            }
        } catch (IOException | SpotifyWebApiException | ParseException ignored) {}

        // Info button
        this.addRenderableWidget(Button.builder(Component.literal("ℹ"), btn -> {
            Util.getPlatform().openUri("https://github.com/mineblock11/Spoticraft");
        }).bounds(2 + 46 + 2, this.height - 22, 20, 20).build());

        // Previous button
        Button previousWidget = Button.builder(Component.literal("←"), btn -> {
            btn.active = false;
            new Thread(() -> {
                try {
                    SpotifyHandler.SPOTIFY_API.skipUsersPlaybackToPreviousTrack().build().execute();
                    btn.active = true;
                    triggerManualPoll();
                } catch (IOException | ParseException | SpotifyWebApiException ignored) {}
            }).start();
        }).bounds(2 + 46 + 2 + 20 + 2, this.height - 22, 20, 20).build();
        previousWidget.active = isPremium;
        this.addRenderableWidget(previousWidget);

        // Play/Pause button
        Button pausePlayWidget = Button.builder(Component.literal("⏯"), btn -> {
            btn.active = false;
            new Thread(() -> {
                try {
                    var status = SpotifyHandler.SPOTIFY_API.getInformationAboutUsersCurrentPlayback().build().execute();
                    if (status.getIs_playing()) {
                        SpotifyHandler.SPOTIFY_API.pauseUsersPlayback().build().execute();
                    } else {
                        SpotifyHandler.SPOTIFY_API.startResumeUsersPlayback().build().execute();
                    }
                    btn.active = true;
                    triggerManualPoll();
                } catch (IOException | ParseException | SpotifyWebApiException ignored) {}
            }).start();
        }).bounds(2 + 46 + 2 + 20 + 2 + 20 + 2, this.height - 22, 20, 20).build();
        pausePlayWidget.active = isPremium;
        this.addRenderableWidget(pausePlayWidget);

        // Next button
        Button nextWidget = Button.builder(Component.literal("→"), btn -> {
            btn.active = false;
            new Thread(() -> {
                try {
                    SpotifyHandler.SPOTIFY_API.skipUsersPlaybackToNextTrack().build().execute();
                    btn.active = true;
                    triggerManualPoll();
                } catch (IOException | ParseException | SpotifyWebApiException ignored) {}
            }).start();
        }).bounds(2 + 46 + 2 + 20 + 2 + 20 + 2 + 20 + 2, this.height - 22, 20, 20).build();
        nextWidget.active = isPremium;
        this.addRenderableWidget(nextWidget);

        // Shuffle button
        Button shuffleWidget = Button.builder(Component.literal("S"), btn -> {
            btn.active = false;
            new Thread(() -> {
                try {
                    var playbackStatus = SpotifyHandler.SPOTIFY_API.getInformationAboutUsersCurrentPlayback().build().execute();
                    boolean shuffleState = playbackStatus.getShuffle_state();
                    SpotifyHandler.SPOTIFY_API.toggleShuffleForUsersPlayback(!shuffleState).build().execute();
                    btn.setMessage(shuffleState ? Component.literal("S") : Component.literal("S").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
                    btn.active = true;
                    triggerManualPoll();
                } catch (IOException | ParseException | SpotifyWebApiException ignored) {}
            }).start();
        }).bounds(this.width - 22, this.height - 22, 20, 20).build();
        shuffleWidget.active = isPremium;
        this.addRenderableWidget(shuffleWidget);

        // Repeat / Loop button
        Button loopWidget = Button.builder(Component.literal("R"), btn -> {
            btn.active = false;
            new Thread(() -> {
                try {
                    var playbackStatus = SpotifyHandler.SPOTIFY_API.getInformationAboutUsersCurrentPlayback().build().execute();
                    var repeatState = playbackStatus.getRepeat_state();
                    switch (repeatState) {
                        case "context" -> {
                            SpotifyHandler.SPOTIFY_API.setRepeatModeOnUsersPlayback("off").build().execute();
                            btn.setMessage(Component.literal("R"));
                        }
                        case "track" -> {
                            SpotifyHandler.SPOTIFY_API.setRepeatModeOnUsersPlayback("context").build().execute();
                            btn.setMessage(Component.literal("R").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
                        }
                        case "off" -> {
                            SpotifyHandler.SPOTIFY_API.setRepeatModeOnUsersPlayback("track").build().execute();
                            btn.setMessage(Component.literal("R").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                        }
                    }
                    btn.active = true;
                    triggerManualPoll();
                } catch (IOException | ParseException | SpotifyWebApiException ignored) {}
            }).start();
        }).bounds(this.width - 44, this.height - 22, 20, 20).build();
        loopWidget.active = isPremium;
        this.addRenderableWidget(loopWidget);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        if (!this.connected) {
            Component text = Component.literal("No Spotify Connection").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            int textWidth = this.font.width(text);
            context.drawString(this.font, text, (this.width / 2) - (textWidth / 2), (this.height / 2) - 4, 0xFFFF0000, false);
            return;
        }

        renderProgressBar(context);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        context.blit(ResourceLocation.tryParse("spoticraft:textures/spotify-long.png"), this.width - 2 - 128 / 2, 4, 64, 18, 0, 0, 312, 92, 312, 92);
        context.blit(ResourceLocation.tryParse("spoticraft:textures/spotify.png"), this.width - 2 - 128 / 2 - 18, 4, 16, 16, 0, 0, 32, 32, 32, 32);

        RenderSystem.disableBlend();
    }

    private void renderProgressBar(GuiGraphics context) {
        float playbackBarWidth = this.width - 46;
        int startX = 2 + 46 + 2 + 20 + 2 + 20 + 2 + 20 + 2 + 20 + 2;
        context.fill(startX, this.height - 19, (int) playbackBarWidth, this.height - 4, 0xFF5E5E5E);

        this.percentageDone = Mth.clamp(this.percentageDone * 0.95F + this.progress * 0.050000012F, 0.0F, 1.0F);

        if (this.percentageDone <= 0) {
            return;
        }

        float filledWidth = startX + Mth.ceil(((this.width - 46) - startX - 2) * this.percentageDone);
        context.fill(startX, this.height - 19, (int) filledWidth, this.height - 4, 0xFFFFFFFF);
    }
}