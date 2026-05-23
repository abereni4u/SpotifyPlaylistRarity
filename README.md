# Spotify Playlist Rarity
A Java program that estimates how likely you are to enjoy the songs in a given Spotify playlist based on your listening history.
Part of a larger hardware project that'll need this functionality.

## Tech stack

- Java 23
- Maven
- Spotify Web API
- Jackson (JSON parsing)
- `java.net.http.HttpClient` for HTTP requests
- `com.sun.net.httpserver.HttpServer` for the OAuth callback

## Status

Currently working:
- OAuth Authorization Code Flow (browser-based login + local callback server)
- Token exchange and access token retrieval

In progress:
- Playlist track retrieval
- Audio feature analysis
- Likelihood scoring model
