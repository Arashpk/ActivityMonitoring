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
import com.paramsen.noise.Noise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EasyPermissions.PermissionCallbacks {
    int STORAGE_PERMISSION_CODE = 1;
    int daqSPEED = 1;
    // Graph
    LineGraphSeries<DataPoint> seriesX;
    LineGraphSeries<DataPoint> seriesY;
    LineGraphSeries<DataPoint> seriesZ;
    LineGraphSeries<DataPoint> series_FFT_X;
    LineGraphSeries<DataPoint> series_FFT_Y;
    LineGraphSeries<DataPoint> series_FFT_Z;

    GraphView graph, graphFFT;
    //sensor variables
    float accelerometer_X_value = 0, accelerometer_Y_value = 0, accelerometer_Z_value = 0;
    float gyro_X_value = 0, gyro_Y_value = 0, gyro_Z_value = 0;
    float magno_X_value = 0, magno_Y_value = 0, magno_Z_value = 0;
    // sample count. is used for the isAnalysing mode
    int sampleCount = 0;
    int windowLength = 50;
    int Fs = 40;
    // Signal features
    float acc_X_array[] = new float[windowLength];
    float acc_Y_array[] = new float[windowLength];
    float acc_Z_array[] = new float[windowLength];
    double acc_X_DC = 0, acc_Y_DC = 0, acc_Z_DC = 0;
    double acc_X_variance = 0, acc_Y_variance = 0, acc_Z_variance = 0;
    double acc_X_std = 0,acc_Y_std = 0,acc_Z_std = 0;
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
        graphFFT = (GraphView) findViewById(R.id.graphFFT);
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
        long time_ms = System.currentTimeMillis();
        if (latest_time_ms != time_ms) { //to stop duplicated time stamp
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
                    break;
                case isRecordingRadio:
                    break;
                default:
                    break;
            }

        } else {
        }
    }

    public void analyze(View v) {
        Timer analyzeTimer = new Timer();
        TimerTask analyzTask = new TimerTask() {
            @Override
            public void run() {
                if (recordingState == isAnalysing) {
                    acc_X_array[sampleCount] = accelerometer_X_value;
                    acc_Y_array[sampleCount] = accelerometer_Y_value;
                    acc_Z_array[sampleCount] = accelerometer_Z_value;
                    sampleCount++;
                    if (sampleCount >= windowLength) {
                        sampleCount = 0;
                        // DC detection
                        for (int i = 0; i < windowLength; i++) {
                            acc_X_DC = acc_X_DC + acc_X_array[i];
                            acc_Y_DC = acc_Y_DC + acc_Y_array[i];
                            acc_Z_DC = acc_Z_DC + acc_Z_array[i];
                        }
                        acc_X_DC = acc_X_DC / Double.valueOf(windowLength);
                        acc_Y_DC = acc_Y_DC / Double.valueOf(windowLength);
                        acc_Z_DC = acc_Z_DC / Double.valueOf(windowLength);
                        //Log.i("DC", String.valueOf(acc_X_DC) + " - " + String.valueOf(acc_Y_DC) + " - " + String.valueOf(acc_Z_DC) + " - ");
                        // variance
                        for (int i = 0; i < windowLength; i++) {
                            acc_X_variance = acc_X_variance + Math.pow(acc_X_array[i] - acc_X_DC, 2);
                            acc_Y_variance = acc_Y_variance + Math.pow(acc_Y_array[i] - acc_Y_DC, 2);
                            acc_Z_variance = acc_Z_variance + Math.pow(acc_Z_array[i] - acc_Z_DC, 2);
                        }
                        acc_X_variance = acc_X_variance / (Double.valueOf(windowLength) - 1);
                        acc_Y_variance = acc_Y_variance / (Double.valueOf(windowLength) - 1);
                        acc_Z_variance = acc_Z_variance / (Double.valueOf(windowLength) - 1);
                        // std
                        acc_X_std = Math.sqrt(acc_X_variance);
                        acc_Y_std = Math.sqrt(acc_Y_variance);
                        acc_Z_std = Math.sqrt(acc_Z_variance);
                        Log.i("STD", String.valueOf(acc_X_std) + " - " + String.valueOf(acc_Y_std) + " - " + String.valueOf(acc_Z_std) + " - ");
                        //Signal plot
                        seriesX = new LineGraphSeries<DataPoint>();
                        seriesY = new LineGraphSeries<DataPoint>();
                        seriesZ = new LineGraphSeries<DataPoint>();
                        for (int i = 0; i < windowLength; i++) {
                            seriesX.appendData(new DataPoint(i, acc_X_array[i]), true, windowLength);
                            seriesY.appendData(new DataPoint(i, acc_Y_array[i]), true, windowLength);
                            seriesZ.appendData(new DataPoint(i, acc_Z_array[i]), true, windowLength);
                        }

                        graph.removeAllSeries();
                        graph.addSeries(seriesX);
                        graph.addSeries(seriesY);
                        graph.addSeries(seriesZ);
                        FrequencyDetection();
                        //run the fft and stuff
                    }
                    //series.appendData(new DataPoint(sampleCount, accelerometer_X_value), true, windowLength);
                }
            }
        };
        analyzeTimer.schedule(analyzTask, 0, 1000 / Fs);
    }

    public void FrequencyDetection() {
        Noise noise = Noise.real(windowLength);
        float[] fft_acc_X = new float[windowLength + 2]; //real output length equals src+2
        float[] fft_acc_Y = new float[windowLength + 2]; //real output length equals src+2
        float[] fft_acc_Z = new float[windowLength + 2]; //real output length equals src+2

        float[] fft_X = noise.fft(acc_X_array, fft_acc_X);
        float[] fft_Y = noise.fft(acc_Y_array, fft_acc_Y);
        float[] fft_Z = noise.fft(acc_Z_array, fft_acc_Z);
        double[] mag_acc_X = new double[windowLength + 2];
        double[] mag_acc_Y = new double[windowLength + 2];
        double[] mag_acc_Z = new double[windowLength + 2];

        for (int i = 0; i < fft_X.length / 2; i++) {
            float real_X = fft_X[i * 2];
            float real_Y = fft_Y[i * 2];
            float real_Z = fft_Z[i * 2];

            float imaginary_X = fft_X[i * 2 + 1];
            float imaginary_Y = fft_Y[i * 2 + 1];
            float imaginary_Z = fft_Z[i * 2 + 1];

            mag_acc_X[i] = Math.sqrt(Math.pow(real_X, 2) + Math.pow(imaginary_X, 2)) / windowLength;
            mag_acc_Y[i] = Math.sqrt(Math.pow(real_Y, 2) + Math.pow(imaginary_Y, 2)) / windowLength;
            mag_acc_Z[i] = Math.sqrt(Math.pow(real_Z, 2) + Math.pow(imaginary_Z, 2)) / windowLength;
        }
        series_FFT_X = new LineGraphSeries<DataPoint>();
        series_FFT_Y = new LineGraphSeries<DataPoint>();
        series_FFT_Z = new LineGraphSeries<DataPoint>();
        double acc_X_Frq_max = 0, acc_X_Mag_max = 0;
        double acc_Y_Frq_max = 0, acc_Y_Mag_max = 0;
        double acc_Z_Frq_max = 0, acc_Z_Mag_max = 0;

        for (int i = 1; i < windowLength / 2; i++) {
            series_FFT_X.appendData(new DataPoint(i, mag_acc_X[i]), true, windowLength / 2);
            series_FFT_Y.appendData(new DataPoint(i, mag_acc_Y[i]), true, windowLength / 2);
            series_FFT_Z.appendData(new DataPoint(i, mag_acc_Z[i]), true, windowLength / 2);

            if (mag_acc_X[i] > acc_X_Mag_max) {
                acc_X_Mag_max = mag_acc_X[i];
                acc_X_Frq_max = ((i * Fs) / Double.valueOf(windowLength));
            }
            if (mag_acc_Y[i] > acc_Y_Mag_max) {
                acc_Y_Mag_max = mag_acc_Y[i];
                acc_Y_Frq_max = ((i * Fs) / Double.valueOf(windowLength));
            }
            if (mag_acc_Z[i] > acc_Z_Mag_max) {
                acc_Z_Mag_max = mag_acc_Z[i];
                acc_Z_Frq_max = ((i * Fs) / Double.valueOf(windowLength));
            }
        }
        //Log.i("frq", String.valueOf(acc_X_Mag_max) + "  " + String.valueOf(acc_X_Frq_max));
        //Log.i("frq", String.valueOf(acc_Y_Mag_max) + "  " + String.valueOf(acc_Y_Frq_max));
        //Log.i("frq", String.valueOf(acc_Z_Mag_max) + "  " + String.valueOf(acc_Z_Frq_max));
        graphFFT.removeAllSeries();
        graphFFT.addSeries(series_FFT_X);
        graphFFT.addSeries(series_FFT_Y);
        graphFFT.addSeries(series_FFT_Z);

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
        analyze(v);
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

