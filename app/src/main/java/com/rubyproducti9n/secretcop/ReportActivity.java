package com.rubyproducti9n.secretcop;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReportActivity extends BaseActivity {

    private static final int PICK_MEDIA_REQUEST = 1;
    private Button submitBtn;
    private EditText descriptionInput;
    private AutoCompleteTextView violationSpinner;
    private RecyclerView mediaRecycler;
    private List<Uri> mediaUris = new ArrayList<>();
    private MediaAdapter mediaAdapter;
    private ProgressDialog progressDialog;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private boolean useMailgunApi = false; // ✅ Set true for API, false for Intent
    private static final String MAILGUN_API_URL = "https://api.mailgun.net/v3/YOUR_DOMAIN_NAME/messages";
    private static final String MAILGUN_API_KEY = "api:YOUR_API_KEY";
    ChipGroup chipGroup;
    private File attachedMediaFile;
    private EditText edtDescription;
    private Button btnSubmit;
    private static final int CAMERA_IMAGE_REQUEST = 102;
    private Uri cameraMediaUri;
    List<String> allViolations = Arrays.asList(
            "Littering", "Noise Pollution", "Illegal Parking", "Encroachment", "Vandalism", "Other"
    );
    private ActivityResultLauncher<Intent> emailLauncher;
    private static final Map<String, String> violationMessages = new HashMap<>();

    static {
        violationMessages.put("Illegal Parking",
                "• A vehicle has been parked illegally, blocking traffic and posing safety hazards.");
        violationMessages.put("Garbage Dumping",
                "• Unauthorized garbage dumping has been observed, creating unsanitary conditions.");
        violationMessages.put("Noise Pollution",
                "• Loud and persistent noise has been reported, possibly exceeding permissible levels.");
        violationMessages.put("Water Wastage",
                "• Water is being wasted due to unattended leaks or overflows.");
        violationMessages.put("Encroachment",
                "• Encroachment on public property is restricting free movement and usage.");
        violationMessages.put("Open Manhole",
                "• An open manhole has been found, posing serious danger to pedestrians and vehicles.");
        violationMessages.put("Blocked Footpath",
                "• Footpaths are obstructed, forcing pedestrians to walk on the road.");
        violationMessages.put("Streetlight Not Working",
                "• A streetlight is non-functional, reducing visibility and increasing security risks.");
    }
    Set<String> selectedViolationsList = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        submitBtn = findViewById(R.id.submitBtn);
        descriptionInput = findViewById(R.id.descriptionInput);
        violationSpinner = findViewById(R.id.violationDropdown);
        chipGroup = findViewById(R.id.violationChipGroup);
        mediaRecycler = findViewById(R.id.mediaRecycler);

        mediaAdapter = new MediaAdapter(mediaUris);
        mediaRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaRecycler.setAdapter(mediaAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        Set<String> selectedViolations = new HashSet<>();
        List<String> dropdownOptions = new ArrayList<>(allViolations);

// Initial adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                dropdownOptions
        );
        violationSpinner.setAdapter(adapter);

// On selection
        violationSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = adapter.getItem(position);
            if (selected != null && !selectedViolations.contains(selected)) {
                selectedViolations.add(selected);
                selectedViolationsList.add(selected);
                addChip(selected, chipGroup, selectedViolations, adapter);
                updateDropdownOptions(adapter, selectedViolations);
            }
            violationSpinner.setText(""); // clear dropdown text after selection
        });

        findViewById(R.id.attachMediaBtn).setOnClickListener(v -> showMediaPickerDialog());
        submitBtn.setOnClickListener(v -> {
            try {
                validateAndSubmit();
            } catch (UnirestException e) {
                throw new RuntimeException(e);
            }
        });

        updateSubmitState();

        emailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Gmail is closed or user came back (whether they sent or not)

                    // 1. Reset UI state
                    mediaUris.clear();
                    mediaAdapter.notifyDataSetChanged();
                    descriptionInput.setText(""); // reset text input if any
                    updateSubmitState();

                    // 2. Reward points
                    addPointsToUser();

                    // 3. Redirect to MainActivity
                    Intent intent = new Intent(ReportActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(this, "Report submitted! You've earned +5 points 🎉", Toast.LENGTH_SHORT).show();
                }
        );

    }
    private String buildCustomReportMessage(Set<String> selectedViolations, String city, double latitude, double longitude) {
        StringBuilder message = new StringBuilder();

        // 1. Format current date and time
        String dateTime = new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date());

        // 2. Intro
        message.append("Respected Authorities,\n\n");
        message.append("I am reporting the following civic violations observed in ")
                .append(city != null ? city : "my locality")
                .append(" on ").append(dateTime).append(":\n\n");

        // 3. Add each violation message
        // Add dynamic violation messages
        for (String violation : selectedViolations) {
            switch (violation) {
                case "Littering":
                    message.append("• Littering observed in a public place.\n");
                    break;
                case "Noise Pollution":
                    message.append("• Excessive noise beyond permissible hours.\n");
                    break;
                case "Illegal Parking":
                    message.append("• Vehicle parked illegally, blocking access.\n");
                    break;
                case "Public Drinking":
                    message.append("• Alcohol consumption observed in public space.\n");
                    break;
                default:
                    message.append("• " + descriptionInput.getText().toString()).append(violation).append("\n");
            }
        }

        // 4. Location section
        message.append("\nReported Location:\n");
        message.append("📍 https://www.google.com/maps?q=")
                .append(latitude).append(",").append(longitude).append("\n");

        // 5. Closing
        message.append("\nPlease find attached media proof for your reference.\n\n");
        message.append("Thank you for your prompt attention.\n\n");
        message.append("Regards,\nA concerned citizen");

        return message.toString();
    }


    private void addChip(String text, ChipGroup chipGroup, Set<String> selectedSet, ArrayAdapter<String> adapter) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
