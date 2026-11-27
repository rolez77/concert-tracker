package com.example.concerttracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private long user;
    private ConcertAdapter adapter;
    private EditText search;
    private TextView empty, filterLabel;
    private String filterType="", filterValue="";
    private final List<ConcertModel> visible=new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Screen.layout(this,R.layout.activity_home);
        db=DatabaseHelper.get(this); user=Screen.user(this); if(!Screen.requireUser(this,db,user)) return;
        if(state!=null) { filterType=state.getString("filter_type",""); filterValue=state.getString("filter_value",""); }
        search=findViewById(R.id.etSearch); empty=findViewById(R.id.tvEmptyConcerts); filterLabel=findViewById(R.id.tvActiveFilter);
        RecyclerView list=findViewById(R.id.recyclerViewConcerts); list.setLayoutManager(new LinearLayoutManager(this));
        adapter=new ConcertAdapter(visible,new ConcertAdapter.Actions() {
            public void open(ConcertModel c) { openConcert(c,SetlistActivity.class); }
            public void edit(ConcertModel c) { openConcert(c,AddConcertActivity.class); }
            public void delete(ConcertModel c) { confirmDelete(c); }
        });
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { refresh(); }
            public void afterTextChanged(Editable e) {}
        });
        findViewById(R.id.btnBack).setOnClickListener(v->Screen.logout(this));
        findViewById(R.id.btnLogifyIt).setOnClickListener(v->startActivity(Screen.intent(this,AddConcertActivity.class,user,-1)));
        findViewById(R.id.btnFilter).setOnClickListener(v->showFilterDialog());
        filterLabel.setOnClickListener(v->setFilter("",""));
        findViewById(R.id.btnNavSetlist).setOnClickListener(v->chooseConcert(SetlistActivity.class));
        findViewById(R.id.btnNavGallery).setOnClickListener(v->chooseConcert(GalleryActivity.class));
        refresh();
    }
    @Override protected void onResume() { super.onResume(); if(adapter!=null) refresh(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("filter_type",filterType); out.putString("filter_value",filterValue); }
    private void refresh() {
        if(adapter==null) return;
        String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<ConcertModel> results=new ArrayList<>();
        try {
            for(ConcertModel c:db.concerts(user)) {
                String artist=c.getArtist().toLowerCase(Locale.ROOT), date=c.getDate();
                if(!q.isEmpty() && !artist.contains(q) && !Dates.matches(date,q)) continue;
                if(filterType.equals("Artist") && !artist.contains(filterValue.toLowerCase(Locale.ROOT))) continue;
                if(filterType.equals("Date") && !date.equals(filterValue)) continue;
                if(filterType.equals("Year") && !date.startsWith(filterValue+"-")) continue;
                results.add(c);
            }
        } catch(RuntimeException e) { android.util.Log.e("ConcertLogify","Read concerts",e); Screen.message(this,"Could not load concerts"); return; }
        visible.clear(); visible.addAll(results); adapter.notifyDataSetChanged();
        empty.setVisibility(visible.isEmpty()?View.VISIBLE:View.GONE);
        empty.setText(q.isEmpty()&&filterType.isEmpty()?"No concerts yet. Tap Logify It! to add one.":"No matching concerts.");
        filterLabel.setVisibility(filterType.isEmpty()?View.GONE:View.VISIBLE);
        filterLabel.setText(filterType.isEmpty()?"":filterType+": "+(filterType.equals("Date")?Dates.display(filterValue):filterValue)+"  × Clear");
    }
    private void openConcert(ConcertModel c,Class<?> screen) { startActivity(Screen.intent(this,screen,user,c.getId())); }
    private void chooseConcert(Class<?> screen) {
        List<ConcertModel> items=new ArrayList<>(visible);
        if(items.isEmpty()) { Screen.message(this,"Add or find a concert first"); return; }
        if(items.size()==1) { openConcert(items.get(0),screen); return; }
        String[] labels=new String[items.size()];
        for(int i=0;i<labels.length;i++) labels[i]=items.get(i).getArtist()+" • "+Dates.display(items.get(i).getDate());
        new AlertDialog.Builder(this).setTitle("Choose a concert").setItems(labels,(d,i)->openConcert(items.get(i),screen)).setNegativeButton("Cancel",null).show();
    }
    private void confirmDelete(ConcertModel c) {
        new AlertDialog.Builder(this).setTitle("Delete concert?").setMessage("Delete "+c.getArtist()+" and its songs and photos?")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{
                    try { if(db.deleteConcert(user,c.getId())) { Screen.message(this,"Concert deleted"); refresh(); } else Screen.message(this,"Concert not found"); }
                    catch(RuntimeException e) { Screen.message(this,"Could not delete concert"); }
                }).show();
    }
    private void setFilter(String type,String value) { filterType=type; filterValue=value; refresh(); Screen.message(this,type.isEmpty()?"Filter cleared":"Filtered by "+type); }
    private void showFilterDialog() {
        View v=getLayoutInflater().inflate(R.layout.dialog_filter,null);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(v).setNeutralButton("Clear filter",(d,w)->setFilter("","")).create();
        v.findViewById(R.id.btnFilterDate).setOnClickListener(x->{dialog.dismiss(); Calendar c=Dates.calendar(filterType.equals("Date")?filterValue:""); new DatePickerDialog(this,(picker,y,m,day)->setFilter("Date",Dates.iso(y,m,day)),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();});
        v.findViewById(R.id.btnFilterArtist).setOnClickListener(x->{dialog.dismiss(); textFilter("Artist");});
        v.findViewById(R.id.btnFilterYear).setOnClickListener(x->{dialog.dismiss(); textFilter("Year");});
        dialog.show();
    }
    private void textFilter(String type) {
        EditText input=new EditText(this); input.setSingleLine(true); input.setHint(type.equals("Year")?"e.g. 2026":"Artist name");
        if(type.equals("Year")) input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(type.equals(filterType)) input.setText(filterValue);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Filter by "+type).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String val=input.getText().toString().trim();
            if(val.isEmpty() || (type.equals("Year")&&!val.matches("[1-9][0-9]{3}"))) { input.setError(type.equals("Year")?"Enter a four-digit year":"Enter an artist"); return; }
            setFilter(type,val); dialog.dismiss();
        })); dialog.show();
    }
}
