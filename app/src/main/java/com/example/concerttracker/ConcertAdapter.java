package com.example.concerttracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ConcertAdapter extends RecyclerView.Adapter<ConcertAdapter.ConcertViewHolder> {
    public interface Actions { void open(ConcertModel c); void edit(ConcertModel c); void delete(ConcertModel c); }
    private final List<ConcertModel> concerts;
    private final Actions actions;
    public ConcertAdapter(List<ConcertModel> concerts,Actions actions) { this.concerts=concerts; this.actions=actions; setHasStableIds(true); }
    @Override public long getItemId(int position) { return concerts.get(position).getId(); }
    @NonNull @Override public ConcertViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
        return new ConcertViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_concert,parent,false));
    }
    @Override public void onBindViewHolder(@NonNull ConcertViewHolder h,int position) {
        ConcertModel c=concerts.get(position);
        h.artist.setText("Artist: "+c.getArtist()); h.date.setText("Date: "+Dates.display(c.getDate())); h.venue.setText(c.getVenue());
        SpotifyCatalog.cancelImage(h.image);
        h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageStore.show(h.image,c.getImageUri(),c.getImageResId());
        String spotifyId=c.getSpotifyArtistId();
        boolean linked=SpotifyCatalog.validId(spotifyId);
        h.spotify.setVisibility(linked?View.VISIBLE:View.GONE);
        h.spotify.setOnClickListener(linked?v->SpotifyCatalog.open(v.getContext(),spotifyId):null);
        if(linked && c.getImageUri()==null) SpotifyCatalog.showImage(h.image,spotifyId,null,c.getImageResId());
        h.actions.setVisibility(View.GONE);
        h.itemView.setOnClickListener(v->actions.open(c));
        h.itemView.setOnLongClickListener(v->{h.actions.setVisibility(View.VISIBLE); return true;});
        h.options.setOnClickListener(v->h.actions.setVisibility(h.actions.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        h.edit.setOnClickListener(v->actions.edit(c)); h.delete.setOnClickListener(v->actions.delete(c));
    }
    @Override public int getItemCount() { return concerts.size(); }
    static class ConcertViewHolder extends RecyclerView.ViewHolder {
        final ImageView image,edit,delete; final TextView artist,date,venue,spotify; final ImageButton options; final LinearLayout actions;
        ConcertViewHolder(View v) {
            super(v); image=v.findViewById(R.id.ivArtist); artist=v.findViewById(R.id.tvArtist); date=v.findViewById(R.id.tvDate); venue=v.findViewById(R.id.tvVenue); spotify=v.findViewById(R.id.tvSpotifyLink); options=v.findViewById(R.id.btnOptions); actions=v.findViewById(R.id.layoutActions); edit=v.findViewById(R.id.btnEdit); delete=v.findViewById(R.id.btnDelete);
        }
    }
}