//        chip.setChipBackgroundColorResource(R.color.m3_sys_color_surface); // Optional M3 theming

        chip.setOnCloseIconClickListener(v -> {
            // 1. Remove chip
            chipGroup.removeView(chip);
            // 2. Remove from selected set
            selectedSet.remove(text);

            selectedViolationsList.remove(selectedSet);

            // 3. Update dropdown options
            updateDropdownOptions(adapter, selectedSet);
            // 4. Force dropdown to rebind and show updated list
            AutoCompleteTextView dropdown = findViewById(R.id.violationDropdown);
            dropdown.setText(""); // Clear any previous input
            dropdown.dismissDropDown(); // Hide dropdown to prevent glitch
//            dropdown.postDelayed(dropdown::showDropDown, 150); // Force refresh dropdown with delay
        });

        chipGroup.addView(chip);
    }
    private void autoSendReportBasedOnLocation(Uri attachmentUri) {

        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String city = getCityFromLocation(location);
                if (city != null) {
                    String[] authorityEmails = getAuthorityEmailForCity(city);
                    String message = buildCustomReportMessage(selectedViolationsList, city, location.getLatitude(), location.getLongitude());

                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("application/octet-stream");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, authorityEmails);
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Violation Report from " + city);
                    emailIntent.putExtra(Intent.EXTRA_TEXT, message);
                    emailIntent.setPackage("com.google.android.gm");

                    if (attachmentUri != null) {
                        emailIntent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
                        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    // Manually resolve Gmail compose activity (see previous response)
                    List<ResolveInfo> matches = getPackageManager().queryIntentActivities(emailIntent, 0);
                    for (ResolveInfo info : matches) {
                        if (info.activityInfo.packageName.equals("com.google.android.gm") &&
                                info.activityInfo.name.contains("ComposeActivityGmail")) {
                            emailIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                            break;
                        }
                    }

                    startActivity(emailIntent);
                } else {
                    Toast.makeText(this, "Unable to detect your city.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Location not found. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String[] getAuthorityEmailForCity(String city) {
        Map<String, String[]> cityEmailMap = new HashMap<>();

        cityEmailMap.put("Mumbai", new String[]{
                "comdir.ud@maharashtra.gov.in",
                "dycollectorlandacqmum@gmail.com"
        });
        cityEmailMap.put("Pune", new String[]{
                "info@punecorporation.org", "feedback@punecorporation.org"
        });
        cityEmailMap.put("Hyderabad", new String[]{
                "mayor.ghmc@gov.in", "commissioner-ghmc@gov.in"
        });
        cityEmailMap.put("Shirdi", new String[]{
                "saibaba@sai.org.in", "addcoll@ahmednagar.maharashtra.gov.in"
        });
        cityEmailMap.put("Kopargaon", new String[]{
                "sdo-shirdi@maharashtra.gov.in"
        });

        return cityEmailMap.getOrDefault(city, new String[]{"defaultauthority@email.com"});
    }

    private String getCityFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getLocality(); // returns city like "Mumbai", "Pune"
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateDropdownOptions(ArrayAdapter<String> adapter, Set<String> selectedSet) {
        List<String> newOptions = new ArrayList<>();
        for (String option : allViolations) {
            if (!selectedSet.contains(option)) {
                newOptions.add(option);
            }
        }
        adapter.clear();
        adapter.addAll(newOptions);
        adapter.notifyDataSetChanged();
    }

    private void sendReportViaIntent() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("application/octet-stream");
//        emailIntent.setType("message/rfc822"); // Best for email apps
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"authority@example.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Violation Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Here is the attached media proof");
        emailIntent.setPackage("com.google.android.gm");

        if (attachedMediaFile != null) {
            Uri fileUri = FileProvider.getUriForFile(this, "com.rubyproducti9n.secretcop.provider", attachedMediaFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            emailLauncher.launch(emailIntent); // ← Modern replacement for startActivity()

            // Do NOT reset here. Wait until launcher callback
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "Gmail app not found. Please install Gmail.", Toast.LENGTH_LONG).show();
        }

    }
    private void showMediaPickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Choose Media")
                .setItems(new String[]{"Camera", "Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        openCameraIntent();
                    } else {
                        pickMediaFromGallery();
                    }
                }).show();
    }

    private void pickMediaFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select media"), PICK_MEDIA_REQUEST);
    }

    private void openCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createTempMediaFile();
            if (photoFile != null) {
                attachedMediaFile = photoFile;
                cameraMediaUri = FileProvider.getUriForFile(
                        this,
                        "com.rubyproducti9n.secretcop.provider", // ✅ Your file provider authority
                        photoFile
                );
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraMediaUri);
                startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
            }
        }
    }

    private File createTempMediaFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(fileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == PICK_MEDIA_REQUEST && data != null) {
            mediaUris.clear();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    mediaUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else {
                Uri singleUri = data.getData();
                if (singleUri != null) {
                    mediaUris.add(singleUri);
                }
            }
            attachedMediaFile = uriToFile(mediaUris.get(0)); // Save first file for intent email
            mediaAdapter.notifyDataSetChanged();
            updateSubmitState();
        } else if (requestCode == CAMERA_IMAGE_REQUEST && attachedMediaFile != null && cameraMediaUri != null) {
            mediaUris.clear();
            mediaUris.add(cameraMediaUri);
            mediaAdapter.notifyDataSetChanged();
            updateSubmitState();
        }
    }

    private File uriToFile(Uri uri) {
        try {
            // 1. Get file extension from uri or MIME type
            String extension = getFileExtension(uri);
            if (extension == null) extension = "dat"; // fallback if no ext

            // 2. Create a properly named file with correct extension
            String fileName = "upload_" + System.currentTimeMillis() + "." + extension;
            File tempFile = new File(getCacheDir(), fileName);

            // 3. Copy data from uri to new file
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }

            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private String getFileExtension(Uri uri) {
        String extension = null;

        // Handle content:// URIs
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getContentResolver();

            // Try MIME type first
            String mime = cr.getType(uri);
            if (mime != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            }

            // If MIME didn't work, fallback to file name via cursor
            if (extension == null) {
                Cursor cursor = cr.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        String name = cursor.getString(nameIndex);
                        if (name != null && name.contains(".")) {
                            extension = name.substring(name.lastIndexOf('.') + 1);
                        }
                    }
                    cursor.close();
                }
            }
        }

        // Handle file:// URIs
        else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null && path.contains(".")) {
                extension = path.substring(path.lastIndexOf('.') + 1);
            }
        }

        return extension;
    }



    private void updateSubmitState() {
        submitBtn.setEnabled(!mediaUris.isEmpty());
    }

    private void validateAndSubmit() throws UnirestException {
        String desc = descriptionInput.getText().toString().trim();
        if (desc.length() < 10) {
            descriptionInput.setError("Please provide a detailed description.");
            return;
        }
        progressDialog.setMessage("Submitting report...");
        progressDialog.show();
        if (useMailgunApi) {
            sendReportToMailgun(desc, "violationSpinner.getSelectedItem().toString()", mediaUris);
        } else {
            autoSendReportBasedOnLocation(mediaUris.get(0));
//            sendReportViaIntent();
        }
    }

    private void sendReportToMailgun(String desc, String violation, List<Uri> uris) throws UnirestException {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("from", "CivicApp <om.lokhande34@gmail.com>")
                .addFormDataPart("to", "om.lokhande34@gmail.com")
                .addFormDataPart("subject", "New Violation Report: " + violation)
                .addFormDataPart("text", "User: " + currentUser.getUid() + "\nDescription: " + desc);

        try {
            for (Uri uri : uris) {
                InputStream is = getContentResolver().openInputStream(uri);
                byte[] bytes = readBytes(is);
                String name = "media_" + System.currentTimeMillis() + ".jpg";
                builder.addFormDataPart("attachment", name,
                        RequestBody.create(MediaType.parse(Objects.requireNonNull(getContentResolver().getType(uri))), bytes));
            }
        } catch (Exception e) { e.printStackTrace(); }

        Request request = new Request.Builder()
                .url("https://api.mailgun.net/v3/sandbox6fec8cb072124d689e2b662615b26de5.mailgun.org/messages")
                .post(builder.build())
                .header("Authorization", Credentials.basic("api", BuildConfig.MAILGUN_API_KEY))
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportActivity.this, "Report failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        userRef.child("points").runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                Integer pts = currentData.getValue(Integer.class);
                                currentData.setValue((pts == null ? 0 : pts) + 5);
                                return Transaction.success(currentData);
                            }
                            @Override public void onComplete(@Nullable DatabaseError e, boolean c, @Nullable DataSnapshot d) {}
                        });
                        Toast.makeText(ReportActivity.this, "Report submitted!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ReportActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Log.d("Mailgun error", "Error " + response.message());
                        Toast.makeText(ReportActivity.this, "Mailgun error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int n;
        byte[] data = new byte[4096];
        while ((n = in.read(data)) != -1) buf.write(data, 0, n);
        return buf.toByteArray();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressDialog.dismiss();
    }

    public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<Uri> mediaList;

        public MediaAdapter(List<Uri> mediaList) {
            this.mediaList = mediaList;
        }

        @Override
        public int getItemViewType(int position) {
            Uri uri = mediaList.get(position);
            String type = uri.toString().toLowerCase();
            if (type.contains("mp4") || type.contains("video")) {
                return 1; // Video
            }
            return 0; // Image
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 1) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
                return new VideoViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
                return new ImageViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Uri uri = mediaList.get(position);

            if (holder instanceof ImageViewHolder) {
                Picasso.get().load(uri).fit().centerCrop().into(((ImageViewHolder) holder).imageView);
            } else if (holder instanceof VideoViewHolder) {
                ((VideoViewHolder) holder).videoView.setVideoURI(uri);
                ((VideoViewHolder) holder).videoView.seekTo(100); // Just for thumbnail preview
            }
        }

        @Override
        public int getItemCount() {
            return mediaList.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.mediaImage);
            }
        }

        class VideoViewHolder extends RecyclerView.ViewHolder {
            VideoView videoView;

            VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                videoView = itemView.findViewById(R.id.mediaVideo);
            }
        }
    }
}