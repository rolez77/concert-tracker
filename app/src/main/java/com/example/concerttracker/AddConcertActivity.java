package com.example.concerttracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class AddConcertActivity extends AppCompatActivity {

    // Keys for the data sent back to the screen that opened this one
    public static final String EXTRA_ARTIST = "extra_artist";
    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_VENUE = "extra_venue";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    private static final String STATE_IMAGE_URI = "state_image_uri";

    private ImageView ivConcertImage;
    private EditText etArtist, etDate, etVenue;
    private Uri selectedImageUri;

    // Opens the system photo picker and shows the chosen picture
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    // Keep access to the photo after the app restarts (not supported on every device)
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                    setImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_concert);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivConcertImage = findViewById(R.id.ivConcertImage);
        etArtist = findViewById(R.id.etArtist);
        etDate = findViewById(R.id.etDate);
        etVenue = findViewById(R.id.etVenue);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnAddPicture = findViewById(R.id.btnAddPicture);
        Button btnSaveConcert = findViewById(R.id.btnSaveConcert);

        // Restore the picked picture after a screen rotation
        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_IMAGE_URI);
            if (saved != null) {
                setImage(Uri.parse(saved));
            }
        }

        btnBack.setOnClickListener(v -> finish());

        btnAddPicture.setOnClickListener(v -> openImagePicker());
        ivConcertImage.setOnClickListener(v -> openImagePicker());

        etDate.setOnClickListener(v -> showDatePicker());

        btnSaveConcert.setOnClickListener(v -> saveConcert());
    }

    private void openImagePicker() {
        pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void setImage(Uri uri) {
        selectedImageUri = uri;
        ivConcertImage.setPadding(0, 0, 0, 0);
        ivConcertImage.setImageURI(uri);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // Same format as the existing concerts, e.g. 9/9/26
                    String date = (month + 1) + "/" + dayOfMonth + "/" + String.format("%02d", year % 100);
                    etDate.setText(date);
                    etDate.setError(null);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveConcert() {
        String artist = etArtist.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();

        boolean valid = true;
        if (TextUtils.isEmpty(artist)) {
            etArtist.setError("Enter the artist");
            valid = false;
        }
        if (TextUtils.isEmpty(date)) {
            etDate.setError("Pick a date");
            valid = false;
        }
        if (TextUtils.isEmpty(venue)) {
            etVenue.setError("Enter the stadium");
            valid = false;
        }
        if (!valid) {
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_ARTIST, artist);
        result.putExtra(EXTRA_DATE, date);
        result.putExtra(EXTRA_VENUE, venue);
        if (selectedImageUri != null) {
            result.putExtra(EXTRA_IMAGE_URI, selectedImageUri.toString());
        }
        setResult(RESULT_OK, result);

        Toast.makeText(this, "Concert saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedImageUri != null) {
            outState.putString(STATE_IMAGE_URI, selectedImageUri.toString());
        }
    }
}
