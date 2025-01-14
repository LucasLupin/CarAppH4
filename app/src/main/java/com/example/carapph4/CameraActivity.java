package com.example.carapph4;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

public class CameraActivity extends AppCompatActivity {

    private static final String IMAGE_DIRECTORY = "CarAppImages";
    private ImageView imageView;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Button takePhotoButton;
    private Button savePhotoButton;
    private Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        takePhotoButton = findViewById(R.id.takePhotoButton);
        savePhotoButton = findViewById(R.id.savePhotoButton);
        resetButton = findViewById(R.id.resetButton);
        imageView = findViewById(R.id.capturedImageView);

        // Initialize the camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        displayCapturedImage();
                        switchToSaveResetButtons();
                    } else {
                        Toast.makeText(this, "Failed to capture image.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePhotoButton.setOnClickListener(v -> {
            if (!hasRequiredPermissions()) {
                requestRequiredPermissions();
            } else {
                captureHighResImage();
            }
        });

        savePhotoButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToFirebaseStorage(imageUri);
            } else {
                Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show();
            }
        });

        resetButton.setOnClickListener(v -> {
            resetToTakePictureButton();
            imageView.setImageBitmap(null);
            imageUri = null;
        });
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No WRITE_EXTERNAL_STORAGE needed on Android 10+
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    private void captureHighResImage() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "carapp_image_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + IMAGE_DIRECTORY);
            }

            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(cameraIntent);
                } else {
                    Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to create a file for the image.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error while launching camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void displayCapturedImage() {
        try {
            if (imageUri != null) {
                // Notify the media scanner about the new image
                getContentResolver().notifyChange(imageUri, null);

                // Open input stream to the image URI
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    // Decode the image stream into a Bitmap
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    // Correct the orientation if needed
                    Bitmap correctedBitmap = correctImageOrientation(bitmap);

                    // Display the corrected bitmap in the ImageView
                    imageView.setImageBitmap(correctedBitmap);
                    imageView.setVisibility(ImageView.VISIBLE);
                    Toast.makeText(this, "Image captured and displayed successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to load image input stream.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Image URI is null. Cannot display image.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to display image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap correctImageOrientation(Bitmap bitmap) throws IOException {
        if (imageUri != null) {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Bitmap correctedBitmap = bitmap;

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    correctedBitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    correctedBitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    correctedBitmap = rotateImage(bitmap, 270);
                    break;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return correctedBitmap;
        } else {
            return bitmap;
        }
    }

    private Bitmap rotateImage(Bitmap bitmap, int degrees) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void uploadImageToFirebaseStorage(Uri imageUri) {
        // Reference Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        // Define the file path in Firebase Storage
        String fileName = "images/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageReference.child(fileName);

        // Upload the image
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        saveImageUrlToDatabase(imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveImageUrlToDatabase(String imageUrl) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("images");

        String uniqueKey = databaseReference.push().getKey();

        if (uniqueKey != null) {
            databaseReference.child(uniqueKey).setValue(imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Image URL saved successfully in database.", Toast.LENGTH_SHORT).show();
                        resetToTakePictureButton();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void switchToSaveResetButtons() {
        takePhotoButton.setVisibility(Button.GONE);
        savePhotoButton.setVisibility(Button.VISIBLE);
        resetButton.setVisibility(Button.VISIBLE);
    }

    private void resetToTakePictureButton() {
        savePhotoButton.setVisibility(Button.GONE);
        resetButton.setVisibility(Button.GONE);
        takePhotoButton.setVisibility(Button.VISIBLE);
        imageView.setImageBitmap(null);
        imageUri = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted.", Toast.LENGTH_SHORT).show();
                captureHighResImage();
            } else {
                Toast.makeText(this, "Camera and storage permissions are required.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
