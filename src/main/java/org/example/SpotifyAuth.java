package org.example;

// -- Authorization -- //
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.awt.Desktop;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// -- Local Server -- //
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


// -- JSON Parsing -- //
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SpotifyAuth {

    private static final String CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID");
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String SCOPE = "playlist-read-private playlist-read-collaborative";

    public static void main(String[] args) throws Exception{

        // Used to verify requests are for and from this client
        String state = generateStateString();

        // Get user credentials (code) for access token. Redirects back to this page
        spotifyLogin(state);

        // using this to prevent main thread from finishing before server receives callback

        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        HttpServer myServer = HttpServer.create(new InetSocketAddress(8888),0);
        getAuthorizationCode(codeFuture, state, myServer);

        // block main from finishing until callback completed or fails

        String code = codeFuture.get();
        myServer.stop(0);
        System.out.println("Got authorization code: " + code);

        // make POST request to get access token

        String credentials = CLIENT_ID + ":" + System.getenv("SPOTIFY_CLIENT_SECRET");
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        // construct body of post request
        String body = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))  // endpoint to make POST request to
                .header("Authorization", "Basic " + encoded)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(
                tokenRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Token response: " + tokenResponse.body());

    }

    private static void getAuthorizationCode(CompletableFuture<String> codeFuture, String state, HttpServer myServer) throws IOException {

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

            String code = params.get("code");
            String response =
                    "<html><body><h1> Authorization successful! </h1>"
                            + "<p> You can close this tab and return to the app. </p></body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length()); // 200 = successful request
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();

            codeFuture.complete(code);

        });

        myServer.start();

        System.out.println("Listening on http://127.0.0.1:8888/callback");

    }

    private static void spotifyLogin(String state) throws IOException {

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

    }

    private static String generateStateString()
    {
        // random state string that protects against cross site request forgery
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);

        // urlEncoder so that string is URL safe and withoutPadding so that extra '=' characters are removed
        // (base64 encodes every 3 bytes into 4 chars so leftover bytes with 16 here)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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

        return params;
    }
}
