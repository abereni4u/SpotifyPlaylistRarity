package org.example;

/*
 * -- Overview --
 * Tracks which CSV belongs to which playlist, and which *version* of that playlist it was
 * exported from. Backed by a playlists.json sitting next to the CSVs in the config dir:
 *
 *   {
 *     "37i9dQZF1DX" : { "file" : "Rap - 37i9dQZF1DX.csv",
 *                       "snapshot_id" : "MTY4Nz...",
 *                       "exported_at" : "2026-08-30T14:02:11Z" }
 *   }
 *
 * snapshot_id comes from the Spotify playlist object and changes every time the playlist is
 * modified. Comparing the recorded snapshot against the live one is what stops a cached CSV from
 * being served after you've added or removed songs.
 *
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class CsvCache {

    private static final String INDEX_FILE = "playlists.json";
    private final Path csvDir;
    private final Path indexFile;
    private final ObjectMapper mapper;
    private final ObjectNode index;

    public CsvCache(Path csvDir){
        this.csvDir = csvDir;
        this.indexFile = csvDir.resolve(INDEX_FILE);
        this.mapper = new ObjectMapper();
        this.index = readIndex();
    }

    private ObjectNode readIndex(){

        if (!Files.exists(indexFile)){
            return mapper.createObjectNode();
        }

        try {
            JsonNode parsed = mapper.readTree(indexFile.toFile());
            if (parsed instanceof ObjectNode indexNode){
                return indexNode;
            }
        }
        catch (IOException e){
            System.out.println("Cache index unreadable, starting a fresh one: " + e.getMessage());
        }
        return mapper.createObjectNode();
    }

    public boolean hasEntry(String playlistId){
        return index.has(playlistId);
    }

    /*
     * Returns the cached CSV only if all three hold:
     *   - we have an entry for this playlist
     *   - the file it points at still exists
     *   - the recorded snapshot matches what Spotify reports right now
     * Otherwise null, and the caller re-exports.
     */
    public Path current(String playlistId, String snapshotId){

        JsonNode entry = index.get(playlistId);
        if (entry == null){
            return null;
        }

        Path csvFile = csvDir.resolve(entry.path("file").asText());
        if (!Files.exists(csvFile)){
            System.out.println("Cached CSV is missing from disk - re-exporting");
            return null;
        }

        if (!entry.path("snapshot_id").asText().equals(snapshotId)){
            System.out.println("Playlist has changed since " + entry.path("exported_at").asText()
                    + " - re-exporting");
            return null;
        }

        return csvFile;
    }

    /*
     * Drops a stale export so the folder doesn't fill up with dead CSVs. Only the in-memory index
     * is updated here - if the re-export then fails, the on-disk index still points at a file that
     * no longer exists, and current() already treats that as a miss.
     */
    public void evict(String playlistId){

        JsonNode entry = index.remove(playlistId);
        if (entry == null){
            return;
        }

        Path staleFile = csvDir.resolve(entry.path("file").asText());
        try {
            if (Files.deleteIfExists(staleFile)){
                System.out.println("Removed stale export: " + staleFile.getFileName());
            }
        }
        catch (IOException e){
            // not fatal, the new export will overwrite it anyway if the name matches
            System.out.println("Couldn't delete stale export: " + e.getMessage());
        }
    }

    public void record(String playlistId, String snapshotId, Path csvFile) throws IOException {

        ObjectNode entry = mapper.createObjectNode();
        entry.put("file", csvFile.getFileName().toString());
        entry.put("snapshot_id", snapshotId);
        entry.put("exported_at", Instant.now().toString());

        index.set(playlistId, entry);

        mapper.writerWithDefaultPrettyPrinter().writeValue(indexFile.toFile(), index);
    }
}
