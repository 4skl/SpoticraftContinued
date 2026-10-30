package com.skl.spoticraft.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skl.spoticraft.spotify.SpotifyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import se.michaelthelin.spotify.model_objects.specification.Episode;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class SpotifyPlaylistItemWidget extends AbstractWidget {

    public SpotifyPlaylistItemWidget(int x, int y) {
        super(x, y, 200, 64, Component.empty());
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (SpotifyUtils.NOW_PLAYING != null && SpotifyUtils.NOW_ART != null && SpotifyUtils.NOW_ID != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();

            int size = SpotifyUtils.NOW_ART.getWidth();
            // Blit album cover
            context.blit(SpotifyUtils.NOW_ID, this.getX(), this.getY(), 64, 64, 0.0F, 0.0F, size, size, size, size);

            RenderSystem.disableBlend();

            Font font = Minecraft.getInstance().font;

            // Track name (yellow)
            context.drawString(font, Component.literal(SpotifyUtils.NOW_PLAYING.getItem().getName()), this.getX() + 64 + 16, this.getY() + 32 + 4, 0xFFFFFF00, false);

            // Artist or show name (white)
            if (SpotifyUtils.NOW_PLAYING.getItem() instanceof Track track && track.getArtists().length > 0) {
                context.drawString(font, Component.literal(track.getArtists()[0].getName()), this.getX() + 64 + 16, this.getY() + 21 + 4, 0xFFFFFFFF, false);
            } else if (SpotifyUtils.NOW_PLAYING.getItem() instanceof Episode episode) {
                context.drawString(font, Component.literal(episode.getShow().getName()), this.getX() + 64 + 16, this.getY() + 21 + 4, 0xFFFFFFFF, false);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}