package com.example.concerttracker;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.LruCache;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Public catalog only. The client secret never enters Android. No disk artwork cache. */
final class SpotifyCatalog {
    private static final String BASE = "http://10.0.2.2:8765";
    private static final ExecutorService SEARCH = Executors.newSingleThreadExecutor();
    private static final ExecutorService IMAGES = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long TTL = 5 * 60 * 1000L;
    private static final LruCache<String, CachedImage> CACHE = new LruCache<>(24);
    private static final class CachedImage {
        final Bitmap bitmap; final long until;
        CachedImage(Bitmap bitmap, long duration) { this.bitmap=bitmap; until=android.os.SystemClock.elapsedRealtime()+duration; }
    }
    static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9]{22}"); }
    static final class Artist {
        final String id,name,image;
        Artist(JSONObject obj) throws Exception {
            id=obj.getString("id"); name=obj.getString("name"); image=obj.optString("image","");
            if(!validId(id)||name.trim().isEmpty()) throw new IOException("Invalid artist response");
        }
    }
    interface SearchResult { void accept(List<Artist> artists, String error); }
    static void search(String query, SearchResult callback) {
        SEARCH.execute(()->{
            try {
                JSONArray items=json("/search?q="+URLEncoder.encode(query,"UTF-8")).getJSONArray("artists");
                List<Artist> artists=new ArrayList<>();
                for(int i=0;i<items.length();i++) artists.add(new Artist(items.getJSONObject(i)));
                MAIN.post(()->callback.accept(artists,null));
            } catch(Exception e) {
                String error=e instanceof ApiException ? e.getMessage() : "Cannot reach Spotify service. Start server.py on your computer and check the connection.";
                MAIN.post(()->callback.accept(null,error));
            }
        });
    }
    private static final class ApiException extends IOException { ApiException(String message) {super(message);} }
    private static JSONObject json(String path) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();
        c.setConnectTimeout(4000); c.setReadTimeout(35000); c.setInstanceFollowRedirects(false); c.setUseCaches(false);
        try {
            int status=c.getResponseCode();
            InputStream stream=status>=200&&status<300?c.getInputStream():c.getErrorStream();
            if(stream==null) throw new IOException("Empty server response");
            JSONObject obj=new JSONObject(new String(read(stream,1024*1024),java.nio.charset.StandardCharsets.UTF_8));
            if(status<200||status>=300) throw new ApiException(obj.optString("error","Spotify request failed ("+status+")"));
            return obj;
        } finally {c.disconnect();}
    }
    private static byte[] read(InputStream input,int limit) throws IOException {
        try(InputStream in=input; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))!=-1) {if(out.size()+n>limit) throw new IOException("Response too large");out.write(buffer,0,n);}
            return out.toByteArray();
        }
    }
    private static Bitmap fetchImage(String raw) throws Exception {
        if(raw==null||raw.isEmpty())return null;
        URL url=new URL(raw);
        if(!"https".equals(url.getProtocol())||!"i.scdn.co".equalsIgnoreCase(url.getHost())||url.getUserInfo()!=null) return null;
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(4000); c.setReadTimeout(8000); c.setInstanceFollowRedirects(false); c.setUseCaches(false);
        try {
            if(c.getResponseCode()!=200)return null;
            byte[] bytes=read(c.getInputStream(),5*1024*1024);
            BitmapFactory.Options opts=new BitmapFactory.Options(); opts.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
            if(opts.outWidth<=0||opts.outHeight<=0)return null;
            opts.inSampleSize=1;
            while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>600)opts.inSampleSize*=2;
            opts.inJustDecodeBounds=false;
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
        } finally {c.disconnect();}
    }
    static void cancelImage(ImageView view) {view.setTag(null);}
    static void showImage(ImageView view,String id,String knownUrl,int placeholder) {
        final Object request=new Object(); view.setTag(request); view.setImageResource(placeholder);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if(!validId(id))return;
        CachedImage hit=CACHE.get(id);
        if(hit!=null&&hit.until>android.os.SystemClock.elapsedRealtime()) {if(hit.bitmap!=null)view.setImageBitmap(hit.bitmap);return;}
        java.lang.ref.WeakReference<ImageView> target=new java.lang.ref.WeakReference<>(view);
        IMAGES.execute(()->{
            Bitmap bitmap=null;
            try {
                CachedImage recent=CACHE.get(id);
                if(recent!=null&&recent.until>android.os.SystemClock.elapsedRealtime()) bitmap=recent.bitmap;
                else {
                    String url=knownUrl;
                    if(url==null)url=json("/artist?id="+id).optString("image","");
                    bitmap=fetchImage(url);
                    CACHE.put(id,new CachedImage(bitmap,bitmap==null?10000:TTL));
                }
            } catch(Exception ignored) { CACHE.put(id,new CachedImage(null,5000)); }
            Bitmap result=bitmap;
            MAIN.post(()->{ImageView current=target.get();if(current!=null&&current.getTag()==request&&result!=null)current.setImageBitmap(result);});
        });
    }
    static void open(Context activity,String id) {
        if(!validId(id))return;
        try {Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://open.spotify.com/artist/"+id));if(!(activity instanceof Activity))intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);activity.startActivity(intent); }
        catch(Exception e){android.widget.Toast.makeText(activity,"No browser available to open Spotify",android.widget.Toast.LENGTH_SHORT).show();}
    }
    static void titleLink(Activity activity,TextView title,String id) {
        if(!validId(id))return;
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_spotify,0,0,0);
        title.setCompoundDrawablePadding(Screen.dp(activity,6));
        title.setContentDescription(title.getText()+". Open on Spotify");
        title.setOnClickListener(v->open(activity,id));
    }
}
