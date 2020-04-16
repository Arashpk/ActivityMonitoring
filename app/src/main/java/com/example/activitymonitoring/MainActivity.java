package com.example.activitymonitoring;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EasyPermissions.PermissionCallbacks {
    int STORAGE_PERMISSION_CODE = 1;
    int daqSPEED = 1;
    // Graph
    LineGraphSeries<DataPoint> series;
    GraphView graph;
    //sensor variables
    float accelerometer_X_value = 0, accelerometer_Y_value = 0, accelerometer_Z_value = 0;
    float gyro_X_value = 0, gyro_Y_value = 0, gyro_Z_value = 0;
    float magno_X_value = 0, magno_Y_value = 0, magno_Z_value = 0;
    // sample count. is used for the isAnalysing mode
    int sampleCount = 0;
    int windowLength = 200;
    int Fs = 100;
    float signal[] = new float[1000];
    // Radio
    RadioGroup radiogroupDAQ;
    RadioGroup radiogroupRecord;

    RadioButton radiobuttonDAQ;
    RadioButton radiobuttonRecord;
    // latest time stamp
    long latest_time_ms;
    // Defines the file name
    String FILE_NAME = "Recordings";
    FileOutputStream fos = null;
    private static final byte isRecordingRadio = 3;
    private static final byte isAnalysing = 2;
    private static final byte isRecording = 1;
    private static final byte isNotRecording = 0;
    byte recordingState = isNotRecording;
    // Defines the Senssor manager
    private android.hardware.SensorManager SensorManager;
    // Individual light and proximity sensors.
    private Sensor SensorAccelerometer;
    private Sensor SensorGyro;
    private Sensor SensorMagno;

    // TextViews to display current sensor values
    private TextView TextSensorAccelerometer_X;
    private TextView TextSensorAccelerometer_Y;
    private TextView TextSensorAccelerometer_Z;
    private TextView TextSensorGyro_X;
    private TextView TextSensorGyro_Y;
    private TextView TextSensorGyro_Z;
    private TextView TextSensorMagno_X;
    private TextView TextSensorMagno_Y;
    private TextView TextSensorMagno_Z;
    private TextView TextSensorMagno_Z_rotation;

    String strDataAcc = "";
    String strDataGyro = "";
    String strDataMagno = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        graph = (GraphView) findViewById(R.id.graph);
        SensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensorList = SensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sensorText = new StringBuilder();

        radiogroupDAQ = findViewById(R.id.radioGroupDAQ);
        radiogroupRecord = findViewById(R.id.radioGroupRecordAnalyze);

        TextSensorAccelerometer_X = (TextView) findViewById(R.id.label_AccelerometerX);
        TextSensorAccelerometer_Y = (TextView) findViewById(R.id.label_AccelerometerY);
        TextSensorAccelerometer_Z = (TextView) findViewById(R.id.label_AccelerometerZ);

        TextSensorGyro_X = (TextView) findViewById(R.id.label_GyroX);
        TextSensorGyro_Y = (TextView) findViewById(R.id.label_GyroY);
        TextSensorGyro_Z = (TextView) findViewById(R.id.label_GyroZ);

        TextSensorMagno_X = (TextView) findViewById(R.id.label_MagnoX);
        TextSensorMagno_Y = (TextView) findViewById(R.id.label_MagnoY);
        TextSensorMagno_Z = (TextView) findViewById(R.id.label_MagnoZ);

        SensorAccelerometer = SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SensorGyro = SensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SensorMagno = SensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        String sensor_error = getResources().getString(R.string.error_no_sensor);

        if (SensorAccelerometer == null) {
            TextSensorAccelerometer_X.setText(sensor_error);
            TextSensorAccelerometer_Y.setText(sensor_error);
            TextSensorAccelerometer_Z.setText(sensor_error);
        }
        if (SensorGyro == null) {
            TextSensorGyro_X.setText(sensor_error);
            TextSensorGyro_Y.setText(sensor_error);
            TextSensorGyro_Z.setText(sensor_error);
        }
        if (SensorMagno == null) {
            TextSensorMagno_X.setText(sensor_error);
            TextSensorMagno_Y.setText(sensor_error);
            TextSensorMagno_Z.setText(sensor_error);
            TextSensorMagno_Z_rotation.setText(sensor_error);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if ((SensorAccelerometer != null) && (SensorGyro != null) && (SensorMagno != null)) {
            switch (daqSPEED) {
                case 0:
                    SensorManager.registerListener(this, SensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                    SensorManager.registerListener(this, SensorGyro, SensorManager.SENSOR_DELAY_FASTEST);
                    SensorManager.registerListener(this, SensorMagno, SensorManager.SENSOR_DELAY_FASTEST);
                    break;

                case 1:
                    SensorManager.registerListener(this, SensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
                    SensorManager.registerListener(this, SensorGyro, SensorManager.SENSOR_DELAY_GAME);
                    SensorManager.registerListener(this, SensorMagno, SensorManager.SENSOR_DELAY_GAME);
                    break;

                case 2:
                    SensorManager.registerListener(this, SensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                    SensorManager.registerListener(this, SensorGyro, SensorManager.SENSOR_DELAY_GAME);
                    SensorManager.registerListener(this, SensorMagno, SensorManager.SENSOR_DELAY_GAME);
                    break;

                case 3:
                    SensorManager.registerListener(this, SensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
                    SensorManager.registerListener(this, SensorGyro, SensorManager.SENSOR_DELAY_NORMAL);
                    SensorManager.registerListener(this, SensorMagno, SensorManager.SENSOR_DELAY_NORMAL);
                    break;

            }

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
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometer_X_value = sensorEvent.values[0];
                accelerometer_Y_value = sensorEvent.values[1];
                accelerometer_Z_value = sensorEvent.values[2];
                TextSensorAccelerometer_X.setText(getResources().getString(R.string.label_accelerometer_x, accelerometer_X_value));
                TextSensorAccelerometer_Y.setText(getResources().getString(R.string.label_accelerometer_y, accelerometer_Y_value));
                TextSensorAccelerometer_Z.setText(getResources().getString(R.string.label_accelerometer_z, accelerometer_Z_value));
                strDataAcc = String.valueOf(accelerometer_X_value) + "," + String.valueOf(accelerometer_Y_value) + "," + String.valueOf(accelerometer_Z_value);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyro_X_value = sensorEvent.values[0];
                gyro_Y_value = sensorEvent.values[1];
                gyro_Z_value = sensorEvent.values[2];
                TextSensorGyro_X.setText(getResources().getString(R.string.label_gyro_x, gyro_X_value));
                TextSensorGyro_Y.setText(getResources().getString(R.string.label_gyro_y, gyro_Y_value));
                TextSensorGyro_Z.setText(getResources().getString(R.string.label_gyro_z, gyro_Z_value));
                strDataGyro = String.valueOf(gyro_X_value) + "," + String.valueOf(gyro_Y_value) + "," + String.valueOf(gyro_Z_value);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magno_X_value = sensorEvent.values[0];
                magno_Y_value = sensorEvent.values[1];
                magno_Z_value = sensorEvent.values[2];
                TextSensorMagno_X.setText(getResources().getString(R.string.label_magno_x, magno_X_value));
                TextSensorMagno_Y.setText(getResources().getString(R.string.label_magno_y, magno_Y_value));
                TextSensorMagno_Z.setText(getResources().getString(R.string.label_magno_z, magno_Z_value));
                strDataMagno = String.valueOf(magno_X_value) + "," + String.valueOf(magno_Y_value) + "," + String.valueOf(magno_Z_value);
                break;
            default:
                // do nothing
        }
        String text = "";
        long time_ms = System.currentTimeMillis();
        if (latest_time_ms != time_ms) { //to stop duplicated time stamp
            latest_time_ms = time_ms;
            text = String.valueOf(time_ms) + "," + strDataAcc + "," + strDataGyro + "," + strDataMagno + "\n";
            switch (recordingState) {
                case isRecording:
                    try {
                        fos.write(text.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case isAnalysing:

                    signal[sampleCount] = accelerometer_X_value;
                    sampleCount++;
                    if (sampleCount >= windowLength) {
                        sampleCount = 0;
                        series = new LineGraphSeries<DataPoint>();
                        for (int i = 0; i < windowLength; i++) {
                            series.appendData(new DataPoint(i, signal[i]), true, windowLength);
                        }
                        graph.removeAllSeries();
                        graph.addSeries(series);

                        //run the fft and stuff
                    }
                    //series.appendData(new DataPoint(sampleCount, accelerometer_X_value), true, windowLength);
                    break;
                case isRecordingRadio:
                    break;
                default:
                    break;
            }

        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private boolean isExternalStorageWritable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.i("State", "Yes it is writable");
            return true;
        } else {
            return false;
        }
    }

    // @AfterPermissionGranted(123)
    public void startRecordingButtonClick(View v) {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "has permissions", Toast.LENGTH_SHORT).show();
            FILE_NAME = fileNAmeGenerator();
            if (isExternalStorageWritable()) {
                File textFile = new File(Environment.getExternalStorageDirectory(), FILE_NAME);
                try {
                    //fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
                    fos = new FileOutputStream(textFile);
                    recordingState = isRecording;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, "Recording at" + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();
            }

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

    String fileNAmeGenerator() {
        Calendar calendar = Calendar.getInstance();
        String currentDate = DateFormat.getDateInstance().format(calendar.getTime());
        SimpleDateFormat format = new SimpleDateFormat("HH_mm_ss");
        String time = format.format(calendar.getTime());
        String finalFileName = currentDate + "_" + time + "_" + ".csv";
        return finalFileName;
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

    public void checkRadioButton(View v) {
        int radioId = radiogroupDAQ.getCheckedRadioButtonId();
        radiobuttonDAQ = findViewById(radioId);
        String radioText = radiobuttonDAQ.getText().toString();
        //SensorManager.unregisterListener(this);
        switch (radioText) {
            case "DAQ Fastest":
                daqSPEED = 0;
                break;
            case "DAQ Gaming":
                daqSPEED = 1;
                break;
            case "DAQ Normal":
                daqSPEED = 2;
                break;
            case "DAQ UI":
                daqSPEED = 3;
                break;
            default:
                daqSPEED = 0;
                break;

        }
        Toast.makeText(this, "Selected DAQ speed is " + radioText + " -- " + String.valueOf(daqSPEED), Toast.LENGTH_SHORT).show();
    }

    public void checkRadioButtonRecordingOrAnalyze(View v) {
        Button button;
        int radioId = radiogroupRecord.getCheckedRadioButtonId();
        radiobuttonRecord = findViewById(radioId);
        String radioText = radiobuttonRecord.getText().toString();
        switch (radioText) {
            case "Analyze":
                recordingState = isAnalysing;
                button = findViewById(R.id.button_record);
                button.setEnabled(false);
                button = findViewById(R.id.button_stop);
                button.setEnabled(false);
                break;
            case "Record":
                recordingState = isRecordingRadio;
                button = findViewById(R.id.button_record);
                button.setEnabled(true);
                button = findViewById(R.id.button_stop);
                button.setEnabled(true);
                break;
            default:
                recordingState = isRecordingRadio;
                break;

        }
        Toast.makeText(this, "Analyze or not - recordingState: " + radioText + " -- " + String.valueOf(recordingState), Toast.LENGTH_SHORT).show();
    }
}

