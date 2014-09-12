package com.haojian.accelerometer.recorder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.haojian.accelerometer.recorder.AccelRecorderService.LocalBinder;
import com.haojian.sensorplayground.R;

public class MainActivity extends Activity {
	View screen1, screen2;
	EditText mFileName;
	TextView mCounterDatapoints, mCounterDuration, mCounterFileSize,
			mCounterRate, mCounterStartTime;
	Map<Integer, CheckBox> check_boxes;
	Spinner mFreq;
	boolean mBound = false;
	AccelRecorderService mService;
	Handler mHandler;
	static Random rand = new Random();
	public static int sessionid = rand.nextInt(1000);
	public static int id_in_session = 0;
	Runnable status_updater = new Runnable() {
		public void run() {
			if (mService == null)
				Log.d("accel_activity", "No service launched yet...");
			if (mService != null && mService.is_running()) {
				is_running();
			} else {
				is_stopped();
			}
			mHandler.postDelayed(status_updater, 1000); // Update every 1 second
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		screen1 = findViewById(R.id.screen1);
		screen2 = findViewById(R.id.screen2);
		check_boxes = new HashMap<Integer, CheckBox>();
		check_boxes.put(Sensor.TYPE_ACCELEROMETER, (CheckBox) findViewById(R.id.checkBox_accelerometer));
		check_boxes.put(Sensor.TYPE_GRAVITY, (CheckBox) findViewById(R.id.checkBox_gravity));
		check_boxes.put(Sensor.TYPE_GYROSCOPE, (CheckBox) findViewById(R.id.checkBox_gyro));
		check_boxes.put(Sensor.TYPE_LINEAR_ACCELERATION, (CheckBox) findViewById(R.id.checkBox_linacc));
		check_boxes.put(Sensor.TYPE_ROTATION_VECTOR, (CheckBox) findViewById(R.id.checkBox_rotation));
		mFileName = (EditText) findViewById(R.id.rec_name);
		mFileName.setText(""+ sessionid + "_" + id_in_session);
		mFreq = (Spinner) findViewById(R.id.spinner_rec_freq);
		mCounterDatapoints = (TextView) findViewById(R.id.counter_datapoints);
		mCounterDuration = (TextView) findViewById(R.id.counter_duration);
		mCounterFileSize = (TextView) findViewById(R.id.counter_filesize);
		mCounterRate = (TextView) findViewById(R.id.counter_rate);
		mCounterStartTime = (TextView) findViewById(R.id.counter_start_time);
		mHandler = new Handler();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		mHandler.removeCallbacks(status_updater);
	}

	@Override
	protected void onResume() {
		super.onResume();
		screen1.setVisibility(View.GONE);
		screen2.setVisibility(View.GONE);
		Intent intent = new Intent(this, AccelRecorderService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mHandler.post(status_updater);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			if (mService.is_running())
				is_running();
			else
				is_stopped();
		}

		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			finish();
		}
	};

	public void start_recording(View v) {
		String file_name = mFileName.getText().toString();
		int freq = mFreq.getSelectedItemPosition();
		if (freq == 0)
			freq = SensorManager.SENSOR_DELAY_FASTEST;
		else if (freq == 1)
			freq = SensorManager.SENSOR_DELAY_GAME;
		else if (freq == 2)
			freq = SensorManager.SENSOR_DELAY_UI;
		else if (freq == 3)
			freq = SensorManager.SENSOR_DELAY_NORMAL;
		else
			freq = -1;

	    String root = Environment.getExternalStorageDirectory().toString();
	    Log.v("path", root);
		File output_folder = new File(
				Environment.getExternalStorageDirectory(), "TestFolder");
		
		if (!output_folder.exists() && !output_folder.mkdirs()) {
			Log.d("accel_activity",
					"Unable to make folder : " + output_folder.toString());
		}
		File output_file = new File(output_folder, file_name);
		Log.d("accel_activity",
				"Attempting to use file : " + output_file.toString());
		try {
			output_file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!output_file.isFile() || !output_file.canWrite()) {
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setTitle("Invalid File Name");
			dialog.setMessage("Error! Unable to create a file with name :\""
					+ file_name + "\"");
			DialogInterface.OnClickListener dummy_listener = null;
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK",
					dummy_listener);
			dialog.show();
			return;
		}

		mFileName.setText("");
		Log.d("accel_activity", "Selected File name : " + file_name);
		Log.d("accel_activity", "Selected Item Id : " + freq);
		ArrayList<Integer> sensor_list = new ArrayList<Integer>();
		for(Map.Entry<Integer, CheckBox> entry : check_boxes.entrySet()) {
			if(entry.getValue().isChecked())
				sensor_list.add(entry.getKey());
		}
		int[] sensor_array = new int [sensor_list.size()];
		for(int i = 0; i < sensor_array.length; i++)
			sensor_array[i] = sensor_list.get(i);
		
		Intent intent = new Intent(this, AccelRecorderService.class);
		intent.putExtra("file_name", file_name);
		intent.putExtra("rec_freq", freq);
		intent.putExtra("sensor_list", sensor_array);
		startService(intent);
		is_running();
	}

	private static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public void is_running() {
		Log.d("accel_activity", "Updating UI running status");
		screen2.setVisibility(View.VISIBLE);
		screen1.setVisibility(View.GONE);
		long no_points = mService.get_no_points();
		long start_time = mService.get_start_time();
		long current_time = System.currentTimeMillis();
		long duration = current_time - start_time;
		duration /= 1000;
		if (duration == 0)
			duration = 1;
		mCounterDatapoints.setText(Long.toString(mService.get_no_points()));
		mCounterDuration.setText(String.format("%d:%02d:%02d", duration / 3600,
				(duration % 3600) / 60, (duration % 60)));
		mCounterFileSize.setText(humanReadableByteCount(no_points * 20, false));

		String sRate = (no_points * 10 / duration) / 10.0 + " points/s , ";
		sRate += humanReadableByteCount(((no_points*20) / duration), false) + "/s";
		mCounterRate.setText(sRate);
		mCounterStartTime.setText(DateFormat.format("MMMM dd, yyyy h:mmaa",
				start_time));
	}

	public void is_stopped() {
		// Log.d("accel_activity","Updating UI stopped status");
		screen1.setVisibility(View.VISIBLE);
		screen2.setVisibility(View.GONE);
	}

	public void stop_recording(View v) {
		mService.stop_recording();
		is_stopped();
		id_in_session++;
		mFileName.setText(""+ sessionid + "_" + id_in_session);
	}
}
