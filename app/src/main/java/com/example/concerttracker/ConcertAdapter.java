package com.example.concerttracker;

import android.net.Uri;
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

    private List<ConcertModel> concertList;

    public ConcertAdapter(List<ConcertModel> concertList) {
        this.concertList = concertList;
    }

    @NonNull
    @Override
    public ConcertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_concert, parent, false);
        return new ConcertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConcertViewHolder holder, int position) {
        ConcertModel concert = concertList.get(position);
        holder.tvArtist.setText("Artist: " + concert.getArtist());
        holder.tvDate.setText("Date: " + concert.getDate());
        holder.tvVenue.setText(concert.getVenue());
        if (concert.getImageUri() != null) {
            holder.ivArtist.setImageURI(Uri.parse(concert.getImageUri()));
        } else {
            holder.ivArtist.setImageResource(concert.getImageResId());
        }

        // Toggle action buttons visibility on options click
        holder.btnOptions.setOnClickListener(v -> {
            if (holder.layoutActions.getVisibility() == View.VISIBLE) {
                holder.layoutActions.setVisibility(View.GONE);
            } else {
                holder.layoutActions.setVisibility(View.VISIBLE);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                concertList.remove(currentPos);
                notifyItemRemoved(currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return concertList.size();
    }

    static class ConcertViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtist, btnEdit, btnDelete;
        TextView tvArtist, tvDate, tvVenue;
        ImageButton btnOptions;
        LinearLayout layoutActions;

        public ConcertViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtist = itemView.findViewById(R.id.ivArtist);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            btnOptions = itemView.findViewById(R.id.btnOptions);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}