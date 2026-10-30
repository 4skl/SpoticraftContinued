package com.skl.spoticraft.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Episode;
import se.michaelthelin.spotify.model_objects.specification.Track;

import static com.skl.spoticraft.spotify.SpotifyUtils.NOW_ART;
import static com.skl.spoticraft.spotify.SpotifyUtils.NOW_ID;

public class SpotifyToast implements Toast {
    private long startTime;
    private boolean justUpdated;
    public final CurrentlyPlaying currentlyPlaying;
    public SpotifyToast(CurrentlyPlaying currentlyPlaying) {
        this.currentlyPlaying = currentlyPlaying;
    }

    @Override
    public Visibility render(GuiGraphics context, ToastComponent manager, long startTime) {
        // 2,2 -> 35,35 image;
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }

        context.fill(0, 0, this.width(), this.height(), 0xFF191414);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();

        context.blit(NOW_ID, 2, 2, this.height() - 4, this.height() - 4, 0, 0, NOW_ART.getWidth(), NOW_ART.getHeight(), NOW_ART.getWidth(), NOW_ART.getHeight());
        context.blit(ResourceLocation.tryParse("spoticraft:textures/spotify.png"), this.width() - (16+8), (this.height() / 2)-8, 16, 16, 0, 0, 32, 32, 32, 32);

        RenderSystem.disableBlend();

        context.drawString(manager.getMinecraft().font, Component.literal(currentlyPlaying.getItem().getName()), 43, 10, -256, false);

        if(currentlyPlaying.getItem() instanceof Track track) {
            context.drawString(manager.getMinecraft().font,Component.literal(track.getArtists()[0].getName()), 43, 21, -1, false);
        } else {
            context.drawString(manager.getMinecraft().font, Component.literal(((Episode) currentlyPlaying.getItem()).getShow().getName()), 43, 21, -1, false);
        }

        return startTime - this.startTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public Object getToken() {
        return Toast.super.getToken();
    }

    @Override
    public int width() {
        int widthName = (int) (Minecraft.getInstance().font.width(currentlyPlaying.getItem().getName()) * (0.75));
        int widthArtist = 0;

        if(currentlyPlaying.getItem() instanceof Track track) {
            widthArtist = (int) (Minecraft.getInstance().font.width(Component.literal(track.getArtists()[0].getName())) * (0.75));
        } else {
            widthArtist = (int) (Minecraft.getInstance().font.width(Component.literal(((Episode) currentlyPlaying.getItem()).getShow().getName())) * (0.75));
        }

        return 160 + Math.max(widthArtist, widthName);
    }

    @Override
    public int height() {
        return 38;
    }
}
