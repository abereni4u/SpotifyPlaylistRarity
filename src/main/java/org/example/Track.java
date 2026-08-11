package org.example;

public class Track {

    private final String songTitle;
    private final String artist;
    private final int BPM;
    private final String camelot;
    private final int energy;
    private final String addedAt;
    private final String duration;
    private final int popularity;
    private final String[] genres;
    private final String album;
    private final String albumDate;
    private final int dance;
    private final int acoustic;
    private final int instrumental;
    private final int valence;
    private final int speech;
    private final int live;
    private final int loud;
    private final String key;
    private final String timeSignature;
    private final String spotifyID;
    private final String explicit;


    public String getSongTitle() {
        return songTitle;
    }

    public String getArtist() {
        return artist;
    }

    public int getBPM() {
        return BPM;
    }

    public String getCamelot() {
        return camelot;
    }

    public int getEnergy() {
        return energy;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public String getDuration() {
        return duration;
    }

    public int getPopularity() {
        return popularity;
    }

    public String[] getGenres() {
        return genres;
    }

    public String getAlbum() {
        return album;
    }

    public String getAlbumDate() {
        return albumDate;
    }

    public int getDance() {
        return dance;
    }

    public int getAcoustic() {
        return acoustic;
    }

    public int getInstrumental() {
        return instrumental;
    }

    public int getValence() {
        return valence;
    }

    public int getSpeech() {
        return speech;
    }

    public int getLive() {
        return live;
    }

    public int getLoud() {
        return loud;
    }

    public String getKey() {
        return key;
    }

    public String getTimeSignature() {
        return timeSignature;
    }

    public String getSpotifyID() {
        return spotifyID;
    }

    public String getExplicit() {
        return explicit;
    }

    public Track(Builder b){
        this.songTitle = b.songTitle;
        this.artist = b.artist;
        this.BPM = b.BPM;
        this.camelot = b.camelot;
        this.energy = b.energy;
        this.addedAt = b.addedAt;
        this.duration = b.duration;
        this.popularity = b.popularity;
        this.genres = b.genres == null ? new String[0] : b.genres.clone();
        this.album = b.album;
        this.albumDate = b.albumDate;
        this.dance = b.dance;
        this.acoustic = b.acoustic;
        this.instrumental = b.instrumental;
        this.valence = b.valence;
        this.speech = b.speech;
        this.live = b.live;
        this.loud = b.loud;
        this.key = b.key;
        this.timeSignature = b.timeSignature;
        this.spotifyID = b.spotifyID;
        this.explicit = b.explicit;
    }

    public static class Builder{

        private String songTitle;
        private String artist;
        private int BPM;
        private String camelot;
        private int energy;
        private String addedAt;
        private String duration;
        private int popularity;
        private String[] genres;
        private String album;
        private String albumDate;
        private int dance;
        private int acoustic;
        private int instrumental;
        private int valence;
        private int speech;
        private int live;
        private int loud;
        private String key;
        private String timeSignature;
        private String spotifyID;
        private String explicit;

        public Builder songTitle(String songTitle) {
            this.songTitle = songTitle;
            return this;
        }

        public Builder artist(String artist) {
            this.artist = artist;
            return this;
        }

        public Builder BPM(int BPM) {
            this.BPM = BPM;
            return this;
        }

        public Builder camelot(String camelot) {
            this.camelot = camelot;
            return this;
        }

        public Builder energy(int energy) {
            this.energy = energy;
            return this;
        }

        public Builder addedAt(String addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public Builder popularity(int popularity) {
            this.popularity = popularity;
            return this;
        }

        public Builder genres(String[] genres) {
            this.genres = genres;
            return this;
        }

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder albumDate(String albumDate) {
            this.albumDate = albumDate;
            return this;
        }

        public Builder dance(int dance) {
            this.dance = dance;
            return this;
        }

        public Builder acoustic(int acoustic) {
            this.acoustic = acoustic;
            return this;
        }

        public Builder instrumental(int instrumental) {
            this.instrumental = instrumental;
            return this;
        }

        public Builder valence(int valence) {
            this.valence = valence;
            return this;
        }

        public Builder speech(int speech) {
            this.speech = speech;
            return this;
        }

        public Builder live(int live) {
            this.live = live;
            return this;
        }

        public Builder loud(int loud) {
            this.loud = loud;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder timeSignature(String timeSignature) {
            this.timeSignature = timeSignature;
            return this;
        }

        public Builder spotifyID(String spotifyID) {
            this.spotifyID = spotifyID;
            return this;
        }

        public Builder explicit(String explicit) {
            this.explicit = explicit;
            return this;
        }

        public Track build(){
            if(spotifyID == null || spotifyID.isBlank()){
                throw new IllegalStateException("spotifyID is required");
            }
            if(songTitle == null || songTitle.isBlank()){
                throw new IllegalStateException("songTitle is required");
            }
            return new Track(this);
        }

    }


}
