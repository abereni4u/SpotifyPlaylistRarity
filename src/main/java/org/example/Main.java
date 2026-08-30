package org.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {

    public static void main(String[] args) throws Exception{

        SpotifyAuth.Tokens tokens = SpotifyAuth.getTokens();
        String accessToken = tokens.accessToken();

        JsonNode userProfile = SpotifyApi.getUserProfile(accessToken);
        System.out.println("Logged in as: " + userProfile.get("display_name").asText());
        System.out.println("Email: " + userProfile.get("email").asText());

    }
}

