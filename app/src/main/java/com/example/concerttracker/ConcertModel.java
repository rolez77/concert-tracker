package com.example.concerttracker;

public class ConcertModel {
    private String artist;
    private String date;
    private String venue;
    private int imageResId;
    private String imageUri; // picture picked from the phone (null if none)

    public ConcertModel(String artist, String date, String venue, int imageResId) {
        this(artist, date, venue, imageResId, null);
    }

    public ConcertModel(String artist, String date, String venue, int imageResId, String imageUri) {
        this.artist = artist;
        this.date = date;
        this.venue = venue;
        this.imageResId = imageResId;
        this.imageUri = imageUri;
    }

    public String getArtist() {
        return artist;
    }

    public String getDate() {
        return date;
    }

    public String getVenue() {
        return venue;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getImageUri() {
        return imageUri;
    }
}
