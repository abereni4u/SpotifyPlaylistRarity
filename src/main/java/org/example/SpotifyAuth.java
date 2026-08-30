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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

// -- Local Server -- //
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// -- JSON Parsing -- //
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SpotifyAuth {

    private static final String CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID");
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String SCOPE = "playlist-read-private playlist-read-collaborative user-read-email";

    private static Path tokenFile;

    public record Tokens(String accessToken, String refreshToken){}

    public static Tokens getTokens() throws IOException, InterruptedException, ExecutionException {

        ObjectMapper mapper = new ObjectMapper(); // Jackson json parsing starts with this

        // path now comes from AppPaths, shared with the CSV cache
        tokenFile = AppPaths.tokenFile();
        Files.createDirectories(tokenFile.getParent());

        if (Files.exists(tokenFile)){
            JsonNode tokenJson = mapper.readTree(tokenFile.toFile());

            Instant expiresAt = Instant.parse(tokenJson.get("expires_at").asText());

            if(Instant.now().isBefore(expiresAt)){
                System.out.println("Using existing access token");

                return new Tokens(tokenJson.get("access_token").asText(),
                        tokenJson.get("refresh_token").asText()
                );
            }
            else{
                System.out.println("Existing access token expired - REFRESHING");
                return refreshAccessToken(tokenJson.get("refresh_token").asText());
            }
        }
        else {
            System.out.println("No access token found - GENERATING");

            // Get authorizationCode

            String code = getAuthorizationCode();

            System.out.println("Successfully retrieved authorization code");

            // make POST request to get access token

            String encoded = getEncoding();

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
                    HttpResponse.BodyHandlers.ofString() // this determines how the response should be formatted
            );

            // json parsing starts here

            JsonNode tokenJson = mapper.readTree(tokenResponse.body()); // use mapper to create JSON tree to parse from

            writeTokensToFile(tokenJson);

            String accessToken = tokenJson.get("access_token").asText();
            String refreshToken = tokenJson.get("refresh_token").asText();

            return new Tokens(accessToken, refreshToken);
        }
    }

    private static String getAuthorizationCode() throws IOException, ExecutionException, InterruptedException {

        // Used to verify requests are for and from this client
        String state = generateStateString();

        // Get user credentials (code) for access token. Redirects back to this page
        spotifyLogin(state);

        CompletableFuture<String> codeFuture = new CompletableFuture<>();  // using this to prevent main thread from finishing before server receives callback
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

            String code = params.get("code");
            String response =
                    "<html><body><h1> Authorization successful! </h1>"
                            + "<p> You can close this tab and return to the app. </p></body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length()); // 200 = successful request
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();

            codeFuture.complete(code); // place STRING, code, into codeFuture so that main can go pass through.

        });

        myServer.start();
        System.out.println("Listening on http://127.0.0.1:8888/callback");
        // block from finishing until callback completed or fails
        String code = codeFuture.get();
        myServer.stop(0);
        return code;
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

        // using awt desktop to open a browser, login the user, and initiate callback request to
        // localhost
        Desktop.getDesktop().browse(URI.create(authUrl));

    }

    private static String generateStateString() {
        // random state string that protects against cross site request forgery
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);

        // urlEncoder so that string is URL safe and withoutPadding so that extra '=' characters are removed
        // (base64 encodes every 3 bytes into 4 chars so leftover bytes with 16 here)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static Map<String, String> parseQuery(String query){
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

    private static String getEncoding(){
        String credentials = CLIENT_ID + ":" + System.getenv("SPOTIFY_CLIENT_SECRET");
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private static Tokens refreshAccessToken(String refreshToken) throws IOException, InterruptedException {

        ObjectMapper mapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newHttpClient();
        String requestBody = "grant_type=refresh_token"
                + "&refresh_token=" + refreshToken;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + getEncoding())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofString());

        JsonNode tokenJson = mapper.readTree(httpResponse.body());

        // Spotify only returns a refresh_token sometimes, so carry the old one forward when it
        // doesn't.
        if (tokenJson.get("refresh_token") == null){
            ObjectNode objectJson = (ObjectNode) tokenJson;
            objectJson.put("refresh_token", refreshToken);
        }
        writeTokensToFile(tokenJson);

        return new Tokens(tokenJson.get("access_token").asText(),
                tokenJson.get("refresh_token").asText());
    }

    private static void writeTokensToFile(JsonNode tokenJson) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        // calculate when the token will expire
        int expiresIn = tokenJson.get("expires_in").asInt();
        Instant expiresAt = Instant.now().plusSeconds(expiresIn);

        // remove expires_in key from json Node and replace with expiresAt
        ObjectNode objectNode = (ObjectNode) tokenJson;
        objectNode.remove("expires_in");
        objectNode.put("expires_at", expiresAt.toString());

        // write jsonNode to file at tokenFile location
        mapper.writeValue(tokenFile.toFile(), tokenJson);
        mapper.writeValue(tokenFile.getParent().resolve("prettyTokens.json").toFile(),
                tokenJson);

    }

}
