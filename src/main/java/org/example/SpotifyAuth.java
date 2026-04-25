package org.example;

// -- Authorization -- //
import com.sun.net.httpserver.HttpServer;

import java.net.*;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// -- Local Server -- //
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

        myServer.createContext("/callback", exchange -> {
            // parse query string from redirect URL
            String query = exchange.getRequestURI().getQuery(); // everything after /callback?
            Map<String, String> params = parseQuery(query);

            // check that state matches what we sent
            if( !(params.get("state").equals(state))){
                String response = "State mismatch!";
                exchange.sendResponseHeaders(400, response.length()); //400 = bad request
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                codeFuture.completeExceptionally(new SecurityException("State mismatch"));
                return;
            }

            // check if user denied authorization
            if (params.containsKey("error")){
                String response = "Authorization denied:" + params.get("error");
                exchange.sendResponseHeaders(400, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                codeFuture.completeExceptionally(new SecurityException("State mismatch"));
                return;
            }



        }


    }

    private static Map<String, String> parseQuery(String query){
        Map<String, String> params = new HashMap<>();

        if ( query == null){
            return params;
        }

        for(String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) { // looking for [ code=randomnumbershere ] , 0 means no key so skip
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
    }
}
