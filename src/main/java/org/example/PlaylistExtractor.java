package org.example;



import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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


public class PlaylistExtractor {

    public static class CSVPlaylist{
        ArrayList<Track> trackItems;

        public CSVPlaylist(ArrayList<Track> trackItems){
            this.trackItems = trackItems;
        }
    }


    public static void main(String[] args) throws IOException {
        // User will get playlist CSV from chosic.com

        // Grab user playlist with input window
        String userPlaylist = JOptionPane.showInputDialog(null, "Please enter your Spotify playlist");
        while ( (userPlaylist == null) || !(userPlaylist.startsWith("https://open.spotify.com") || userPlaylist.startsWith("open.spotify.com")) ){
            userPlaylist = JOptionPane.showInputDialog(null, "Please enter your Spotify playlist");
        }

        // get downloads path

        Path downloads = getDownloadPath();

        // get chosic CSV using user entered playlist link

        Path csvFile = getChosicCSV(downloads, userPlaylist);

        // Parse CSV here
            // Playlist Object
                // Array list of Track Objects
            // Track Objects
                // Song Title
                // Artist
                // BPM
                // Camelot
                // Energy
                // Duration
                // Popularity
                // Genres
                // Album
                // Features ~ Dance, Acoustic, Instrumental, Valence, Speech, Live, Loud, Key, Signature
                // Track ID
                // --- containing audio features, and the title

        ArrayList<Track> arrListTracks = new ArrayList<>();

        try(Reader reader = new FileReader(csvFile.toFile())){
            // Configures format to automatically handle the first row as headers
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for(CSVRecord record : records){
               Track track = new Track.Builder()
                       .songTitle(record.get("Song"))
                       .artist(record.get("Artist"))
                       .BPM(Integer.parseInt(record.get("BPM")))
                       .camelot(record.get("Camelot"))
                       .energy(Integer.parseInt(record.get("Energy")))
                       .addedAt(record.get("Added At"))
                       .duration(record.get("Duration"))
                       .popularity(Integer.parseInt(record.get("Popularity")))
                       .genres(record.get("Genres").split(","))
                       .album(record.get("Album"))
                       .albumDate(record.get("Album Date"))
                       .dance(Integer.parseInt(record.get("Dance")))
                       .acoustic(Integer.parseInt(record.get("Acoustic")))
                       .instrumental(Integer.parseInt(record.get("Instrumental")))
                       .valence(Integer.parseInt(record.get("Valence")))
                       .speech(Integer.parseInt(record.get("Speech")))
                       .live(Integer.parseInt(record.get("Live")))
                       .loud(Integer.parseInt(record.get("Loud")))
                       .key(record.get("Key"))
                       .timeSignature(record.get("Time Signature"))
                       .spotifyID(record.get("Spotify Track Id"))
                       .build();
               arrListTracks.add(track);
            }

        }
        // Public method for converting the playlist into a group of objects with features
    }

    private static Path getDownloadPath(){
        String userHome = System.getProperty("user.home");

        return Paths.get(userHome, "Downloads");
    }

    public static Path getChosicCSV(Path downloads, String userPlaylist) {

        try (Playwright playwright = Playwright.create()) {

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
                        ))
            );

            // create browser context

            BrowserContext context =
                    browser.newContext(new Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                            .setViewportSize(1920, 1080)
                            .setLocale("en-US")
                            .setTimezoneId("America/New_York"));

            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");

            // open chosic page

            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));

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

            //Locator element = page.locator("#export");
            //element.waitFor(new Locator.WaitForOptions()
            //        .setState(WaitForSelectorState.ATTACHED)
            //        .setTimeout(60_000));
            //// click export csv button

            System.out.println("Downloading...");

            Download download = page.waitForDownload(() -> {
                page.getByText("Export to CSV").click();
            });

            Path savedPath = downloads.resolve(download.suggestedFilename());
            download.saveAs(savedPath);

            System.out.println("CSV downloaded. Path: " + savedPath.toString());

            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("trace.zip")));

            // close browser:
            browser.close();

            return savedPath;

        } catch (PlaywrightException e) {
            JOptionPane.showMessageDialog(null, "Couldn't Export Playlist"
                    + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

//            WatchService watchService = FileSystems.getDefault().newWatchService();
//
//            downloads.register(watchService,
//                    StandardWatchEventKinds.ENTRY_CREATE);

//            while(true){
//                WatchKey key = watchService.take();
//
//
//                for(WatchEvent<?> event : key.pollEvents()){
//                    Path filePath = ((Path)event.context());
//                    String fileName = filePath.getFileName().toString();
//
//                    if(fileName.toLowerCase().endsWith(".csv")) {
//                        System.out.println("CSV downloaded");
//                        return filePath;
//                    }
//                    key.reset();
//                }
//            }

    }

}