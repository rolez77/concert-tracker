package com.example.concerttracker;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private long user,concert;
    private LinearLayout rows;
    private AlertDialog editor;
    private EditText editorInput;
    private long editingPhoto=-1;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker=registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),uri->{
        if(uri==null || rows==null) return;
        String path=null;
        long id=-1;
        try {path=ImageStore.copy(this,uri); id=db.addPhoto(user,concert,path);}
        catch(Exception e){android.util.Log.e("ConcertLogify","Save gallery photo",e);}
        if(id<=0){ImageStore.delete(this,path);Screen.message(this,"Could not save photo (use an image under 25 MB)");return;}
        refresh(); Screen.message(this,"Photo saved!"); captionEditor(id,"");
    });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Screen.layout(this,R.layout.activity_gallery);
        db=DatabaseHelper.get(this); user=Screen.user(this); concert=Screen.concert(this);
        if(!Screen.requireUser(this,db,user))return;
        ConcertModel c=db.concert(user,concert);if(c==null){Screen.message(this,"Concert not found");finish();return;}
        rows=findViewById(R.id.photoRows);
        ((TextView)findViewById(R.id.tvGalleryTitle)).setText("Gallery – "+c.getArtist());
        SpotifyCatalog.titleLink(this,findViewById(R.id.tvGalleryTitle),c.getSpotifyArtistId());
        findViewById(R.id.btnBack).setOnClickListener(v->finish()); findViewById(R.id.btnAddPhoto).setOnClickListener(v->pick());
        refresh();
        if(state!=null&&state.getBoolean("editor_open"))captionEditor(state.getLong("editing_photo"),state.getString("caption_draft",""));
        if(state==null&&getIntent().getBooleanExtra("pick_photo",false)) rows.post(this::pick);
    }
    private void pick(){picker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());}
    private int dp(int n){return Screen.dp(this,n);}
    private void refresh(){
        rows.removeAllViews(); List<DatabaseHelper.Photo> photos=db.photos(user,concert);
        if(photos.isEmpty()){TextView empty=new TextView(this);empty.setText("No photos yet. Tap + to add a photo.");empty.setTextColor(getColor(R.color.white));empty.setTextSize(16);empty.setPadding(dp(8),dp(16),dp(8),dp(16));rows.addView(empty);return;}
        for(DatabaseHelper.Photo p:photos){
            CardView card=new CardView(this);card.setCardBackgroundColor(android.graphics.Color.parseColor("#0B2E1C"));card.setRadius(dp(8));card.setCardElevation(dp(2));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(16);rows.addView(card,cp);
            LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.HORIZONTAL);content.setGravity(Gravity.CENTER_VERTICAL);content.setPadding(dp(12),dp(12),dp(12),dp(12));card.addView(content);
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackgroundColor(getColor(R.color.white));image.setContentDescription("Concert photo");content.addView(image,new LinearLayout.LayoutParams(dp(100),dp(100)));ImageStore.show(image,p.path,android.R.drawable.ic_menu_gallery);
            TextView caption=new TextView(this);caption.setText(p.caption.isEmpty()?"Tap to add caption  ⋮":p.caption+"  ⋮");caption.setTextColor(getColor(R.color.white));caption.setTextSize(16);caption.setGravity(Gravity.CENTER);caption.setPadding(dp(8),dp(8),dp(8),dp(8));content.addView(caption,new LinearLayout.LayoutParams(0,-2,1));
            card.setOnClickListener(v->photoOptions(p));card.setOnLongClickListener(v->{photoOptions(p);return true;});
        }
    }
    private void photoOptions(DatabaseHelper.Photo p){new AlertDialog.Builder(this).setTitle("Concert photo").setItems(new String[]{"View photo","Edit caption","Delete photo"},(d,i)->{
        if(i==0){ImageView full=new ImageView(this);full.setAdjustViewBounds(true);full.setMaxHeight(dp(420));full.setScaleType(ImageView.ScaleType.FIT_CENTER);ImageStore.show(full,p.path,android.R.drawable.ic_menu_gallery);new AlertDialog.Builder(this).setTitle(p.caption.isEmpty()?"Concert photo":p.caption).setView(full).setPositiveButton("Close",null).show();}
        else if(i==1)captionEditor(p.id,p.caption);
        else new AlertDialog.Builder(this).setTitle("Delete photo?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,w)->{try{if(db.deletePhoto(user,concert,p.id)){refresh();Screen.message(this,"Photo deleted");}else Screen.message(this,"Photo not found");}catch(RuntimeException e){Screen.message(this,"Could not delete photo");}}).show();
    }).show();}
    private void captionEditor(long id,String text){
        editingPhoto=id;editorInput=new EditText(this);editorInput.setHint("Optional caption");editorInput.setText(text);editorInput.setSelection(text.length());
        editor=new AlertDialog.Builder(this).setTitle("Photo caption").setView(editorInput).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String caption=editorInput.getText().toString().trim();if(caption.length()>300){editorInput.setError("Maximum 300 characters");return;}try{if(db.editPhoto(user,concert,id,caption)){editor.dismiss();refresh();Screen.message(this,"Caption saved!");}else Screen.message(this,"Photo not found");}catch(RuntimeException e){Screen.message(this,"Could not save caption");}}));editor.show();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(editor!=null&&editor.isShowing()){out.putBoolean("editor_open",true);out.putLong("editing_photo",editingPhoto);out.putString("caption_draft",editorInput.getText().toString());}}
}
