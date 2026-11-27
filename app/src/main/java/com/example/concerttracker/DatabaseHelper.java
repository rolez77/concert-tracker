package com.example.concerttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import android.util.Base64;

/** All persistent app data. Child rows are scoped through their owning concert and user. */
public final class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper instance;
    private final Context context;
    public static synchronized DatabaseHelper get(Context context) {
        if (instance == null) instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }
    private DatabaseHelper(Context context) { super(context, "concert_logify.db", null, 2); this.context = context; }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE COLLATE NOCASE, salt TEXT NOT NULL, password_hash TEXT NOT NULL)");
        db.execSQL("CREATE TABLE concerts (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, artist TEXT NOT NULL, concert_date TEXT NOT NULL, venue TEXT NOT NULL, image_path TEXT, spotify_artist_id TEXT)");
        db.execSQL("CREATE TABLE songs (id INTEGER PRIMARY KEY AUTOINCREMENT, concert_id INTEGER NOT NULL REFERENCES concerts(id) ON DELETE CASCADE, name TEXT NOT NULL, position INTEGER NOT NULL, favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1)))");
        db.execSQL("CREATE TABLE photos (id INTEGER PRIMARY KEY AUTOINCREMENT, concert_id INTEGER NOT NULL REFERENCES concerts(id) ON DELETE CASCADE, image_path TEXT NOT NULL, caption TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX concerts_user_date ON concerts(user_id, concert_date)");
        db.execSQL("CREATE INDEX songs_concert ON songs(concert_id, position)");
        db.execSQL("CREATE INDEX photos_concert ON photos(concert_id)");
        db.execSQL("CREATE UNIQUE INDEX one_favorite ON songs(concert_id) WHERE favorite=1");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion==1 && newVersion==2) { db.execSQL("ALTER TABLE concerts ADD COLUMN spotify_artist_id TEXT"); return; }
        throw new IllegalStateException("No migration defined from " + oldVersion + " to " + newVersion);
    }
    private static String encode(byte[] data) { return Base64.encodeToString(data, Base64.NO_WRAP); }
    private static byte[] hash(String password, byte[] salt) {
        // PBKDF2-HMAC-SHA1 works on the project's minimum Android API 24.
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 200000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException("Password hashing failed", e); }
        finally { spec.clearPassword(); }
    }
    public long register(String username, String password) {
        username = username.trim().toLowerCase(Locale.ROOT);
        if (username.isEmpty() || username.length() > 64 || password.length() < 6 || password.length() > 128 || password.trim().isEmpty()) return -1;
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        ContentValues v = new ContentValues();
        v.put("username", username); v.put("salt", encode(salt)); v.put("password_hash", encode(hash(password, salt)));
        return getWritableDatabase().insertWithOnConflict("users", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }
    public long login(String username, String password) {
        try (Cursor c = getReadableDatabase().query("users", new String[]{"id","salt","password_hash"}, "username=?", new String[]{username.trim().toLowerCase(Locale.ROOT)}, null,null,null)) {
            if (!c.moveToFirst()) return -1;
            byte[] actual = hash(password, Base64.decode(c.getString(1), Base64.DEFAULT));
            byte[] expected = Base64.decode(c.getString(2), Base64.DEFAULT);
            return MessageDigest.isEqual(actual, expected) ? c.getLong(0) : -1;
        }
    }
    public boolean userExists(long user) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id FROM users WHERE id=?", args(user))) { return c.moveToFirst(); }
    }
    private static String[] args(long... ids) {
        String[] result = new String[ids.length]; for (int i=0;i<ids.length;i++) result[i]=Long.toString(ids[i]); return result;
    }
    private static ConcertModel concert(Cursor c) { return new ConcertModel(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)); }
    public List<ConcertModel> concerts(long user) {
        List<ConcertModel> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,artist,concert_date,venue,image_path,spotify_artist_id FROM concerts WHERE user_id=? ORDER BY concert_date DESC,id DESC", args(user))) {
            while(c.moveToNext()) list.add(concert(c));
        } return list;
    }
    public ConcertModel concert(long user,long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,artist,concert_date,venue,image_path,spotify_artist_id FROM concerts WHERE user_id=? AND id=?", args(user,id))) { return c.moveToFirst()?concert(c):null; }
    }
    public long saveConcert(long user,long id,String artist,String date,String venue,String image) {
        return saveConcert(user,id,artist,date,venue,image,null);
    }
    public long saveConcert(long user,long id,String artist,String date,String venue,String image,String spotifyId) {
        if(spotifyId!=null && !SpotifyCatalog.validId(spotifyId)) return -1;
        artist=artist.trim(); venue=venue.trim();
        if (artist.isEmpty() || venue.isEmpty() || artist.length()>150 || venue.length()>200 || !Dates.valid(date)) return -1;
        ContentValues v = new ContentValues(); v.put("artist",artist); v.put("concert_date",date); v.put("venue",venue); v.put("image_path",image); v.put("spotify_artist_id",spotifyId);
        if (id <= 0) { v.put("user_id",user); return getWritableDatabase().insertOrThrow("concerts",null,v); }
        ConcertModel old = concert(user,id); if (old==null) return -1;
        if (getWritableDatabase().update("concerts",v,"user_id=? AND id=?",args(user,id)) != 1) return -1;
        if (old.getImageUri()!=null && !old.getImageUri().equals(image)) ImageStore.delete(context,old.getImageUri());
        return id;
    }
    public boolean deleteConcert(long user,long id) {
        ConcertModel old = concert(user,id); if(old==null) return false;
        List<Photo> oldPhotos = photos(user,id);
        if (getWritableDatabase().delete("concerts","user_id=? AND id=?",args(user,id))!=1) return false;
        ImageStore.delete(context,old.getImageUri()); for(Photo p:oldPhotos) ImageStore.delete(context,p.path);
        return true;
    }
    public static final class Song {
        public final long id; public final String name; public final boolean favorite;
        Song(long id,String name,boolean favorite) { this.id=id; this.name=name; this.favorite=favorite; }
    }
    public List<Song> songs(long user,long concert) {
        List<Song> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT s.id,s.name,s.favorite FROM songs s JOIN concerts c ON s.concert_id=c.id WHERE c.user_id=? AND c.id=? ORDER BY s.position,s.id",args(user,concert))) {
            while(c.moveToNext()) result.add(new Song(c.getLong(0),c.getString(1),c.getInt(2)!=0));
        } return result;
    }
    // Ownership is enforced both for child creation and for all subsequent updates/deletes.
    private static final String CHILD_WHERE = "id=? AND concert_id IN (SELECT id FROM concerts WHERE user_id=? AND id=?)";
    public long addSong(long user,long concert,String name) {
        name=name.trim(); if(name.isEmpty() || name.length()>200 || concert(user,concert)==null) return -1;
        SQLiteDatabase db=getWritableDatabase(); int pos=1;
        try(Cursor c=db.rawQuery("SELECT COALESCE(MAX(position),0)+1 FROM songs WHERE concert_id=?",args(concert))) { if(c.moveToFirst()) pos=c.getInt(0); }
        ContentValues v=new ContentValues(); v.put("concert_id",concert); v.put("name",name); v.put("position",pos);
        return db.insertOrThrow("songs",null,v);
    }
    public boolean editSong(long user,long concert,long song,String name) {
        name=name.trim(); if(name.isEmpty() || name.length()>200) return false;
        ContentValues v=new ContentValues(); v.put("name",name);
        return getWritableDatabase().update("songs",v,CHILD_WHERE,args(song,user,concert))==1;
    }
    public boolean deleteSong(long user,long concert,long song) { return getWritableDatabase().delete("songs",CHILD_WHERE,args(song,user,concert))==1; }
    public boolean favoriteSong(long user,long concert,long song) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            try(Cursor c=db.rawQuery("SELECT id FROM songs WHERE "+CHILD_WHERE,args(song,user,concert))) { if(!c.moveToFirst()) return false; }
            ContentValues v=new ContentValues(); v.put("favorite",0); db.update("songs",v,"concert_id=?",args(concert));
            v.put("favorite",1); if(db.update("songs",v,CHILD_WHERE,args(song,user,concert))!=1) return false;
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
    public static final class Photo {
        public final long id; public final String path, caption;
        Photo(long id,String path,String caption) { this.id=id; this.path=path; this.caption=caption; }
    }
    public List<Photo> photos(long user,long concert) {
        List<Photo> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT p.id,p.image_path,p.caption FROM photos p JOIN concerts c ON p.concert_id=c.id WHERE c.user_id=? AND c.id=? ORDER BY p.id",args(user,concert))) {
            while(c.moveToNext()) result.add(new Photo(c.getLong(0),c.getString(1),c.getString(2)));
        } return result;
    }
    public long addPhoto(long user,long concert,String path) {
        if(path==null || concert(user,concert)==null) return -1;
        ContentValues v=new ContentValues(); v.put("concert_id",concert); v.put("image_path",path);
        return getWritableDatabase().insertOrThrow("photos",null,v);
    }
    public boolean editPhoto(long user,long concert,long photo,String caption) {
        caption=caption.trim(); if(caption.length()>300) return false;
        ContentValues v=new ContentValues(); v.put("caption",caption);
        return getWritableDatabase().update("photos",v,CHILD_WHERE,args(photo,user,concert))==1;
    }
    public boolean deletePhoto(long user,long concert,long photo) {
        String path=null;
        try(Cursor c=getReadableDatabase().query("photos",new String[]{"image_path"},CHILD_WHERE,args(photo,user,concert),null,null,null)) { if(c.moveToFirst()) path=c.getString(0); }
        if(getWritableDatabase().delete("photos",CHILD_WHERE,args(photo,user,concert))!=1) return false;
        ImageStore.delete(context,path); return true;
    }
}
