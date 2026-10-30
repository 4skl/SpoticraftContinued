package com.skl.spoticraft.client;

import com.skl.spoticraft.client.config.SpoticraftConfig;
import com.skl.spoticraft.client.ui.SpotifyScreen;
import com.skl.spoticraft.client.ui.widget.SpotifyButtonWidget;
import com.skl.spoticraft.spotify.SpotifyHandler;
import com.skl.spoticraft.spotify.SpotifyUtils;
import com.skl.spoticraft.utils.LiveWriteProperties;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ScreenEvent;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Mod(SpoticraftClient.MODID)
public class SpoticraftClient {

    public static final String MODID = "spoticraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("Spoticraft");
    public static final String VERSION = "1.1.0";
    public static final LiveWriteProperties SPOTIFY_CONFIG = new LiveWriteProperties();

    public static final KeyMapping OPEN_KEY = new KeyMapping(
            "key.spotify.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.spotify.main"
    );

    public SpoticraftClient(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the client configuration directly via the injected context
        context.registerConfig(ModConfig.Type.CLIENT, SpoticraftConfig.SPEC);

        // Mod bus listeners
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerKeyMappings);

        // Game bus listeners
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            boolean reset = SpoticraftConfig.CLIENT.resetSpotifyCredentials.get();
            if (reset) {
                SpoticraftConfig.CLIENT.resetSpotifyCredentials.set(false);
                SpoticraftConfig.SPEC.save();
            }

            SpotifyHandler.setup(reset);

            SpotifyHandler.PollingThread thread = new SpotifyHandler.PollingThread();
            ExecutorService checkTasksExecutorService = new ThreadPoolExecutor(
                    1, 10,
                    100000, TimeUnit.MILLISECONDS,
                    new SynchronousQueue<>()
            );
            checkTasksExecutorService.execute(thread);
            SpotifyHandler.songChangeEvent.add(SpotifyUtils::run);
        });
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            SpotifyUtils.MC_LOADED = true;

            int l = titleScreen.height / 4 + 48;
            event.addListener(new SpotifyButtonWidget(
                    titleScreen.width / 2 + 129,
                    l + 96,
                    20,
                    20,
                    Component.empty(),
                    btn -> Minecraft.getInstance().setScreen(new SpotifyScreen(titleScreen))
            ));
        }
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft client = Minecraft.getInstance();
            while (OPEN_KEY.consumeClick()) {
                client.setScreen(new SpotifyScreen(client.screen));
            }

            // Handles the auto-mute music callback formerly in SpoticraftConfigModel
            if (SpoticraftConfig.CLIENT.autoMuteIngameMusic.get()
                    && SpotifyUtils.NOW_PLAYING != null
                    && SpotifyUtils.NOW_PLAYING.getIs_playing()) {
                client.getSoundManager().stop(null, SoundSource.MUSIC);
            }
        }
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() != null && event.getSound().getSource() == SoundSource.MUSIC) {
            if (SpoticraftConfig.CLIENT.autoMuteIngameMusic.get()
                    && SpotifyUtils.NOW_PLAYING != null
                    && SpotifyUtils.NOW_PLAYING.getIs_playing()) {
                event.setSound(null); // Cancels the sound in Forge
            }
        }
    }
}