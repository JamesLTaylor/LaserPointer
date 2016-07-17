package com.cogn.laserpointer;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CalibrateActivity extends Activity implements View.OnClickListener {
    private String prefix;
    private Button okButton;
    private TextView textView;
    private SensorManager sensorMan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);

        findViewById(R.id.calibrate_btn_top).setOnClickListener(this);
        findViewById(R.id.calibrate_btn_bottom).setOnClickListener(this);
        findViewById(R.id.calibrate_btn_left).setOnClickListener(this);
        findViewById(R.id.calibrate_btn_right).setOnClickListener(this);

        textView = (TextView)findViewById(R.id.calibrate_text);
        okButton = (Button)findViewById(R.id.calibrate_btn_ok);
        okButton.setOnClickListener(this);
        okButton.setEnabled(false);

        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMan.registerListener(MainActivity.angleWatcher, accelerometer, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorMan.unregisterListener(MainActivity.angleWatcher);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.calibrate_btn_top):
                prefix = "t";
                okButton.setEnabled(true);
                textView.setText("Point to top of screen then press OK");
                break;
            case (R.id.calibrate_btn_bottom):
                prefix = "b";
                okButton.setEnabled(true);
                break;
            case (R.id.calibrate_btn_left):
                prefix = "l";
                okButton.setEnabled(true);
                break;
            case (R.id.calibrate_btn_right):
                prefix = "r";
                okButton.setEnabled(true);
                break;
            case (R.id.calibrate_btn_ok):
                new Thread(new Runnable() {
                    public void run() {
                        MainActivity.messageSender.requestMessageSend(prefix + Double.toString(MainActivity.angleWatcher.getGravityX()) + "," +
                                Double.toString(MainActivity.angleWatcher.getGravityY()));
                    }
                }).start();

                Toast toast = Toast.makeText(this, "Set for " + prefix, Toast.LENGTH_SHORT);
                toast.show();

                textView.setText(getResources().getString(R.string.press_locate_button));
                okButton.setEnabled(false);
                break;
        }
    }
}
