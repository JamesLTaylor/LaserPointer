package com.cogn.laserpointer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.concurrent.TimeoutException;

public class MainActivity extends Activity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnTouchListener {

    public static final String TAG = "POINTER";
    private static final String PREF_SERVER_ADDR1 = "com.cogn.laserpointer.PREF_SERVER_ADDR1";
    private static final String PREF_SERVER_ADDR2 = "com.cogn.laserpointer.PREF_SERVER_ADDR2";
    private static final String PREF_SERVER_ADDR3 = "com.cogn.laserpointer.PREF_SERVER_ADDR3";
    private static final String PREF_SERVER_ADDR4 = "com.cogn.laserpointer.PREF_SERVER_ADDR4";

    // Objects to be shared with other activities
    public static AngleWatcher angleWatcher;
    public static MessageSender messageSender;

    private SensorManager sensorMan;
    private String SERVER_ADDRESS;
    private SharedPreferences mPrefs;
    private Button mainButton;



    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        angleWatcher = new AngleWatcher(this);
        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        ((TextView)findViewById(R.id.server_address1)).setText(mPrefs.getString(PREF_SERVER_ADDR1, "192"));
        ((TextView)findViewById(R.id.server_address2)).setText(mPrefs.getString(PREF_SERVER_ADDR2, "168"));
        ((TextView)findViewById(R.id.server_address3)).setText(mPrefs.getString(PREF_SERVER_ADDR3, "1"));
        ((TextView)findViewById(R.id.server_address4)).setText(mPrefs.getString(PREF_SERVER_ADDR4, "104"));

        setServerAddress(true);

        findViewById(R.id.btn_set_server).setOnClickListener(this);

        ((Switch)findViewById(R.id.sw_screen_on)).setOnCheckedChangeListener(this);
        ((Switch)findViewById(R.id.sw_ctrl)).setOnCheckedChangeListener(this);

        mainButton = (Button)findViewById(R.id.btn_start);
        mainButton.setOnClickListener(this);

        findViewById(R.id.btn_pg_up).setOnClickListener(this);
        findViewById(R.id.btn_pg_dwn).setOnClickListener(this);
        findViewById(R.id.btn_click).setOnTouchListener(this);

        messageSender = new MessageSender(this);
        messageSender.setAddress(SERVER_ADDRESS);
        messageSender.start();

	}

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMan.registerListener(angleWatcher, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorMan.unregisterListener(angleWatcher);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        messageSender.stop();
    }


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
        if (id==R.id.action_calibrate) {
            angleWatcher.mustSend = false;

            Intent intent = new Intent(this, CalibrateActivity.class);
            startActivity(intent);
            return true;
        }
		if (id == R.id.action_settings) {
			return true;
		}
        if (id == R.id.action_test) {
            testTime();
            return true;
        }
		if (id == R.id.Exit) {
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    /**
     *
     */
    public void stopSending()
    {
        angleWatcher.mustSend = false;
        updateButton();
        try {
            messageSender.sendMessageAndWait("cn");
        }
        catch (TimeoutException | IllegalStateException e)
        {
        }
    }

    private void updateButton(){
        if (angleWatcher.mustSend) {
            mainButton.setBackground(getResources().getDrawable(R.drawable.round_button_red));
            mainButton.setText("Stop");
        } else {
            mainButton.setBackground(getResources().getDrawable(R.drawable.round_button_green));
            mainButton.setText("Start");
        }
    }

    public void showDialogGeneralError(String title, String message)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.show();
    }

    public void showDialogTestError() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("No response");
        dialog.setMessage("No server responded from the provided IP address.  Please try the following:" +
                "\n\nCheck that a LaserPointer server is running on your computer" +
                "\n\nMake sure your phone and computer are connected to the same network" +
                "\n\nCheck the address displayed on the server" +
                "\n\nType this into the IP address area above" +
                "");
        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.show();
    }

    private void showDialogTest(long timeInMilis) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("response time");
        dialog.setMessage(Html.fromHtml("<b>" + Long.toString(timeInMilis) + "ms</b>"));
        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.show();
    }

    private void testTime(){
        long startTime = Calendar.getInstance().getTimeInMillis();
        try {
            messageSender.sendMessageAndWait("z0,0");
        } catch (TimeoutException e)  {
            showDialogGeneralError("Could not send","Could not send the message, make sure nothing else is sending");
            return;
        } catch (IllegalStateException e) {
            showDialogTestError();
            return;
        }

        final long elapsedTime = Calendar.getInstance().getTimeInMillis() - startTime;
        if (elapsedTime>1000) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showDialogTestError();
                }
            });
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showDialogTest(elapsedTime);
                }
            });
        }

    }

    private void setServerAddress(boolean firstCall){
        String serverAddr1 = ((EditText) findViewById(R.id.server_address1)).getText().toString();
        String serverAddr2 = ((EditText) findViewById(R.id.server_address2)).getText().toString();
        String serverAddr3 = ((EditText) findViewById(R.id.server_address3)).getText().toString();
        String serverAddr4 = ((EditText) findViewById(R.id.server_address4)).getText().toString();

        String newAddress = "tcp://" + serverAddr1 + "." +
                serverAddr2 + "." +
                serverAddr3 + "." +
                serverAddr4 + ":5555";
        SERVER_ADDRESS = newAddress;

        findViewById(R.id.layout_main).requestFocus();

        if (!firstCall) {
            Toast toast = Toast.makeText(this, newAddress, Toast.LENGTH_SHORT);
            toast.show();
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putString(PREF_SERVER_ADDR1, serverAddr1);
            ed.putString(PREF_SERVER_ADDR2, serverAddr2);
            ed.putString(PREF_SERVER_ADDR3, serverAddr3);
            ed.putString(PREF_SERVER_ADDR4, serverAddr4);
            ed.apply();

            messageSender.changeAddress(SERVER_ADDRESS);

        }
    }

    public boolean onTouch(View view , MotionEvent event ) {
        switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN: {
                Log.d(TAG, "Button down");
                try {
                    messageSender.sendMessageAndWait("dy");
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.d(TAG, "Button up");
                try {
                    messageSender.sendMessageAndWait("dn");
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return true;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.btn_set_server): {
                setServerAddress(false);
                break;
            }
            case (R.id.btn_start): {
                angleWatcher.mustSend = !angleWatcher.mustSend;
                updateButton();
                break;
            }
            case (R.id.btn_pg_up): {
                try {
                    messageSender.sendMessageAndWait("pu");
                } catch (TimeoutException e) {
                    showDialogGeneralError("Timeout","Could not send page up, test server and try again.");
                } catch (IllegalStateException e) {
                    showDialogGeneralError("No Server","Could not send page up, test server and try again.");
                }
                break;
            }
            case (R.id.btn_pg_dwn): {
                try {
                    messageSender.sendMessageAndWait("pd");
                } catch (TimeoutException e) {
                    showDialogGeneralError("Timeout","Could not send page down, test server and try again.");
                } catch (IllegalStateException e) {
                    showDialogGeneralError("No Server","Could not send page down, test server and try again.");
                }
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId()==R.id.sw_ctrl){
            if (isChecked) {
                try {
                    messageSender.sendMessageAndWait("cy");
                } catch (Exception e) {
                    showDialogGeneralError("Error","Did not send, check server");
                }
            } else {
                try {
                    messageSender.sendMessageAndWait("cn");
                } catch (Exception e) {
                    showDialogGeneralError("Error","Did not send, check server");
                }
            }
        }
        else if (buttonView.getId()==R.id.sw_screen_on){
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }
}
