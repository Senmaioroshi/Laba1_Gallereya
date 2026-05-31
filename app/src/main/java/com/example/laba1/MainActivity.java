package com.example.laba1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.database.Cursor;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_PERMISSION = 101;

    private ImageView imageView;
    private TextView counterText;

    private final ArrayList<Uri> photos = new ArrayList<>();
    private int currentIndex = 0;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private long lastSwitchTime = 0;
    private static final int SWITCH_DELAY = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        counterText = findViewById(R.id.counterText);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        checkPermissionAndLoadPhotos();
    }

    private void checkPermissionAndLoadPhotos() {
        String permission = Manifest.permission.READ_MEDIA_IMAGES;

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    REQUEST_PERMISSION
            );

        } else {
            loadPhotos();
        }
    }

    private void loadPhotos() {
        photos.clear();

        String[] projection = {MediaStore.Images.Media._ID};

        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                );
                photos.add(uri);
            }

            cursor.close();
        }

        if (photos.isEmpty()) {
            Toast.makeText(this, "Фото не найдены", Toast.LENGTH_SHORT).show();
            counterText.setText("Нет фото");
        } else {
            showPhoto();
        }
    }

    private void showPhoto() {
        imageView.setImageURI(photos.get(currentIndex));
        counterText.setText((currentIndex + 1) + " / " + photos.size());
    }

    private void nextPhoto() {
        if (photos.isEmpty()) return;

        currentIndex++;
        if (currentIndex >= photos.size()) {
            currentIndex = 0;
        }

        showPhoto();
    }

    private void previousPhoto() {
        if (photos.isEmpty()) return;

        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = photos.size() - 1;
        }

        showPhoto();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSwitchTime < SWITCH_DELAY) return;

        if (x > 5.5f) {
            previousPhoto();
            lastSwitchTime = currentTime;
        } else if (x < -5.5f) {
            nextPhoto();
            lastSwitchTime = currentTime;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (accelerometer != null) {
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPhotos();
        } else {
            Toast.makeText(this, "Нет доступа к фото", Toast.LENGTH_SHORT).show();
        }
    }
}
