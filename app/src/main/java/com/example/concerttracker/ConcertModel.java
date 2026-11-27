package com.example.concerttracker;

public class ConcertModel {
    private final long id;
    private final String artist, date, venue, imageUri, spotifyArtistId;
    public ConcertModel(long id, String artist, String date, String venue, String imageUri) {
        this(id,artist,date,venue,imageUri,null);
    }
    public ConcertModel(long id, String artist, String date, String venue, String imageUri, String spotifyArtistId) {
        this.spotifyArtistId=spotifyArtistId;
        this.id = id; this.artist = artist; this.date = date; this.venue = venue; this.imageUri = imageUri;
    }
    public long getId() { return id; }
    public String getArtist() { return artist; }
    // Dates are stored in YYYY-MM-DD format so sorting works across years.
    public String getDate() { return date; }
    public String getVenue() { return venue; }
    public int getImageResId() { return android.R.drawable.ic_menu_camera; }
    public String getSpotifyArtistId() { return spotifyArtistId; }
    public String getImageUri() { return imageUri; }
}
