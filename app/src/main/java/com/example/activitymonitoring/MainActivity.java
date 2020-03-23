package com.example.activitymonitoring;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
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

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                TextSensorAccelerometer_X.setText(getResources().getString(R.string.label_accelerometer_x, accelerometer_X_value));
                TextSensorAccelerometer_Y.setText(getResources().getString(R.string.label_accelerometer_y, accelerometer_Y_value));
                TextSensorAccelerometer_Z.setText(getResources().getString(R.string.label_accelerometer_z, accelerometer_Z_value));
                break;
            default:
                // do nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
