package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SpotifyApi {

    private static final String PLAYLIST_PATH = "playlist/";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // snapshotId changes whenever the playlist is modified - it's what the CSV cache compares on
    public record PlaylistMeta(String name, String snapshotId){}

    public static JsonNode getRequest(String getEndpoint, String token) throws IOException, InterruptedException {

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(getEndpoint))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> httpResponse = HTTP_CLIENT.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonNode responseBody = MAPPER.readTree(httpResponse.body());

        // fail loudly and with Spotify's own message rather than returning an error body
        // that the caller will try to read fields off
        if (httpResponse.statusCode() != 200){
            String message = responseBody.path("error").path("message").asText("unknown error");
            throw new IOException("Spotify API " + httpResponse.statusCode() + ": " + message);
        }
        return responseBody;
    }

    public static JsonNode getUserProfile(String token) throws IOException, InterruptedException {
        return getRequest("https://api.spotify.com/v1/me", token);
    }

    // the fields parameter keeps Spotify from serializing the entire track list back at us just
    // to read two strings
    public static PlaylistMeta getPlaylistMeta(String playlistId, String token) throws IOException, InterruptedException {

        JsonNode playlist = getRequest(
                "https://api.spotify.com/v1/playlists/" + playlistId + "?fields=name,snapshot_id",
                token
        );

        return new PlaylistMeta(
                playlist.path("name").asText(),
                playlist.path("snapshot_id").asText()
        );
    }

    /*
     * Pulls the id out of any of these shapes:
     *   https://open.spotify.com/playlist/37i9dQZF1DX?si=abc123
     *   https://open.spotify.com/intl-de/playlist/37i9dQZF1DX
     *   open.spotify.com/playlist/37i9dQZF1DX/
     * Returns null when there's no playlist segment, so the caller can bail before authing.
     */
    public static String extractPlaylistId(String playlistUrl){

        if (playlistUrl == null){
            return null;
        }

        String cleaned = playlistUrl.trim();

        // drop the ?si= share token and any trailing slash
        int queryIdx = cleaned.indexOf('?');
        if (queryIdx > 0){
            cleaned = cleaned.substring(0, queryIdx);
        }
        while (cleaned.endsWith("/")){
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        // lastIndexOf handles the /intl-xx/ locale prefix Spotify sometimes injects
        int playlistIdx = cleaned.lastIndexOf(PLAYLIST_PATH);
        if (playlistIdx < 0){
            return null;
        }

        String playlistId = cleaned.substring(playlistIdx + PLAYLIST_PATH.length());
        return playlistId.isBlank() ? null : playlistId;
    }
}
