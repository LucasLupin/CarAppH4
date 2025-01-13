package com.example.carapph4;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import android.graphics.Matrix;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KameraActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private PreviewView previewView;
    private TextView detectedTextView;
    private ImageView capturedImageView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kamera);

        // Handle system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        previewView = findViewById(R.id.previewView);
        detectedTextView = findViewById(R.id.detectedText);
        capturedImageView = findViewById(R.id.capturedImageView);

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            setupCamera();
        }

        // Set up the capture button
        findViewById(R.id.takePictureButton).setOnClickListener(v -> takePicture());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if permission is not granted
            }
        }
    }

    private void setupCamera() {
        // Initialize CameraX
        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Bind the camera lifecycle
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        // Select back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Configure the preview
        androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder()
                .setTargetResolution(new Size(1920, 1080))
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Configure ImageCapture
        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1920, 1080))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.getDisplay().getRotation()) // Ensure correct rotation
                .build();

        // Bind everything to the lifecycle
        Camera camera = cameraProvider.bindToLifecycle(
                this, // LifecycleOwner
                cameraSelector,
                preview,
                imageCapture
        );

        Log.d("KameraActivity", "Camera successfully bound.");
    }

    private void takePicture() {
        if (imageCapture == null) {
            Toast.makeText(this, "ImageCapture is not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = new File(getExternalCacheDir(), "captured_image.jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("KameraActivity", "Image saved at: " + photoFile.getAbsolutePath() + ", File size: " + photoFile.length());

                        if (photoFile.exists() && photoFile.length() > 0) {
                            Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                            Bitmap rotatedBitmap = rotateBitmapIfNeeded(originalBitmap, photoFile.getAbsolutePath());

                            // Scale the bitmap to fit the ImageView dimensions
                            int maxWidth = capturedImageView.getWidth();
                            int maxHeight = capturedImageView.getHeight();
                            Bitmap scaledBitmap = scaleBitmap(rotatedBitmap, maxWidth, maxHeight);

                            if (scaledBitmap != null) {
                                runOnUiThread(() -> {
                                    previewView.setVisibility(PreviewView.GONE);
                                    capturedImageView.setVisibility(ImageView.VISIBLE);
                                    capturedImageView.setImageBitmap(scaledBitmap);
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(KameraActivity.this, "Failed to decode or scale the image file.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(KameraActivity.this, "Image file is empty or not written.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("KameraActivity", "Image capture failed: " + exception.getMessage());
                        Toast.makeText(KameraActivity.this, "Failed to capture image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to handle rotation
    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e("KameraActivity", "Error rotating bitmap: " + e.getMessage());
            return bitmap;
        }
    }

    // Helper method to scale the bitmap
    private Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > 1) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
