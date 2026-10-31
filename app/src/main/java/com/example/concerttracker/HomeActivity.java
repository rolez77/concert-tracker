package com.example.concerttracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConcertAdapter adapter;
    private List<ConcertModel> concertList;

    // Opens AddConcertActivity and adds the saved concert to the top of the list
    private final ActivityResultLauncher<Intent> addConcertLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null) {
                    ConcertModel concert = new ConcertModel(
                            data.getStringExtra(AddConcertActivity.EXTRA_ARTIST),
                            data.getStringExtra(AddConcertActivity.EXTRA_DATE),
                            data.getStringExtra(AddConcertActivity.EXTRA_VENUE),
                            android.R.drawable.ic_menu_camera,
                            data.getStringExtra(AddConcertActivity.EXTRA_IMAGE_URI));
                    concertList.add(0, concert);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        recyclerView = findViewById(R.id.recyclerViewConcerts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        concertList = new ArrayList<>();
        concertList.add(new ConcertModel("Rihanna", "9/9/26", "Orlando Kia Center!!", android.R.drawable.ic_menu_camera));
        concertList.add(new ConcertModel("Beyonce", "7/14/25", "MetLife Stadium", android.R.drawable.ic_menu_camera));
        concertList.add(new ConcertModel("The Weeknd", "10/20/25", "SoFi Stadium", android.R.drawable.ic_menu_camera));

        adapter = new ConcertAdapter(concertList);
        recyclerView.setAdapter(adapter);

        Button btnLogifyIt = findViewById(R.id.btnLogifyIt);
        btnLogifyIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AddConcertActivity.class);
                // Use the launcher (not startActivity) so the saved concert comes back here
                addConcertLauncher.launch(intent);
            }
        });

        ImageButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> showFilterDialog());


        Button btnNavSetlist = findViewById(R.id.btnNavSetlist);
        btnNavSetlist.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, SetlistActivity.class));
        });

        Button btnNavGallery = findViewById(R.id.btnNavGallery);
        btnNavGallery.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, GalleryActivity.class));
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnFilterDate).setOnClickListener(v -> {
            Toast.makeText(this, "Filtered by Date", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnFilterArtist).setOnClickListener(v -> {
            Toast.makeText(this, "Filtered by Artist", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnFilterYear).setOnClickListener(v -> {
            Toast.makeText(this, "Filtered by Year", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}