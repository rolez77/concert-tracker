package com.example.concerttracker;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

/** Run on an emulator: checks persistence, account isolation, and cascading deletion. */
@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {
    @Test public void localAccountsAndConcerts() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        DatabaseHelper db=DatabaseHelper.get(ctx);
        String first="test_"+UUID.randomUUID(), second="test_"+UUID.randomUUID();
        long a=db.register(first,"sample-password"), b=db.register(second,"sample-password");
        assertTrue(a>0); assertTrue(b>0);
        try {
            assertEquals(-1,db.register(first,"sample-password"));
            assertEquals(a,db.login(first,"sample-password"));
            assertEquals(-1,db.login(first,"wrong-password"));
            long concert=db.saveConcert(a,-1,"Artist","2026-11-21","Venue",null,"1234567890123456789012");
            assertTrue(concert>0); assertNull(db.concert(b,concert));
            assertTrue(db.concerts(b).isEmpty());
            assertEquals(-1,db.addSong(b,concert,"Not allowed"));
            long s1=db.addSong(a,concert,"Song 1"),s2=db.addSong(a,concert,"Song 2");
            assertTrue(s1>0);assertTrue(s2>0);
            assertFalse(db.editSong(b,concert,s1,"Not allowed"));
            assertTrue(db.favoriteSong(a,concert,s1));
            assertTrue(db.favoriteSong(a,concert,s2));
            assertFalse(db.songs(a,concert).get(0).favorite);
            assertTrue(db.songs(a,concert).get(1).favorite);
            assertTrue(db.editSong(a,concert,s1,"Edited song"));
            long photo=db.addPhoto(a,concert,"test-photo-path");
            assertTrue(photo>0); assertTrue(db.editPhoto(a,concert,photo,"Caption"));
            db.close(); // Reopen the file through SQLiteOpenHelper, not an in-memory list.
            assertEquals("Artist",db.concert(a,concert).getArtist());
            assertEquals("1234567890123456789012",db.concert(a,concert).getSpotifyArtistId());
            assertEquals("Edited song",db.songs(a,concert).get(0).name);
            assertEquals("Caption",db.photos(a,concert).get(0).caption);
            assertFalse(db.deleteConcert(b,concert));
            assertTrue(db.deleteConcert(a,concert));
            assertTrue(db.songs(a,concert).isEmpty());assertTrue(db.photos(a,concert).isEmpty());
            assertEquals(0,android.database.DatabaseUtils.longForQuery(db.getReadableDatabase(),"SELECT COUNT(*) FROM songs WHERE concert_id=?",new String[]{Long.toString(concert)}));
            assertEquals(0,android.database.DatabaseUtils.longForQuery(db.getReadableDatabase(),"SELECT COUNT(*) FROM photos WHERE concert_id=?",new String[]{Long.toString(concert)}));
        } finally {
            for(ConcertModel c:db.concerts(a))db.deleteConcert(a,c.getId());
            for(ConcertModel c:db.concerts(b))db.deleteConcert(b,c.getId());
            db.getWritableDatabase().delete("users","id IN (?,?)",new String[]{Long.toString(a),Long.toString(b)});
        }
    }
}
