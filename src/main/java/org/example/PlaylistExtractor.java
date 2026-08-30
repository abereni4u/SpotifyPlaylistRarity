package org.example;



import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.util.List;

import javax.swing.*;


import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import javax.swing.*;


public class PlaylistExtractor {

    private static final String CSV_EXT = ".csv";
    private static final String ID_SEPARATOR = " - ";

    public static class CSVPlaylist{
        ArrayList<Track> trackItems;

        public CSVPlaylist(ArrayList<Track> trackItems){
            this.trackItems = trackItems;
        }
    }


    public static void main(String[] args) throws Exception {

        // Grab user playlist with input window
        String userPlaylist = promptForPlaylist();
        if (userPlaylist == null){
            System.out.println("Cancelled");
            return;
        }

        // everything downstream keys off the id, so resolve it before we bother authing
        String playlistId = SpotifyApi.extractPlaylistId(userPlaylist);
        if (playlistId == null){
            JOptionPane.showMessageDialog(null, "Couldn't read a playlist id out of that link",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // auth, then ask Spotify for the name (for the filename) and the snapshot (for staleness)
        SpotifyAuth.Tokens tokens = SpotifyAuth.getTokens();
        String accessToken = tokens.accessToken();

        SpotifyApi.PlaylistMeta meta = SpotifyApi.getPlaylistMeta(playlistId, accessToken);
        System.out.println("Playlist: " + meta.name() + " (" + playlistId + ")");

        Path csvDir = AppPaths.csvDir();
        System.out.println("CSV directory: " + csvDir);

        CsvCache cache = new CsvCache(csvDir);
        Path csvFile = cache.current(playlistId, meta.snapshotId());

        if (csvFile != null){
            System.out.println("Skipping Chosic, using: " + csvFile.getFileName());
        }
        else {
            // a miss is either "never seen this playlist" or "seen it, but it's changed".
            // Only the first case should go looking for files we didn't write ourselves
            boolean seenBefore = cache.hasEntry(playlistId);
            cache.evict(playlistId);   // no-op when there was nothing recorded

            if (!seenBefore){
                csvFile = findOrphanCSV(csvDir, playlistId);

                if (csvFile == null){
                    csvFile = adoptFromDownloads(csvDir, meta.name(), playlistId);
                }
            }

            if (csvFile == null){
                System.out.println("No usable local CSV - exporting from Chosic");
                csvFile = getChosicCSV(csvDir, userPlaylist, meta.name(), playlistId);
            }

            if (csvFile == null){
                return;  // export failed, error dialog already shown by getChosicCSV
            }

            cache.record(playlistId, meta.snapshotId(), csvFile);
        }

        // this was being handed the Downloads *directory* before, which the reader would choke on
        ArrayList<Track> arrListTracks = csvParser(csvFile);
        System.out.println("Parsed " + arrListTracks.size() + " tracks");
    }

    // null means the user cancelled or closed the dialog, which is now distinct from entering
    // something invalid - a bad link re-prompts, cancel ends the run
    private static String promptForPlaylist(){

        while (true){
            String userPlaylist = JOptionPane.showInputDialog(null, "Please enter your Spotify playlist");

            if (userPlaylist == null){
                return null;
            }

            userPlaylist = userPlaylist.trim();

            if (userPlaylist.startsWith("https://open.spotify.com") || userPlaylist.startsWith("open.spotify.com")){
                return userPlaylist;
            }

            JOptionPane.showMessageDialog(null, "That doesn't look like a Spotify playlist link",
                    "Invalid link", JOptionPane.WARNING_MESSAGE);
        }
    }

    // canonical name for anything we save ourselves
    private static String cacheFileName(String playlistName, String playlistId){
        return sanitizeFileName(playlistName) + ID_SEPARATOR + playlistId + CSV_EXT;
    }

    /*
     * Scans the config dir for a CSV whose filename ends in this playlist's id but that the index
     * doesn't know about - files written by the version of this code that predates playlists.json,
     * or left behind if the index got deleted. Matching on the suffix rather than the whole
     * filename means the readable half can be renamed by hand without breaking the lookup.
     */
    private static Path findOrphanCSV(Path csvDir, String playlistId) throws IOException {

        String idSuffix = ID_SEPARATOR + playlistId + CSV_EXT;

        try (DirectoryStream<Path> csvFiles = Files.newDirectoryStream(csvDir, "*" + CSV_EXT)){
            for (Path csvFile : csvFiles){
                if (csvFile.getFileName().toString().endsWith(idSuffix)){
                    System.out.println("Found untracked export: " + csvFile.getFileName());
                    return csvFile;
                }
            }
        }
        return null;
    }

    /*
     * One-time sweep of Downloads for CSVs exported before the config dir move. Chosic names its
     * file after the playlist, so this is the only place we have to match on title. Both sides get
     * normalized (lowercased, punctuation stripped) so casing and "(1)" duplicate suffixes don't
     * break the match.
     *
     * startsWith rather than equals is what catches "Name (1).csv", but it also means a playlist
     * called "Rap" would match a stray "RapCaviar.csv" in Downloads. Copy, not move, so a wrong
     * guess is recoverable - and once the file is in the config dir this path never runs again
     * for that playlist.
     */
    private static Path adoptFromDownloads(Path csvDir, String playlistName, String playlistId) throws IOException {

        Path downloads = AppPaths.downloadsDir();
        if (!Files.isDirectory(downloads)){
            return null;
        }

        String targetName = normalize(playlistName);

        try (DirectoryStream<Path> csvFiles = Files.newDirectoryStream(downloads, "*" + CSV_EXT)){
            for (Path csvFile : csvFiles){

                String fileName = csvFile.getFileName().toString();
                String stem = fileName.substring(0, fileName.length() - CSV_EXT.length());

                if (normalize(stem).startsWith(targetName)){
                    Path destination = csvDir.resolve(cacheFileName(playlistName, playlistId));
                    Files.copy(csvFile, destination, StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Adopted " + fileName + " from Downloads");
                    return destination;
                }
            }
        }
        return null;
    }

    // strips everything that varies between how Spotify spells a playlist name and how it ends
    // up in a filename
    private static String normalize(String value){
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    // playlist names are user input, so they can contain characters Windows refuses in filenames
    private static String sanitizeFileName(String playlistName){

        String cleaned = playlistName.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        // windows also rejects names ending in a dot or a space
        while (cleaned.endsWith(".") || cleaned.endsWith(" ")){
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned.isBlank() ? "playlist" : cleaned;
    }

    public static ArrayList<Track> csvParser(Path csvFile) throws IOException {
        ArrayList<Track> csvTracks = new ArrayList<>();
        int skipped = 0;

        try (Reader reader = Files.newBufferedReader(csvFile)){
            // Configures format to automatically handle the first row as headers
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                try {
                    Track track = new Track.Builder()
                            .songTitle(textField(record, "Song"))
                            .artist(textField(record, "Artist"))
                            .BPM(intField(record, "BPM"))
                            .camelot(textField(record, "Camelot"))
                            .energy(intField(record, "Energy"))
                            .addedAt(textField(record, "Added At"))
                            .duration(textField(record, "Duration"))
                            .popularity(intField(record, "Popularity"))
                            .genres(genreField(record))
                            .album(textField(record, "Album"))
                            .albumDate(textField(record, "Album Date"))
                            .dance(intField(record, "Dance"))
                            .acoustic(intField(record, "Acoustic"))
                            .instrumental(intField(record, "Instrumental"))
                            .valence(intField(record, "Valence"))
                            .speech(intField(record, "Speech"))
                            .live(intField(record, "Live"))
                            .loud(intField(record, "Loud"))
                            .key(textField(record, "Key"))
                            .timeSignature(textField(record, "Time Signature"))
                            .spotifyID(textField(record, "Spotify Track Id"))
                            .build();
                    csvTracks.add(track);
                }
                catch (IllegalStateException e){
                    // Builder rejects rows with no title or no spotify id. Local files and
                    // region-blocked tracks land here - skip them rather than killing the parse
                    skipped++;
                    System.out.println("Skipping row " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
        }

        if (skipped > 0){
            System.out.println("Skipped " + skipped + " unusable row(s)");
        }
        return csvTracks;
    }

    // record.get throws when the column isn't in the header at all, so check before reading
    private static String textField(CSVRecord record, String column){

        if (!record.isMapped(column) || !record.isSet(column)){
            return "";
        }
        return record.get(column).trim();
    }

    /*
     * Chosic leaves numeric cells blank for local files and unavailable tracks, and occasionally
     * writes a decimal where the column is otherwise integral. parseInt died on both, taking the
     * whole playlist with it. Parse as a double and round, fall back to 0.
     *
     * 0 is a real value for these features, not a null, so if a track's obscurity score later
     * looks suspicious this is the first place to check.
     */
    private static int intField(CSVRecord record, String column){

        String value = textField(record, column);
        if (value.isEmpty()){
            return 0;
        }

        try {
            return (int) Math.round(Double.parseDouble(value));
        }
        catch (NumberFormatException e){
            return 0;
        }
    }

    // blank genre cell used to produce a String[] containing one empty string
    private static String[] genreField(CSVRecord record){

        String value = textField(record, "Genres");
        if (value.isEmpty()){
            return new String[0];
        }
        return value.split("\\s*,\\s*");
    }

    /*
     * Browser, context and playwright are all AutoCloseable, so try-with-resources closes them in
     * reverse order whether we return normally or throw. Previously a failure anywhere after
     * launch() leaked the chrome process. The inner finally guarantees the trace gets written on
     * failure too, which is the run you actually want a trace for.
     */
    public static Path getChosicCSV(Path csvDir, String userPlaylist, String playlistName, String playlistId) {

        try (Playwright playwright = Playwright.create();

             // launch browser with additional launch options to simulate real browser while headless
             Browser browser = playwright.chromium().launch(
                     new BrowserType.LaunchOptions()
                             .setChannel("chrome")
                             .setHeadless(true)
                             .setArgs(List.of(
                                     "--disable-blink-features=AutomationControlled",
                                     "--disable-features=IsolateOrigins,site-per-process",
                                     "--no-sandbox",
                                     "--disable-web-security",
                                     "--headless=new"
                             )));

             BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                     .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                     .setViewportSize(1920, 1080)
                     .setLocale("en-US")
                     .setAcceptDownloads(true)
                     .setTimezoneId("America/New_York"))) {

            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");

            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));

            try {
                Page page = context.newPage();

                System.out.println("Opening Chosic page...");

                page.navigate("https://www.chosic.com/spotify-playlist-exporter/");

                // insert playlist into page
                page.getByPlaceholder("Paste a Spotify playlist link").fill(userPlaylist);

                System.out.println("Inserting playlist...");

                // click button
                page.locator("#analyze").click();

                System.out.println("Waiting for playlist analysis...");

                // wait for playlist analyis to complete

                page.waitForTimeout(10000);
                PlaywrightAssertions.setDefaultAssertionTimeout(8000);

                assertThat(page.locator("#export")).isInViewport();
                page.locator("#export").click();

                System.out.println("Downloading...");

                Download download = page.waitForDownload(() -> {
                    page.getByText("Export to CSV").click();
                });

                // save into the config dir under our own name rather than whatever Chosic
                // suggested. This is what lets the cache locate it on the next run
                Path savedPath = csvDir.resolve(cacheFileName(playlistName, playlistId));
                download.saveAs(savedPath);

                System.out.println("Chosic suggested filename: " + download.suggestedFilename());
                System.out.println("CSV saved. Path: " + savedPath);

                return savedPath;
            }
            finally {
                // trace goes in the config dir too, instead of the working directory
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(csvDir.resolve("trace.zip")));
            }

        } catch (PlaywrightException e) {
            JOptionPane.showMessageDialog(null, "Couldn't Export Playlist"
                    + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

}