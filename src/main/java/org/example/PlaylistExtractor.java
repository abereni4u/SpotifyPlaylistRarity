package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlaylistExtractor {

    private static final String CHOSIC_PAGE = "https://www.chosic.com/spotify-playlist-exporter/";
    private static final String CHOSIC_TOKEN_URL = "https://www.chosic.com/api/tools/t/";

    public static void main(String[] args) throws IOException, InterruptedException {
        getChosicToken();
    }

    public static void getChosicToken() throws IOException, InterruptedException {
        HttpClient cookieGrabber = HttpClient.newBuilder()
                .cookieHandler(new CookieManager()).build();

        HttpRequest cookieRequest = HttpRequest.newBuilder()
                .uri(URI.create(CHOSIC_PAGE))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> cookieResponse = cookieGrabber.send(cookieRequest, HttpResponse.BodyHandlers.ofString());


        String postBody = "app=playlist_analyzer";

        cookieRequest = HttpRequest.newBuilder()
                .uri(URI.create(CHOSIC_TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", "https://www.chosic.com")
                .header("Referer", CHOSIC_PAGE)
                .header("X-Requested-With", "XMLHttpRequest")
                .POST(HttpRequest.BodyPublishers.ofString(postBody))
                .build();

        cookieResponse = cookieGrabber.send(cookieRequest, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jNode = mapper.readTree(cookieResponse.body());
        System.out.println(jNode.get("token").asText());
    }
}
