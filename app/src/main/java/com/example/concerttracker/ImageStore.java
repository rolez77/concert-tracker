package com.example.concerttracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Copies selected photos into app-private storage; no external URI permission is needed later. */
final class ImageStore {
    private static final long MAX_BYTES=25L*1024*1024;
    private static File file(Context context,String name) {
        if(name==null || !name.matches("[a-f0-9-]{36}\\.img")) return null;
        return new File(new File(context.getFilesDir(),"concert_images"),name);
    }
    static String copy(Context context,Uri uri) throws IOException {
        String name=UUID.randomUUID()+".img";
        File dest=file(context,name);
        if(!dest.getParentFile().isDirectory() && !dest.getParentFile().mkdirs()) throw new IOException("Cannot create image directory");
        boolean success=false;
        try {
            try(InputStream in=context.getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(dest)) {
                if(in==null) throw new IOException("Cannot open image");
                byte[] buffer=new byte[8192]; int n; long total=0;
                while((n=in.read(buffer))!=-1) { total+=n; if(total>MAX_BYTES) throw new IOException("Image exceeds 25 MB"); out.write(buffer,0,n); }
            }
            BitmapFactory.Options bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(dest.getAbsolutePath(),bounds);
            if(bounds.outWidth<=0 || bounds.outHeight<=0) throw new IOException("Unsupported image");
            success=true; return name;
        } finally { if(!success) dest.delete(); }
    }
    static void delete(Context context,String name) { File f=file(context,name); if(f!=null) f.delete(); }
    static void show(ImageView view,String name,int placeholder) {
        view.setImageResource(placeholder); File f=file(view.getContext(),name); if(f==null || !f.isFile()) return;
        BitmapFactory.Options o=new BitmapFactory.Options(); o.inJustDecodeBounds=true; BitmapFactory.decodeFile(f.getAbsolutePath(),o);
        // Decode only a thumbnail for fixed-size list images; keep detail for previews.
        int target=1200;
        if(view.getLayoutParams()!=null && view.getLayoutParams().width>0 && view.getLayoutParams().height>0)
            target=Math.max(256,Math.min(1200,Math.max(view.getLayoutParams().width,view.getLayoutParams().height)*2));
        o.inSampleSize=1; while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>target) o.inSampleSize*=2;
        o.inJustDecodeBounds=false; Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath(),o); if(b!=null) view.setImageBitmap(b);
    }
}
