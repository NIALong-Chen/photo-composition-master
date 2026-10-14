package com.vinchen.photocomposition;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local face guidance remains optional; the user always controls the shutter. */
public final class MainActivity extends ComponentActivity {
    private PreviewView previewView;
    private TextView statusText;
    private Button permissionButton, lastPhotoButton, gridButton, guideButton;
    private ImageButton shutterButton;
    private View gridOverlay;
    private FaceOverlayView faceOverlay;
    private SharedPreferences preferences;
    private LifecycleCameraController controller;
    private FaceDetector detector;
    private final CompositionGuide guide = new CompositionGuide();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean gridEnabled, guidanceEnabled, cameraReady, captureInProgress, permissionBlocked;
    private int analysisGeneration;
    private long statusUntil;
    private Uri lastPhotoUri;
    private final Runnable staleResult = () -> {
        if (guidanceEnabled && cameraReady && !isDestroyed()) {
            faceOverlay.clear();
            guide.reset();
            showHint(CompositionGuide.Hint.WAITING);
        }
    };

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else showPermissionRequired();
            });
    private final ActivityResultLauncher<Intent> appSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (hasPermission()) startCamera();
                else showPermissionRequired();
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        // TextureView stays in the normal view hierarchy. On X300 the SurfaceView
        // preview can cover the portrait header despite its correct layout bounds.
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        statusText = findViewById(R.id.statusText);
        permissionButton = findViewById(R.id.permissionButton);
        lastPhotoButton = findViewById(R.id.lastPhotoButton);
        shutterButton = findViewById(R.id.shutterButton);
        gridButton = findViewById(R.id.gridButton);
        guideButton = findViewById(R.id.guideButton);
        gridOverlay = findViewById(R.id.gridOverlay);
        faceOverlay = findViewById(R.id.faceOverlay);
        preferences = getSharedPreferences("camera_preferences", MODE_PRIVATE);
        gridEnabled = preferences.getBoolean("grid_enabled", true);
        guidanceEnabled = preferences.getBoolean("guidance_enabled", true);
        restoreLastPhoto();
        updateControls();
        View root = findViewById(R.id.cameraRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        // Controller supplies viewport, rotation and sensor-to-preview transforms to ML Kit.
        controller = new LifecycleCameraController(this);
        controller.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        controller.setImageCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
        controller.setImageAnalysisBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
        controller.setImageAnalysisTargetSize(new CameraController.OutputSize(new Size(640, 480)));
        previewView.setController(controller);
        previewView.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (r-l != or-ol || b-t != ob-ot) {
                resetAnalysis();
                if (cameraReady && guidanceEnabled && detector != null) installAnalyzer();
            }
        });
        detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(.10f).enableTracking().build());
        gridButton.setOnClickListener(v -> {
            gridEnabled = !gridEnabled;
            preferences.edit().putBoolean("grid_enabled", gridEnabled).apply();
            updateControls();
        });
        guideButton.setOnClickListener(v -> {
            if (captureInProgress) return;
            guidanceEnabled = !guidanceEnabled;
            preferences.edit().putBoolean("guidance_enabled", guidanceEnabled).apply();
            statusUntil = 0;
            resetAnalysis();
            try {
                configureAnalysis();
                showStatus(guidanceEnabled ? "正在识别人脸…" : "指导已关闭 · 手动拍照");
            } catch (RuntimeException error) { fallbackToCamera(); }
            updateControls();
        });
        permissionButton.setOnClickListener(v -> {
            if (hasPermission()) startCamera();
            else if (permissionBlocked) appSettings.launch(new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            else {
                preferences.edit().putBoolean("permission_requested", true).apply();
                cameraPermission.launch(Manifest.permission.CAMERA);
            }
        });
        shutterButton.setOnClickListener(v -> takePhoto());
        lastPhotoButton.setOnClickListener(v -> openLastPhoto());
    }

    @Override protected void onResume() {
        super.onResume();
        restoreLastPhoto();
        if (hasPermission()) startCamera();
        else showPermissionRequired();
    }
    @Override protected void onStop() {
        resetAnalysis();
        controller.clearImageAnalysisAnalyzer();
        super.onStop();
    }
    @Override protected void onDestroy() {
        resetAnalysis();
        controller.clearImageAnalysisAnalyzer();
        controller.unbind();
        detector.close();
        super.onDestroy();
    }
    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }
    private void restoreLastPhoto() {
        String uri = preferences.getString("last_photo", null);
        lastPhotoUri = uri == null ? null : Uri.parse(uri);
        lastPhotoButton.setEnabled(lastPhotoUri != null);
    }
    private void showPermissionRequired() {
        cameraReady = false;
        resetAnalysis();
        controller.unbind();
        permissionBlocked = preferences.getBoolean("permission_requested", false)
                && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
        permissionButton.setText(permissionBlocked ? "打开应用设置" : "授予相机权限");
        permissionButton.setVisibility(View.VISIBLE);
        showStatus(permissionBlocked ? "请在系统设置中开启相机权限" : "允许使用相机后即可取景和拍照");
        updateControls();
    }
    private void startCamera() {
        if (isDestroyed() || captureInProgress) return;
        resetAnalysis();
        try {
            configureAnalysis();
            controller.bindToLifecycle(this);
            cameraReady = true;
            permissionButton.setVisibility(View.GONE);
            showStatus(guidanceEnabled ? "正在识别人脸…" : "相机已就绪 · 手动拍照");
        } catch (RuntimeException error) { fallbackToCamera(); }
        updateControls();
    }
    private void fallbackToCamera() {
        guidanceEnabled = false;
        resetAnalysis();
        try {
            controller.clearImageAnalysisAnalyzer();
            controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE);
            controller.bindToLifecycle(this);
            cameraReady = true;
            permissionButton.setVisibility(View.GONE);
            showStatus("指导暂不可用 · 可手动拍照");
        } catch (RuntimeException error) {
            cameraReady = false;
            permissionButton.setText("重试打开相机");
            permissionButton.setVisibility(View.VISIBLE);
            showStatus("相机启动失败，请重试");
        }
        updateControls();
    }
    private void resetAnalysis() {
        analysisGeneration++;
        handler.removeCallbacks(staleResult);
        faceOverlay.clear();
        guide.reset();
    }
    private void configureAnalysis() {
        controller.clearImageAnalysisAnalyzer();
        if (guidanceEnabled) installAnalyzer();
        controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE
                | (guidanceEnabled ? CameraController.IMAGE_ANALYSIS : 0));
    }
    private void installAnalyzer() {
        int generation = analysisGeneration;
        controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),
                new MlKitAnalyzer(Collections.singletonList(detector),
                        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        ContextCompat.getMainExecutor(this), result -> {
                    if (generation != analysisGeneration || !guidanceEnabled || !cameraReady
                            || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) return;
                    handler.removeCallbacks(staleResult);
                    handler.postDelayed(staleResult, 1500);
                    List<Face> faces = result.getValue(detector);
                    if (result.getThrowable(detector) != null || faces == null) {
                        faceOverlay.clear();
                        showHint(guide.update(CompositionGuide.Hint.ERROR, SystemClock.elapsedRealtime()));
                        return;
                    }
                    List<RectF> boxes = new ArrayList<>();
                    Face single = null;
                    int width = previewView.getWidth(), height = previewView.getHeight();
                    if (width <= 0 || height <= 0) return;
                    for (Face face : faces) {
                        RectF box = new RectF(face.getBoundingBox());
                        if (RectF.intersects(box, new RectF(0,0,width,height))) {
                            boxes.add(box);
                            single = face;
                        }
                    }
                    CompositionGuide.Hint raw;
                    if (boxes.size() != 1) raw = boxes.isEmpty()
                            ? CompositionGuide.Hint.NO_FACE : CompositionGuide.Hint.MULTIPLE;
                    else {
                        RectF box = boxes.get(0);
                        raw = CompositionGuide.evaluate(1, box.left/width, box.top/height,
                                box.right/width, box.bottom/height,
                                single.getHeadEulerAngleY(), single.getHeadEulerAngleZ());
                    }
                    CompositionGuide.Hint stable = guide.update(raw, SystemClock.elapsedRealtime());
                    faceOverlay.showFaces(boxes, stable == CompositionGuide.Hint.READY);
                    showHint(stable);
                }));
    }
    private void updateControls() {
        gridOverlay.setVisibility(gridEnabled && cameraReady ? View.VISIBLE : View.GONE);
        gridButton.setText(gridEnabled ? "网格：开" : "网格：关");
        guideButton.setText(guidanceEnabled ? "指导：开" : "指导：关");
        guideButton.setEnabled(cameraReady && !captureInProgress);
        shutterButton.setEnabled(cameraReady && !captureInProgress);
    }
    private void showStatus(String text) {
        statusText.setTextColor(Color.WHITE);
        statusText.setText(text);
    }
    private void showHint(CompositionGuide.Hint hint) {
        if (captureInProgress || SystemClock.elapsedRealtime() < statusUntil) return;
        if (!hint.text.contentEquals(statusText.getText())) statusText.setText(hint.text);
        statusText.setTextColor(hint == CompositionGuide.Hint.READY
                ? Color.rgb(102,230,150) : Color.WHITE);
    }
    private void takePhoto() {
        if (!cameraReady || captureInProgress) return;
        captureInProgress = true;
        updateControls();
        showStatus("正在保存照片…");
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "PhotoMaster_" + stamp + ".jpg");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/PhotoMaster");
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build();
        try {
            controller.takePicture(options, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(ImageCapture.OutputFileResults result) {
                    Uri saved = result.getSavedUri();
                    if (saved != null) preferences.edit().putString("last_photo", saved.toString()).apply();
                    finishCapture(saved != null ? "已保存到系统相册" : "已保存，请在系统相册查看");
                    if (!isDestroyed()) restoreLastPhoto();
                }
                @Override public void onError(ImageCaptureException error) {
                    finishCapture("保存失败，请重试");
                }
            });
        } catch (RuntimeException error) { finishCapture("拍照失败，请重试"); }
    }
    private void finishCapture(String message) {
        captureInProgress = false;
        if (isDestroyed() || isFinishing()) return;
        statusUntil = SystemClock.elapsedRealtime() + 1800;
        showStatus(message);
        updateControls();
    }
    private void openLastPhoto() {
        if (lastPhotoUri == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(lastPhotoUri, "image/jpeg")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "无法打开照片，请在系统相册查看", Toast.LENGTH_SHORT).show();
        }
    }
}
