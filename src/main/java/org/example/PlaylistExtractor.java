package org.example;



import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import com.microsoft.playwright.*;


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

        // Launch broswer with page, start listener / watchService on downloads folder
        Desktop.getDesktop().browse(URI.create("www.chosic.com/spotify-playlist-exporter/"));

        // get downloads path

        Path downloads = getDownloadPath();

        // keep watching download directory for file with CSV. prints "CSV download" upon success

        Path csvFile = getChosicCSV();



        // Create a directory watcher and wait for the existence of a file


        // While waiting for existence of file keep browser open
        // User places spotify link and downloads CSV
        // Once CSV is downloaded, browser closes and parsing begins
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

        Path downloadsPath = Paths.get(userHome, "Downloads");

        return downloadsPath;
    }

    public static Path getChosicCSV(Path downloads){

        try (Playwright playwright = Playwright.create()){

            // launch browser
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false)
            );

            // create browser context

            BrowserContext context = browser.newContext();

            // open chosic page

            Page page = context.newPage();

            page.navigate("https://www.chosic.com/spotify-playlist-exporter/");

            // insert playlist into page
            page.getByPlaceholder("Paste a Spotify playlist link").fill()

            System.out.println("Opened browser window, please enter your playlist");

            WatchService watchService = FileSystems.getDefault().newWatchService();

            downloads.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE);

            while(true){
                WatchKey key = watchService.take();
                for(WatchEvent<?> event : key.pollEvents()){
                    Path filePath = ((Path)event.context());
                    String fileName = filePath.getFileName().toString();

                    if(fileName.toLowerCase().endsWith(".csv")) {
                        System.out.println("CSV downloaded");
                        return filePath;
                    }
                    key.reset();
                }
            }

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }


    }


}