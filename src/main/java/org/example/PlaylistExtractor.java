package org.example;



import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.List;

import javax.swing.*;


public class PlaylistExtractor {

    public static class csvPlaylist{
        ArrayList<Track> trackItems;

        public csvPlaylist(ArrayList<Track> trackItems){
            this.trackItems = trackItems;
        }
    }

    public record Track(
            String songTitle,
            String artist,
            int BPM,
            String camelot,
            int energy,
            String addedAt,
            String duration,
            int popularity,
            String[] genres,
            String album,
            String albumDate,
            int dance,
            int acoustic,
            int instrumental,
            int valence,
            int speech,
            int live,
            int loud,
            String key,
            String timeSignature,
            String spotifyID,
            String explicit
    ){}

    public static void main(String[] args) throws IOException {
        // User will get playlist CSV from chosic.com

        // Grab user playlist with input window
        String userPlaylist = JOptionPane.showInputDialog(null, "Please enter your Spotify playlist");
        while ( (userPlaylist == null) || !(userPlaylist.startsWith("open.spotify.com")) ){
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
                        .setHeadless(true)
                        .setArgs(List.of(
                                "--disable-blink-features=AutomationControlled",
                                "--disable-features=IsolateOrigins,site-per-process",
                                "--no-sandbox",
                                "--disable-web-security"
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

            Page page = context.newPage();

            System.out.println("Opening Chosic page...");

            page.navigate("https://www.chosic.com/spotify-playlist-exporter/");

            // insert playlist into page
            page.getByPlaceholder("Paste a Spotify playlist link").fill(userPlaylist);

            System.out.println("Inserting playlist...");

            // click button
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Start")).click();

            System.out.println("Waiting for playlist analysis...");

            // wait for playlist analyis to complete
            Locator element = page.locator("#export");
            element.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(60_000));
            // click export csv button

            System.out.println("Downloading...");

            Download download = page.waitForDownload(() -> {
                page.getByText("Export to CSV").click();
            });

            Path savedPath = downloads.resolve(download.suggestedFilename());
            download.saveAs(savedPath);

            System.out.println("CSV downloaded. Path: " + savedPath.toString());

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