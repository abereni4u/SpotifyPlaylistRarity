# Spotify Playlist Rarity

A Java app that scores Spotify playlists based on how well their tracks match
your recent listening history. Part of a larger hardware project that needs this
piece working first.

## What it does

You paste a public Spotify playlist link. The app pulls the playlist's tracks
along with their audio features, compares them against your recent top tracks
from Spotify, and gives each song a rarity score. The idea is a lootbox-style
reveal. Most songs in a random playlist won't match your taste, but a few might
be unexpected hits.

## Tech stack

- Java 23 + Maven
- Spotify Web API (OAuth 2.0 Authorization Code Flow)
- Playwright for Java (browser automation for the CSV export step)
- Jackson (JSON parsing)
- `java.net.http.HttpClient` for HTTP requests
- `com.sun.net.httpserver.HttpServer` for the local OAuth callback
- Java Swing (`JOptionPane`) for input dialogs

## Notes on data sources

Spotify restricted a bunch of Web API endpoints in 2024 and 2026, including
audio features, popularity, and playlist contents for playlists the user
doesn't own. Working around that:

- Playlist tracks and their audio features come from a CSV export through
  Chosic's playlist exporter, automated with Playwright.
- User listening data (top tracks, profile) comes from Spotify's Web API
  directly, since that still works for the authenticated user's own data.

## Status

**Working:**
- OAuth Authorization Code Flow with a local callback server
- Token persistence with automatic refresh on expiry
- Cross-platform config directory (Windows / macOS / Linux)
- User profile retrieval
- Playlist CSV export

**Building:**
- CSV parsing into Track records
- Building the taste profile from the user's top tracks
- Per-track rarity scoring
- Bucket classification (Epic / Good / Okay / Meh)

**Planned:**
- Calibration loop for scoring thresholds
- Hardware integration