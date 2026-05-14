package org.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception{

        SpotifyAuth.Tokens tokens = SpotifyAuth.getTokens();
        String accessToken = tokens.accessToken();
        String refreshToken = tokens.refreshToken();

        JsonNode userProfile = getUserProfile(accessToken);
        System.out.println("Logged in as: " + userProfile.get("display_name").asText());
        System.out.println("Email: " + userProfile.get("email").asText());
    }


    private static JsonNode getRequest(String getEndpoint, String token) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri((URI.create(getEndpoint)))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> httpResponse = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(httpResponse.body());
    }

    private static JsonNode getUserProfile(String token) throws IOException, InterruptedException {
        return getRequest("https://api.spotify.com/v1/me", token);
    }
}