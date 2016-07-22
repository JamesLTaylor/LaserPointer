package com.cogn.laserpointer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.Calendar;

/**
 * Created by James on 6/23/2016.
 */
public class AngleWatcher implements SensorEventListener {
    private double yAvg, xAvg;
    long lastSend = 0;
    public boolean mustSend = false;
    private MainActivity mainActivity;

    public AngleWatcher(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        yAvg = 0.0f;
        xAvg = 0.0f;
    }

    public double getGravityX(){
        return xAvg;
    }

    public double getGravityY(){
        return yAvg;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            yAvg = yAvg*0.9 + 0.1*event.values[1];
            xAvg = xAvg*0.9 + 0.1*event.values[0];
            long now = Calendar.getInstance().getTimeInMillis();

            if (mustSend && (now-lastSend)>50) {
                lastSend = now;
                try {
                    MainActivity.messageSender.requestMessageSend(Double.toString(xAvg) + "," + Double.toString(yAvg));
                } catch (IllegalStateException e) {
                    mainActivity.stopSending();
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
