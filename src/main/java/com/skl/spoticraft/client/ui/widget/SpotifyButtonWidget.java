package com.skl.spoticraft.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SpotifyButtonWidget extends Button {

    private static final ResourceLocation SPOTIFY_ICON = ResourceLocation.tryParse("spoticraft:textures/spotify.png");

    public SpotifyButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        // Draw dark background inside the button bounds
        context.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, 0xFF000000);

        // Render Spotify icon
        RenderSystem.enableBlend();
        if (SPOTIFY_ICON != null) {
            context.blit(SPOTIFY_ICON, this.getX() + 2, this.getY() + 2, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        }
        RenderSystem.disableBlend();
    }
}