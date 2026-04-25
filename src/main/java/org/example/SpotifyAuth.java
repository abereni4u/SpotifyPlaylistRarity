package org.example;

// -- Authorization -- //
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.awt.Desktop;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// -- Local Server -- //
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SpotifyAuth {
    private static final String CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID");
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String SCOPE = "playlist-read-private playlist-read-collaborative";

    public static void main(String[] args) throws Exception{
        // random state string that protects against cross site request forgery
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);

        // urlEncoder so that the encoding is URL safe.
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Build URL to access authorize endpoint.
        // URLEncoder.encode handles special chars, makes each query parameter URL safe

        String authUrl = "https://accounts.spotify.com/authorize?"
                + "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        System.out.println("Opening browser for authorization...");

        // using awt desktop to open a browser, login the user, and retrieve code for
        // access token
        Desktop.getDesktop().browse(URI.create(authUrl));

        // using this to prevent main thread from finishing before server receives callback
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        HttpServer myServer = HttpServer.create(new InetSocketAddress(8888),0);

        myServer.create


    }
}
