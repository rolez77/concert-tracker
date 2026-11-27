package com.example.concerttracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

final class Dates {
    private Dates() {}
    static String iso(int year, int monthZeroBased, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }
    static boolean valid(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setLenient(false);
            return f.format(f.parse(value)).equals(value);
        } catch (Exception e) { return false; }
    }
    static Calendar calendar(String iso) {
        Calendar c = Calendar.getInstance();
        if (valid(iso)) {
            c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7))-1,
                    Integer.parseInt(iso.substring(8, 10)));
        }
        return c;
    }
    static String display(String iso) {
        if (!valid(iso)) return "";
        return Integer.parseInt(iso.substring(5, 7)) + "/" + Integer.parseInt(iso.substring(8, 10)) + "/" + iso.substring(0, 4);
    }
    static boolean matches(String iso, String query) {
        String q = query.trim();
        String display = display(iso);
        if (iso.contains(q) || display.contains(q)) return true;
        if (!valid(iso)) return false;
        String shortDate = display.substring(0, display.length()-4) + iso.substring(2,4);
        String padded = iso.substring(5,7) + "/" + iso.substring(8,10) + "/" + iso.substring(0,4);
        return shortDate.contains(q) || padded.contains(q) || (padded.substring(0,6)+iso.substring(2,4)).contains(q);
    }
}
