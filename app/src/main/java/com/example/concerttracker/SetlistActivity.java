package com.example.concerttracker;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class SetlistActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private long user,concert;
    private LinearLayout rows;
    private TextView favorite;
    private AlertDialog editor;
    private EditText editorInput;
    private long editingSong=-1;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Screen.layout(this,R.layout.activity_setlist);
        db=DatabaseHelper.get(this); user=Screen.user(this); concert=Screen.concert(this);
        if(!Screen.requireUser(this,db,user)) return;
        ConcertModel c=db.concert(user,concert); if(c==null) {Screen.message(this,"Concert not found"); finish(); return;}
        rows=findViewById(R.id.songRows); favorite=findViewById(R.id.tvFavoriteSong);
        ((TextView)findViewById(R.id.tvSetlistTitle)).setText("Setlistify – "+c.getArtist());
        SpotifyCatalog.titleLink(this,findViewById(R.id.tvSetlistTitle),c.getSpotifyArtistId());
        findViewById(R.id.btnBack).setOnClickListener(v->finish());
        findViewById(R.id.btnGallery).setOnClickListener(v->startActivity(Screen.intent(this,GalleryActivity.class,user,concert)));
        findViewById(R.id.btnAddSong).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Add to concert")
                .setItems(new String[]{"Add song","Add photo"},(d,i)->{if(i==0) songEditor(-1,""); else startActivity(Screen.intent(this,GalleryActivity.class,user,concert).putExtra("pick_photo",true));}).show());
        refresh();
        if(state!=null && state.getBoolean("editor_open")) songEditor(state.getLong("editing_song",-1),state.getString("song_draft",""));
    }
    @Override protected void onResume() {super.onResume(); if(rows!=null) refresh();}
    private TextView label(String text) {
        TextView t=new TextView(this); t.setText(text); t.setTextColor(getColor(R.color.white)); t.setTextSize(16); t.setPadding(Screen.dp(this,12),Screen.dp(this,12),Screen.dp(this,12),Screen.dp(this,12)); return t;
    }
    private void refresh() {
        rows.removeAllViews(); favorite.setText("Fav Song: Not selected");
        List<DatabaseHelper.Song> songs=db.songs(user,concert);
        if(songs.isEmpty()) { rows.addView(label("No songs yet. Tap + to add a song.")); return; }
        int n=0;
        for(DatabaseHelper.Song song:songs) {
            if(song.favorite) favorite.setText("Fav Song: "+song.name);
            TextView t=label((++n)+". "+song.name+(song.favorite?"  ★":"")+"   ⋮");
            android.util.TypedValue ripple=new android.util.TypedValue();
            if(getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true)) t.setBackgroundResource(ripple.resourceId);
            t.setContentDescription("Song "+n+": "+song.name+". Tap for options.");
            t.setOnClickListener(v->songOptions(song)); t.setOnLongClickListener(v->{songOptions(song);return true;});
            rows.addView(t,new LinearLayout.LayoutParams(-1,-2));
        }
    }
    private void songOptions(DatabaseHelper.Song song) {
        new AlertDialog.Builder(this).setTitle(song.name).setItems(new String[]{"Edit song","Set as favorite","Delete song"},(d,i)->{
            if(i==0) songEditor(song.id,song.name);
            else if(i==1) { try { if(db.favoriteSong(user,concert,song.id)) {refresh();Screen.message(this,"Favorite song saved");} else Screen.message(this,"Song not found"); }catch(RuntimeException e){Screen.message(this,"Could not save favorite");} }
            else new AlertDialog.Builder(this).setTitle("Delete song?").setMessage(song.name).setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,w)->{
                try {if(db.deleteSong(user,concert,song.id)){refresh();Screen.message(this,"Song deleted");}else Screen.message(this,"Song not found");}catch(RuntimeException e){Screen.message(this,"Could not delete song");}
            }).show();
        }).show();
    }
    private void songEditor(long id,String text) {
        editingSong=id; editorInput=new EditText(this); editorInput.setSingleLine(true); editorInput.setHint("Song name"); editorInput.setText(text); editorInput.setSelection(text.length());
        editor=new AlertDialog.Builder(this).setTitle(id<0?"Add song":"Edit song").setView(editorInput).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String name=editorInput.getText().toString().trim();
            if(name.isEmpty()||name.length()>200){editorInput.setError("Enter a song name (1–200 characters)");Screen.message(this,"Song name is required");return;}
            try {boolean ok=id<0?db.addSong(user,concert,name)>0:db.editSong(user,concert,id,name);
                if(ok){editor.dismiss();refresh();Screen.message(this,id<0?"Song added!":"Song updated!");}else Screen.message(this,"Could not save song");
            }catch(RuntimeException e){Screen.message(this,"Could not save song");}
        })); editor.show();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if(editor!=null&&editor.isShowing()){out.putBoolean("editor_open",true);out.putLong("editing_song",editingSong);out.putString("song_draft",editorInput.getText().toString());}
    }
}
