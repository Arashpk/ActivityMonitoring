package com.example.activitymonitoring;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    int FEATURE_LENGTH = 12, SAMPLE_LENGTH = 118;
    double[] realTimeFeature = new double[FEATURE_LENGTH];
    double[][] trainedSampleTable = new double[SAMPLE_LENGTH][FEATURE_LENGTH];
    String[] categoryArray = new String[SAMPLE_LENGTH];
    List<String> ActivityTypes = Arrays.asList("downstairs", "jogging", "running", "standing", "upstairs", "walking");
    ArrayList<String> ActivityDetectedRecordings;
    ArrayAdapter<String> arrayAdapter;
    ListView listView;
    // Signal features
    float[] acc_X_array = new float[windowLength];
    float[] acc_Y_array = new float[windowLength];
    float[] acc_Z_array = new float[windowLength];
    double acc_X_DC = 0, acc_Y_DC = 0, acc_Z_DC = 0;
    double acc_X_variance = 0, acc_Y_variance = 0, acc_Z_variance = 0;
    double acc_X_std = 0, acc_Y_std = 0, acc_Z_std = 0;
    // functions
    functions Functions = new functions();
    // Radio
    RadioGroup radiogroupDAQ;
    RadioGroup radiogroupRecord;

    RadioButton radiobuttonRecord;
    // latest time stamp
    long latest_time_ms;
    // Defines the file name
    String FILE_NAME = "Recordings";
    FileOutputStream fos = null;
    byte isConfusionFileCreated = 0;
    private static final byte isRecordingRadio = 3;
    private static final byte isAnalysing = 2;
    private static final byte isRecording = 1;
    private static final byte isNotRecording = 0;
    private static final byte isIdle = -1;
    byte systemState = isIdle;
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
    private TextView TextDetectedActivity;

    String strDataAcc = "";
    String strDataGyro = "";
    String strDataMagno = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityDetectedRecordings = new ArrayList<String>();
        TextDetectedActivity = (TextView) findViewById(R.id.label_Activity);
        TextDetectedActivity.setMovementMethod(new ScrollingMovementMethod());
        //
        graph = (GraphView) findViewById(R.id.graph);
        SensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensorList = SensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sensorText = new StringBuilder();

        TextSensorAccelerometer_X = (TextView) findViewById(R.id.label_AccelerometerX);
        TextSensorAccelerometer_Y = (TextView) findViewById(R.id.label_AccelerometerY);
        TextSensorAccelerometer_Z = (TextView) findViewById(R.id.label_AccelerometerZ);


        SensorAccelerometer = SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SensorGyro = SensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SensorMagno = SensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        String sensor_error = getResources().getString(R.string.error_no_sensor);

        if (SensorAccelerometer == null) {
            TextSensorAccelerometer_X.setText(sensor_error);
            TextSensorAccelerometer_Y.setText(sensor_error);
            TextSensorAccelerometer_Z.setText(sensor_error);
        }
        readReferenceCSV();
    }

    private void readReferenceCSV() {
        InputStream is = getResources().openRawResource(R.raw.reference);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8"))
        );
        String line = "";
        int offset = 2;
        int i = 0;

        try {
            for (int sampleLine = 0; sampleLine < SAMPLE_LENGTH; sampleLine++) {
                // row offset
                while (i < offset) {
                    i++;
                    line = reader.readLine();
                }
                // Split by ','
                String[] tokens = line.split(",");
                // Read the Category
                categoryArray[sampleLine] = tokens[0];
                // Read the table data
                for (int Col = 0; Col < 12; Col++) {
                    double currentFeature = Double.parseDouble(tokens[Col + 1]);
                    trainedSampleTable[sampleLine][Col] = currentFeature;
                }
                TrainingSample sample = new TrainingSample();
                line = reader.readLine();

            }
            //Log.d("My Activity", "Just created trainign sample");
        } catch (IOException e) {
            Log.wtf("MyActivity", "Error reading file on line " + line, e);
            e.printStackTrace();
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
                    strDataGyro = String.valueOf(gyro_X_value) + "," + String.valueOf(gyro_Y_value) + "," + String.valueOf(gyro_Z_value);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magno_X_value = sensorEvent.values[0];
                    magno_Y_value = sensorEvent.values[1];
                    magno_Z_value = sensorEvent.values[2];
                    strDataMagno = String.valueOf(magno_X_value) + "," + String.valueOf(magno_Y_value) + "," + String.valueOf(magno_Z_value);
                    break;
                default:
                    // do nothing
            }
            String text = "";
            latest_time_ms = time_ms;
            text = String.valueOf(time_ms) + "," + strDataAcc + "," + strDataGyro + "," + strDataMagno + "\n";
            switch (systemState) {
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

    void signalPlot() {
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
    }

    public void analyze(View v) {
        Timer analyzeTimer = new Timer();
        TimerTask analyzeTask = new TimerTask() {
            @Override
            public void run() {
                if (systemState == isAnalysing) {
                    acc_X_array[sampleCount] = accelerometer_X_value;
                    acc_Y_array[sampleCount] = accelerometer_Y_value;
                    acc_Z_array[sampleCount] = accelerometer_Z_value;
                    sampleCount++;
                    if (sampleCount >= windowLength) {
                        sampleCount = 0;
                        Functions.SetVars(windowLength, Fs, FEATURE_LENGTH, SAMPLE_LENGTH,
                                acc_X_array, acc_Y_array, acc_Z_array, realTimeFeature,
                                trainedSampleTable, categoryArray);

                        //Signal plot
                        signalPlot();
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat format = new SimpleDateFormat("HH_mm_ss");
                        String time = format.format(calendar.getTime());


                        String Activity = Functions.categorize();
                        String gotText = TextDetectedActivity.getText().toString();
                        TextDetectedActivity.setText(time + " -- " + Activity + "\n" + gotText);
                        try {
                            fos.write((Functions.getVotingArrayEU().toString() + "\n").getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        analyzeTimer.schedule(analyzeTask, 0, 1000 / Fs);
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
        systemState = isRecording;
        streamInit();
    }

    void streamInit() {
        //initiates a file to record or analyze
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "has permissions", Toast.LENGTH_SHORT).show();
            FILE_NAME = Functions.fileNAmeGenerator();
            switch (systemState) {
                case isRecording:
                    FILE_NAME = "Rec_" + FILE_NAME;
                    break;
                case isAnalysing:
                    FILE_NAME = "ConfMatrix_" + FILE_NAME;
                    break;
            }

            if (isExternalStorageWritable()) {
                File textFile = new File(Environment.getExternalStorageDirectory(), FILE_NAME);
                try {
                    //fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
                    fos = new FileOutputStream(textFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    systemState = isIdle;
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

    public void stopAnalyzeButtonClick(View v) {
        if (fos != null) {
            try {
                fos.close();
                Toast.makeText(this, "Stopped recording the " + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error. Cannot Stop", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        systemState = isIdle;
    }

    public void startAnalyzeButtonClick(View v) {
        systemState = isAnalysing;
        analyze(v);
        streamInit();
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

