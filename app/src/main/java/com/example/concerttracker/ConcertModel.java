package com.example.concerttracker;

public class ConcertModel {
    private String artist;
    private String date;
    private String venue;
    private int imageResId;

    public ConcertModel(String artist, String date, String venue, int imageResId) {
        this.artist = artist;
        this.date = date;
        this.venue = venue;
        this.imageResId = imageResId;
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
}