package com.example.liverecognition;

import android.net.Uri;
import android.Manifest;
import android.content.Intent;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.annotation.SuppressLint;
import android.provider.OpenableColumns;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;

import android.app.Activity;
import android.app.AlertDialog;

import android.util.Size;
import android.util.DisplayMetrics;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.OptIn;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import androidx.camera.view.TransformExperimental;
import androidx.camera.view.transform.OutputTransform;

import androidx.camera.core.Preview;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;

import androidx.camera.view.PreviewView;
import androidx.camera.lifecycle.ProcessCameraProvider;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import com.luxand.FSDK;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final boolean SHOW_FPS = true;
    private static final int FPS_WINDOW_SIZE = 32;
    private static final double FPS_MOVING_ALPHA = 2. / (FPS_WINDOW_SIZE + 1);

    /** The number of bytes for copying images for matching. */
    private static final int FILE_TRANSFER_SIZE = 4096;

    /** Set the size used for image analysis. Lower values increase performance, but decrease accuracy. */
    private static final Size imageAnalysisTargetSize = new Size(640, 480);

    /** Ensure FSDK Tracker is only loaded once. */
    private static boolean facesProcessorLoaded = false;

    private static final int permissionsRequestCode = 355;

    /** Newer Android versions require different sets of permission to access user images. */
    private static final String[] permissionsRequired;
    static {
        final var sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            permissionsRequired = new String[]{ Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED };
        else if (sdkVersion == Build.VERSION_CODES.TIRAMISU)
            permissionsRequired = new String[]{ Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES };
        else
            permissionsRequired = new String[]{ Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE };
    }

    /** Frame size to request for camera preview. */
    private Size targetSize;

    private FacesView facesView;
    private TextView fpsTextView;
    private PreviewView previewView;

    /** Face detection runs on a separate execution thread to increase the overall app performance. */
    private ExecutorService analysisExecutor;

    private ProcessCameraProvider cameraProvider;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    /** File to save tracker memory to. */
    private File facesFile;

    private double fps = -1;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fpsTextView = findViewById(R.id.fps_text);
        fpsTextView.setEnabled(SHOW_FPS);

        facesView = findViewById(R.id.faces_view);
        previewView = findViewById(R.id.preview_view);
        analysisExecutor = Executors.newSingleThreadExecutor();
        targetSize = getScreenDimensions();

        findViewById(R.id.flip_button).setOnClickListener((button) -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;

            restartCamera();
        });

        final var liveness_button = this.<Button>findViewById(R.id.liveness_button);
        liveness_button.setText(FacesProcessor.isLivenessEnabled() ? R.string.liveness_on : R.string.liveness_off);
        liveness_button.setOnClickListener((button) -> liveness_button.setText(FacesProcessor.toggleLiveness() ? R.string.liveness_on : R.string.liveness_off));

        findViewById(R.id.match_button).setOnClickListener((button) -> {
            final var chooseImage = new Intent(Intent.ACTION_GET_CONTENT);
            chooseImage.setType("image/*");
            chooseImage.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{ "image/jpeg", "image/png", "image/bmp" });
            chooseImage.addCategory(Intent.CATEGORY_OPENABLE);

            imageSelectionResultLauncher.launch(Intent.createChooser(chooseImage, getResources().getString(R.string.choose_image)));
        });

        findViewById(R.id.clear_button).setOnClickListener((button) -> FacesProcessor.clear());

        facesView.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                var face = facesView.getFaceContainingPoint(event.getX(), event.getY());
                if (face != null) {
                    var name = face.lockName();

                    var builder = new AlertDialog.Builder(this);
                    var inflater = getLayoutInflater();
                    builder.setTitle(R.string.set_name);

                    var dialogLayout = inflater.inflate(R.layout.edit_name_dialog, null);
                    var editText = dialogLayout.<EditText>findViewById(R.id.edit_name);

                    var oldName = name.get();
                    if (!oldName.isEmpty())
                        editText.setText(oldName);

                    builder.setView(dialogLayout);
                    builder.setPositiveButton(R.string.ok, (dialogInterface, which) -> name.setAndUnlock(editText.getText().toString()));

                    builder.setNegativeButton(R.string.cancel, ((dialogInterface, which) -> {}));

                    builder.show();
                }

                v.performClick();
            }

            return true;
        });

        if (!facesProcessorLoaded) {
            /* Initilize FacesProcessor before calling any of the class methods */
            if (!FacesProcessor.initialize(getApplication(), getCacheDir().getAbsolutePath()))
                showError(R.string.activation_error, true);

            /* Tracker memory file is saved in the app's data directory. */
            facesFile = new File(getExternalFilesDir(null), "tracker.bin");
            if (!FacesProcessor.load(facesFile))
                showError(R.string.wrong_detection_version);

            facesProcessorLoaded = true;
        }

        startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        analysisExecutor.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();

        FacesProcessor.save(facesFile);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != permissionsRequestCode)
            return;

        if (shouldRequestPermissions()) {
            showError(R.string.no_permissions, true);
            return;
        }

        startCamera();
    }

    private String formatString(final String format, final Object... args) {
        return String.format(Locale.getDefault(), format, args);
    }

    private String formatString(@StringRes final int format, final Object... args) {
        return formatString(getResources().getString(format), args);
    }

    private void showError(@StringRes final int messageID) {
        showError(messageID, false);
    }

    private void showError(@StringRes final int messageID, final boolean finishOnClose) {
        var builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_title);
        builder.setMessage(messageID);

        if (finishOnClose) {
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.ok, (dialogInterface, which) -> finish());
        }

        builder.show();
    }

    /** FSDK cannot load images from content URIs. If the image provided by user has a content path copy it to the cache directory. */
    private String copyImageIfContent(@NonNull final Uri uri) {
        final var scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            try (final var cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst())
                    return null;

                final var filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

                @SuppressLint("UnsanitizedFilenameFromContentProvider")
                final var destinationFile = new File(getCacheDir(), filename); // the filename is obtained from image selection screen, no need to sanitize the path provided directly by android.

                try (final var inputStream = getContentResolver().openInputStream(uri);
                     final var outputStream = new FileOutputStream(destinationFile)) {
                    if (inputStream == null)
                        return null;

                    int bytesRead;
                    final var buffer = new byte[FILE_TRANSFER_SIZE];
                    while ((bytesRead = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, bytesRead);

                } catch (IOException e) {
                    return null;
                }

                return destinationFile.getAbsolutePath();
            }
        }

        return uri.getPath();
    }

    private final ActivityResultLauncher<Intent> imageSelectionResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        (result) -> {
            if (result.getResultCode() != Activity.RESULT_OK)
                return;

            final var data = result.getData();
            if (data == null || data.getData() == null)
                return;

            final var imagePath = copyImageIfContent(data.getData());
            if (imagePath == null) {
                showError(R.string.image_read_error);
                return;
            }

            final var face = FacesProcessor.matchFace(imagePath);
            final var builder = new AlertDialog.Builder(this);
            if (face.isError()) {
                builder.setTitle(R.string.error_title);
                if (face.getError() == FSDK.FSDKE_FACE_NOT_FOUND)
                    builder.setMessage(R.string.face_not_found_error);
                else
                    builder.setMessage(formatString(R.string.error, face.getError()));
            } else if (!face.hasMatch()) {
                builder.setTitle(R.string.no_match_title);
                builder.setMessage(R.string.no_match_error);
            } else {
                builder.setTitle(R.string.match_title);
                builder.setMessage(formatString(R.string.face_found, face.getName(), face.getID(), face.getSimilarity()));
            }

            builder.setPositiveButton(R.string.ok, ((dialogInterface, which) -> {}));
            builder.show();
        }
    );

    private Size getScreenDimensions() {
        var displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    private boolean shouldRequestPermissions() {
        for (String permission : permissionsRequired) {
            if (permission.equals(Manifest.permission.READ_MEDIA_IMAGES) && ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                /* Read media images permission is not granted if the user selected partial access to images starting from API Level 34. */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    continue;

                return true;
            }

            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED)
                return true;
        }

        return false;
    }

    private void restartCamera() {
        /* Reset transformation matrix in case screen or camera dimensions have changed. */
        facesView.setFacesTransform(null);
        cameraProvider.unbindAll();
        startCamera();
    }

    private void startCamera() {
        if (shouldRequestPermissions()) {
            ActivityCompat.requestPermissions(this, permissionsRequired, permissionsRequestCode);
            return;
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            bindPreview(cameraProvider);
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("RestrictedApi")
    @OptIn(markerClass = TransformExperimental.class)
    private void bindPreview(final ProcessCameraProvider cameraProvider) {
        final var resolutionSelector = new ResolutionSelector.Builder()
            .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
            .setResolutionStrategy(new ResolutionStrategy(targetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
            .build();

        final var preview = new Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build();

        final var cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        final var imageAnalysisResolutionSelector = new ResolutionSelector.Builder()
            .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
            .setResolutionStrategy(new ResolutionStrategy(imageAnalysisTargetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
            .build();

        final var imageAnalysis = new ImageAnalysis.Builder()
            .setResolutionSelector(imageAnalysisResolutionSelector)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(analysisExecutor, (imageProxy) -> {
            /* Obtain the transformation matrix from image space to screen space to properly display face boxes on screen. */
            if (facesView.getFacesTransform() == null) {
                runOnUiThread(() -> {
                    final var source = imageProxy.getImageInfo().getSensorToBufferTransformMatrix();

                    final OutputTransform outputTransform = previewView.getOutputTransform();
                    if (outputTransform != null) {
                        final var target = outputTransform.getMatrix();
                        final var matrix = new Matrix();
                        source.invert(matrix);

                        final var values = new float[9];
                        source.getValues(values);

                        final var lengthX = Math.sqrt(values[0] * values[0] + values[3] * values[3]);
                        final var lengthY = Math.sqrt(values[1] * values[1] + values[4] * values[4]);

                        final var angle  = Math.round(Math.acos(values[0] / lengthX) / Math.PI * 180) % 180;
                        final var imageAngle = imageProxy.getImageInfo().getRotationDegrees();

                        matrix.postScale((float)lengthX, (float)lengthY);
                        if (imageAngle == 90 || imageAngle == 270)
                            matrix.postScale(-1, -1);

                        matrix.postRotate(imageAngle);

                        if (angle >= 45 && angle <= 135)
                            matrix.postScale(2.f / imageProxy.getHeight(), 2.f / imageProxy.getWidth());
                        else
                            matrix.postScale(2.f / imageProxy.getWidth(), 2.f / imageProxy.getHeight());

                        switch (imageAngle) {
                            case 0:
                                matrix.postTranslate(-1, -1);
                                break;
                            case 90:
                                matrix.postTranslate(-1, 1);
                                break;
                            case 180:
                                matrix.postTranslate(1, 1);
                                break;
                            case 270:
                                matrix.postTranslate(1, -1);
                                break;
                        }

                        matrix.postConcat(target);

                        facesView.setFacesTransform(matrix);
                    }
                });

                imageProxy.close();
                fps = -1;
                return;
            }

            final var time = System.nanoTime();
            facesView.setDetectionResult(FacesProcessor.accept(imageProxy));

            if (SHOW_FPS) {
                final var newFPS = 1000000000 / (double)(System.nanoTime() - time);
                fps = fps > 0
                    ? newFPS * FPS_MOVING_ALPHA + fps * (1 - FPS_MOVING_ALPHA)
                    : newFPS;

                runOnUiThread(() -> fpsTextView.setText(String.format(Locale.getDefault(), "%.1f", fps)));
            }

            imageProxy.close();
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }
}
