package com.example.activitymonitoring;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EasyPermissions.PermissionCallbacks {
    int STORAGE_PERMISSION_CODE = 1;
    // Defines the file name
    private static final String FILE_NAME = "Recordings";
    FileOutputStream fos = null;
    private static final byte isRecording = 1;
    private static final byte isNotRecording = 0;
    byte recordingState = isNotRecording;
    // Defines the Senssor manager
    private android.hardware.SensorManager SensorManager;
    // Individual light and proximity sensors.
    private Sensor SensorAccelerometer;
    // TextViews to display current sensor values
    private TextView TextSensorAccelerometer_X;
    private TextView TextSensorAccelerometer_Y;
    private TextView TextSensorAccelerometer_Z;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = SensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sensorText = new StringBuilder();

        TextSensorAccelerometer_X = (TextView) findViewById(R.id.label_AccelerometerX);
        TextSensorAccelerometer_Y = (TextView) findViewById(R.id.label_AccelerometerY);
        TextSensorAccelerometer_Z = (TextView) findViewById(R.id.label_AccelerometerZ);

        SensorAccelerometer = SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        String sensor_error = getResources().getString(R.string.error_no_sensor);

        if (SensorAccelerometer == null) {
            TextSensorAccelerometer_X.setText(sensor_error);
            TextSensorAccelerometer_Y.setText(sensor_error);
            TextSensorAccelerometer_Z.setText(sensor_error);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (SensorAccelerometer != null) {
            SensorManager.registerListener(this, SensorAccelerometer,
                    //SensorManager.SENSOR_DELAY_NORMAL);
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        float accelerometer_X_value = sensorEvent.values[0];
        float accelerometer_Y_value = sensorEvent.values[1];
        float accelerometer_Z_value = sensorEvent.values[2];
        //String text = String.valueOf(12) + ";" + String.valueOf(50) + ";" + String.valueOf(12);
        String text = "";

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                TextSensorAccelerometer_X.setText(getResources().getString(R.string.label_accelerometer_x, accelerometer_X_value));
                TextSensorAccelerometer_Y.setText(getResources().getString(R.string.label_accelerometer_y, accelerometer_Y_value));
                TextSensorAccelerometer_Z.setText(getResources().getString(R.string.label_accelerometer_z, accelerometer_Z_value));
                long time_ms = System.currentTimeMillis();
                Log.i("recording state", String.valueOf(recordingState));
                text = String.valueOf(time_ms) + ","
                        + String.valueOf(accelerometer_X_value) + ","
                        + String.valueOf(accelerometer_Y_value) + ","
                        + String.valueOf(accelerometer_Z_value) + "\n";
                if (recordingState == isRecording) {
                    try {
                        fos.write(text.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {

                }
                break;
            default:
                // do nothing
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // @AfterPermissionGranted(123)
    public void startRecordingButtonClick(View v) {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "has permissions", Toast.LENGTH_SHORT).show();
            Calendar calendar = Calendar.getInstance();
            String currentDate = DateFormat.getDateInstance().format(calendar.getTime());
            try {
                fos = openFileOutput(currentDate + "_" + FILE_NAME + ".csv", MODE_PRIVATE);
                recordingState = isRecording;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "Recording at" + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();


        } else {
            EasyPermissions.requestPermissions(this, "I need your permission", 123, perms);
        }


    }

    public void stopRecordingButtonClick(View v) {

        if (fos != null) {
            try {
                fos.close();
                Toast.makeText(this, "Stopped recording the " + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error. Cannot Stop", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "has permissions", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {

        }
    }
}
