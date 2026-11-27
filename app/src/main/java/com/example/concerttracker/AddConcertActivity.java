package com.example.concerttracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.View;
import android.view.Gravity;
import android.text.TextWatcher;
import android.text.Editable;
import androidx.appcompat.app.AlertDialog;
import java.util.List;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AddConcertActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private long user,concert;
    private ImageView image;
    private EditText artist,date,venue;
    private String selectedDate="", selectedImage, originalImage;
    private boolean saved;
    private String spotifyId, spotifyName, spotifyImageUrl;
    private TextView spotifySelection;
    private Button spotifySearch;
    private AlertDialog spotifyDialog;
    private static final String LOCAL_NETWORK="android.permission.ACCESS_LOCAL_NETWORK";
    private final ActivityResultLauncher<String> networkPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
        if(granted && spotifySearch!=null)searchSpotify();
        else Screen.message(this,"Local network access is needed for Spotify. Manual logging still works.");
    });
    private final ActivityResultLauncher<PickVisualMediaRequest> picker=registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),uri->{
        if(uri==null || image==null) return;
        try {
            String file=ImageStore.copy(this,uri); discardDraftImage(); selectedImage=file; showImage();
        } catch(Exception e) { Screen.message(this,"Could not load photo (use an image under 25 MB)"); }
    });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Screen.layout(this,R.layout.activity_add_concert);
        db=DatabaseHelper.get(this); user=Screen.user(this); concert=Screen.concert(this); if(!Screen.requireUser(this,db,user)) return;
        image=findViewById(R.id.ivConcertImage); artist=findViewById(R.id.etArtist); date=findViewById(R.id.etDate); venue=findViewById(R.id.etVenue);
        if(concert>0) {
            ConcertModel c=db.concert(user,concert); if(c==null) { Screen.message(this,"Concert not found"); finish(); return; }
            originalImage=c.getImageUri(); selectedImage=originalImage; selectedDate=c.getDate();
            artist.setText(c.getArtist()); venue.setText(c.getVenue());
            spotifyId=c.getSpotifyArtistId(); spotifyName=c.getArtist();
            ((TextView)findViewById(R.id.tvConcertFormTitle)).setText("Edit Concert");
        }
        if(state!=null) {
            selectedImage=state.getString("image"); selectedDate=state.getString("date","");
            spotifyId=state.getString("spotify_id"); spotifyName=state.getString("spotify_name");
            artist.setText(state.getString("artist_draft",artist.getText().toString()));
        }
        spotifySearch=findViewById(R.id.btnSpotifySearch);
        spotifySelection=findViewById(R.id.tvSpotifySelection);
        spotifySearch.setOnClickListener(v->searchSpotify());
        artist.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                if(spotifyId!=null&&!s.toString().trim().equals(spotifyName)){clearSpotify();showImage();}
            }
            public void afterTextChanged(Editable e){}
        });
        date.setText(Dates.display(selectedDate)); showImage();
        findViewById(R.id.btnBack).setOnClickListener(v->finish());
        findViewById(R.id.btnAddPicture).setOnClickListener(v->pick()); image.setOnClickListener(v->pick());
        date.setOnClickListener(v->chooseDate()); findViewById(R.id.btnSaveConcert).setOnClickListener(v->save());
        findViewById(R.id.btnRemovePicture).setOnClickListener(v->{discardDraftImage(); selectedImage=null; clearSpotify(); showImage();});
    }
    private void pick() { picker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()); }
    private void showImage() {
        boolean linked=SpotifyCatalog.validId(spotifyId);
        int pad=selectedImage==null&&!linked?Screen.dp(this,16):0; image.setPadding(pad,pad,pad,pad);
        SpotifyCatalog.cancelImage(image);
        if(selectedImage==null && linked) SpotifyCatalog.showImage(image,spotifyId,spotifyImageUrl,android.R.drawable.ic_menu_gallery);
        else {image.setScaleType(ImageView.ScaleType.CENTER_CROP); ImageStore.show(image,selectedImage,android.R.drawable.ic_menu_gallery);}
        if(spotifySelection!=null){
            spotifySelection.setText(linked?"Open on Spotify · "+spotifyName:"Optional: find an artist and their picture on Spotify.");
            spotifySelection.setCompoundDrawablesRelativeWithIntrinsicBounds(linked?R.drawable.ic_spotify:0,0,0,0);
            spotifySelection.setCompoundDrawablePadding(Screen.dp(this,6));
            spotifySelection.setOnClickListener(linked?v->SpotifyCatalog.open(this,spotifyId):null);
        }
    }
    private void clearSpotify(){spotifyId=null;spotifyName=null;spotifyImageUrl=null;}
    private void searchSpotify(){
        String query=artist.getText().toString().trim();
        if(query.isEmpty()||query.length()>150){artist.setError("Enter an artist name to search");return;}
        if(android.os.Build.VERSION.SDK_INT>=37 && checkSelfPermission(LOCAL_NETWORK)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            networkPermission.launch(LOCAL_NETWORK);return;
        }
        spotifySearch.setEnabled(false); spotifySearch.setText("Searching Spotify…");
        SpotifyCatalog.search(query,(artists,error)->{
            if(isFinishing()||isDestroyed())return;
            spotifySearch.setEnabled(true); spotifySearch.setText("Search Spotify");
            if(error!=null){new AlertDialog.Builder(this).setTitle("Spotify unavailable").setMessage(error+"\n\nYou can still enter the artist manually and add your own picture.").setPositiveButton("OK",null).show();return;}
            if(!artist.getText().toString().trim().equals(query)){Screen.message(this,"Artist text changed. Search again.");return;}
            if(artists.isEmpty()){Screen.message(this,"No Spotify artists found. Try a different name.");return;}
            showArtists(artists);
        });
    }
    private void showArtists(List<SpotifyCatalog.Artist> artists){
        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(Screen.dp(this,12),0,Screen.dp(this,12),0);scroll.addView(list);
        for(SpotifyCatalog.Artist result:artists){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Screen.dp(this,8),0,Screen.dp(this,8));
            ImageView thumb=new ImageView(this);thumb.setContentDescription(result.name+" Spotify artist image");row.addView(thumb,new LinearLayout.LayoutParams(Screen.dp(this,64),Screen.dp(this,64)));
            SpotifyCatalog.showImage(thumb,result.id,result.image,android.R.drawable.ic_menu_gallery);
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(Screen.dp(this,12),0,0,0);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            Button select=new Button(this);select.setText(result.name);select.setAllCaps(false);labels.addView(select,new LinearLayout.LayoutParams(-1,-2));
            TextView link=new TextView(this);link.setText("Open on Spotify");link.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_spotify,0,0,0);link.setCompoundDrawablePadding(Screen.dp(this,6));link.setPadding(0,Screen.dp(this,6),0,Screen.dp(this,6));labels.addView(link);link.setOnClickListener(v->SpotifyCatalog.open(this,result.id));
            select.setOnClickListener(v->{
                if(result.name.length()>150){Screen.message(this,"Artist name is too long for this log");return;}
                discardDraftImage();selectedImage=null;
                clearSpotify();artist.setText(result.name);artist.setError(null);
                spotifyId=result.id;spotifyName=result.name;spotifyImageUrl=result.image;showImage();
                spotifyDialog.dismiss();Screen.message(this,"Spotify artist selected!");
            });
            list.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        spotifyDialog=new AlertDialog.Builder(this).setTitle("Choose a Spotify artist").setView(scroll).setNegativeButton("Cancel",null).create();spotifyDialog.show();
    }
    private void chooseDate() {
        Calendar c=Dates.calendar(selectedDate);
        new DatePickerDialog(this,(v,y,m,d)->{selectedDate=Dates.iso(y,m,d); date.setText(Dates.display(selectedDate)); date.setError(null);},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void save() {
        String name=artist.getText().toString().trim(), place=venue.getText().toString().trim(); boolean valid=true;
        artist.setError(null); date.setError(null); venue.setError(null);
        if(name.isEmpty()||name.length()>150) {artist.setError("Enter an artist (1–150 characters)"); valid=false;}
        if(!Dates.valid(selectedDate)) {date.setError("Select a valid date"); valid=false;}
        if(place.isEmpty()||place.length()>200) {venue.setError("Enter a venue (1–200 characters)"); valid=false;}
        if(!valid) {Screen.message(this,"Please fill in artist, date and venue"); return;}
        try {
            long id=db.saveConcert(user,concert,name,selectedDate,place,selectedImage,spotifyId);
            if(id<=0) {Screen.message(this,"Could not save concert"); return;}
            saved=true; Screen.message(this,concert>0?"Concert updated!":"Concert saved!");
            if(concert<=0) startActivity(Screen.intent(this,SetlistActivity.class,user,id));
            finish();
        } catch(RuntimeException e) { android.util.Log.e("ConcertLogify","Save concert",e); Screen.message(this,"Could not save concert. Try again."); }
    }
    private void discardDraftImage() { if(selectedImage!=null && !selectedImage.equals(originalImage)) ImageStore.delete(this,selectedImage); }
    @Override protected void onSaveInstanceState(Bundle out) {super.onSaveInstanceState(out); out.putString("image",selectedImage); out.putString("date",selectedDate); out.putString("spotify_id",spotifyId);out.putString("spotify_name",spotifyName);out.putString("artist_draft",artist==null?"":artist.getText().toString());}
    @Override protected void onDestroy() { if(spotifyDialog!=null) spotifyDialog.dismiss(); if(isFinishing()&&!saved) discardDraftImage(); super.onDestroy(); }
}
