package com.skl.spoticraft.spotify.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.skl.spoticraft.client.SpoticraftClient;
import com.skl.spoticraft.spotify.SpotifyHandler;
import com.skl.spoticraft.spotify.SpotifyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import static com.skl.spoticraft.client.SpoticraftClient.LOGGER;

public class CallbackHandler implements HttpHandler {
    private final HttpServer server;

    public CallbackHandler(HttpServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        LOGGER.info(requestMethod + " " + exchange.getRequestURI());
        if (requestMethod.equalsIgnoreCase("GET")) {
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, 0);
            try(OutputStream responseBody = exchange.getResponseBody(); InputStream page = SpotifyUtils.loadHTMLFile("callback")) {
                IOUtils.copy(page, responseBody);
            }

            LOGGER.info("Captured callback. Parsing oauth2 code.");

            var queries = SpotifyUtils.queryToMap(exchange.getRequestURI().getQuery());
            var code = queries.get("code");
            LOGGER.info(code);

            if (SpotifyHandler.SPOTIFY_API == null) {
                String clientId = SpoticraftClient.SPOTIFY_CONFIG.getProperty("client-id");
                String clientSecret = SpoticraftClient.SPOTIFY_CONFIG.getProperty("client-secret");

                if (clientId != null && clientSecret != null) {
                    SpotifyHandler.SPOTIFY_API = new SpotifyApi.Builder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setRedirectUri(URI.create("http://127.0.0.1:23435/callback"))
                            .build();
                } else {
                    SpoticraftClient.LOGGER.error("Spotify instance is null and no credentials were saved. Redirecting to setup.");
                    exchange.getResponseHeaders().set("Location", "http://127.0.0.1:23435/setup");
                    exchange.sendResponseHeaders(302, -1);
                    return;
                }
            }

            AuthorizationCodeCredentials credentials = null;
            try {
                LOGGER.info("Marking credentials.");
                credentials = SpotifyHandler.SPOTIFY_API.authorizationCode(code).build().execute();
                LOGGER.info("Marked credentials.");
            } catch (SpotifyWebApiException | ParseException e) {
                LOGGER.info("Failed to setup Spotify - " + e);
                System.exit(1);
            }

            LOGGER.info("Setting credentials to spotify api instance.");
            SpotifyHandler.SPOTIFY_API.setAccessToken(credentials.getAccessToken());
            SpotifyHandler.SPOTIFY_API.setRefreshToken(credentials.getRefreshToken());

            LOGGER.info("Saving credentials.");
            SpoticraftClient.SPOTIFY_CONFIG.put("token", credentials.getAccessToken());
            SpoticraftClient.SPOTIFY_CONFIG.put("refresh-token", credentials.getRefreshToken());
            SpoticraftClient.SPOTIFY_CONFIG.put("version", SpoticraftClient.VERSION);
            LOGGER.info("Saved Credentials.");

            server.stop(15);
            LOGGER.info("Stopping server in 15 max-secs");
        } else {
            new NotImplementedHandler().handle(exchange);
        }
    }
}
