package org.example;



import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;


public class PlaylistExtractor {

    public static class csvPlaylist{
        ArrayList<Track> trackItems;

        public csvPlaylist(ArrayList<Track> trackItems){
            this.trackItems = trackItems;
        }
    }


    public static void main(String[] args)  {
        // User will get playlist CSV from chosic.com

        // Launch broswer with page, start listener / watchService on downloads folder
        Desktop.getDesktop().browse();
        // User places spotify link and downloads CSV
        // program watches download folder for CSV and parsing begins
        // Parse CSV here
        // Use Reccobeats and track IDs to create track objects for each playlist item
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

    public static


}
