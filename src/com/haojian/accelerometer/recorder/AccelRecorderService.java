package com.haojian.accelerometer.recorder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.haojian.sensorplayground.R;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AccelRecorderService extends IntentService implements
		SensorEventListener {

	private SensorManager mSmgr;
	private boolean running = false;
	private String file_name;
	private int rec_freq;
	private long start_time;
	private long no_datapoints;
	FileOutputStream mOutputFileStream;
	BufferedOutputStream mOutputBufferedStream;
	DataOutputStream mOutputDataStream;
	PowerManager.WakeLock wl;
	private static int MAX_ENCODING_TYPES = 8; // maximum number of different
												// sensors being encoded

	public AccelRecorderService() {
		super("Accelerator Recorder Service");

	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		AccelRecorderService getService() {
			return AccelRecorderService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("accel_service", "Bindinig to serivce");
		return mBinder;
	}

	@Override
	public void onDestroy() {
		Log.d("accelrecservice", "Destroying Service");
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mSmgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		running = false;
		Intent i;
		PowerManager pm = (PowerManager) getApplicationContext()
				.getSystemService(POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"accel_recorder_wakelock");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (!running) {
			Bundle data = intent.getExtras();
			if (data.containsKey("file_name") && data.containsKey("rec_freq")) {
				file_name = data.getString("file_name");
				rec_freq = data.getInt("rec_freq");
				int[] sensorList = data.getIntArray("sensor_list");
				if (sensorList == null) {
					sensorList = new int[1];
					sensorList[0] = Sensor.TYPE_ACCELEROMETER;
				}
				running = true;
				wl.acquire();
				start_time = System.currentTimeMillis();
				no_datapoints = 0;
				// start data collection;

			    File output_folder = new File(Environment.getExternalStorageDirectory(), "TestFolder");
			    if (!output_folder.exists()) {
			    	output_folder.mkdir();
			    }
			    	
				Log.v("path", output_folder.toString());
				output_folder.mkdirs();
				File output_file = new File(output_folder, file_name);
				int total_sensors_in_use = 0;
				for (int sensor_type : sensorList) {
					Sensor mSensor = mSmgr.getDefaultSensor(sensor_type);
					if (mSensor != null) {
						total_sensors_in_use++;
						mSmgr.registerListener(this, mSensor, rec_freq);
					} else {
						Log.w("accel_service", "Sensor of type " + sensor_type + " was not available");
					}
				}
				if(total_sensors_in_use == 0) {
					Log.e("accel_service", "No valid sensors available not starting...");
					return;
				}
				try {
					mOutputFileStream = new FileOutputStream(output_file);
				} catch (FileNotFoundException e) {
					stop_recording();
				}
				mOutputBufferedStream = new BufferedOutputStream(
						mOutputFileStream);
				mOutputDataStream = new DataOutputStream(mOutputBufferedStream);
				Log.d("accel_service", "STarting to record...");
				counter = 0;
			} else {
				Log.e("accel_service", "No file name or frequency specified");
			}
		} else {
			Log.e("accel_service", "Requested second recording while recording");
		}
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this);
		mBuilder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Recording Accelerometer Data")
				.setContentText(
						"Accelerometer Data is currently being recorded")
				.setTicker("Starting Accelerometer Recording...")
				.setContentIntent(pendingIntent);

		startForeground(1, mBuilder.build());

		// Don't let the function end or IntentService might destroy you are add
		// other recordings
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop_recording() {
		if (running) {
			running = false;
			wl.release();
			Log.d("accel_service", "Stopped recording...");
			// stop recording;
			mSmgr.unregisterListener(this);
			try {
				mOutputDataStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stopForeground(true);
			synchronized (this) {
				this.notify();
			}
		}
	}

	public boolean is_running() {
		return running;
	}

	public long get_no_points() {
		return no_datapoints;
	}

	public long get_start_time() {
		return start_time;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	private static final boolean ADAPTIVE_ACCEL_FILTER = true;
	float lastAccel[] = new float[3];
	float accelFilter[] = new float[3];
	
	private int counter = 0;
	private int window_size = 10;
	
	private DescriptiveStatistics stat_x = new DescriptiveStatistics(10);
	private DescriptiveStatistics stat_y = new DescriptiveStatistics(10);
	private DescriptiveStatistics stat_z = new DescriptiveStatistics(10);
	
	public double norm(float x, float y, float z){
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	public double clamp(double num, double min, double max){
		return num < min ? min : (num > max ? max : num);
	}
	
	public void onAccelerometerChanged(float accelX, float accelY, float accelZ) {
	    // high pass filter
	    float updateFreq = 30; // match this to your update speed
	    float cutOffFreq = 0.9f;
	    float RC = 1.0f / cutOffFreq;
	    float dt = 1.0f / updateFreq;
	    float filterConstant = RC / (dt + RC);
	    float alpha = filterConstant; 
	    float kAccelerometerMinStep = 0.033f;
	    float kAccelerometerNoiseAttenuation = 3.0f;

	    if(ADAPTIVE_ACCEL_FILTER)
	    {
	        float d = (float) clamp(Math.abs(norm(accelFilter[0], accelFilter[1], accelFilter[2]) - norm(accelX, accelY, accelZ)) / kAccelerometerMinStep - 1.0, 0.0, 1.0);
	        alpha = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;
	    }

	    accelFilter[0] = (float) (alpha * (accelFilter[0] + accelX - lastAccel[0]));
	    accelFilter[1] = (float) (alpha * (accelFilter[1] + accelY - lastAccel[1]));
	    accelFilter[2] = (float) (alpha * (accelFilter[2] + accelZ - lastAccel[2]));

	    lastAccel[0] = accelX;
	    lastAccel[1] = accelY;
	    lastAccel[2] = accelZ;
	    onFilteredAccelerometerChanged(accelFilter[0], accelFilter[1], accelFilter[2]);
	}
	
	public void onFilteredAccelerometerChanged(float x, float y, float z){
		int id = 4;
		try {
			mOutputDataStream.writeLong(System.currentTimeMillis()
					* MAX_ENCODING_TYPES + id);
			mOutputDataStream.writeFloat(x);
			mOutputDataStream.writeFloat(y);
			mOutputDataStream.writeFloat(z);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void write_data(SensorEvent event) {
		no_datapoints++;
		try {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				int id = 0;
//				Log.v("path", "ifnull: " + mOutputDataStream==null? "null" : "fine");
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				int id = 1;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			} else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
				int id = 2;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
				int id = 3;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
				//mOutputDataStream.writeFloat(event.values[3]); // this one is not always available
			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				int id = 4;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
//				mOutputDataStream.writeFloat(event.values[0]);
//				mOutputDataStream.writeFloat(event.values[1]);
//				mOutputDataStream.writeFloat(event.values[2]);
				stat_x.addValue(event.values[0]);
				stat_y.addValue(event.values[1]);
				stat_z.addValue(event.values[2]);
				mOutputDataStream.writeFloat((float) stat_x.getMean());
				mOutputDataStream.writeFloat((float) stat_y.getMean());
				mOutputDataStream.writeFloat((float) stat_z.getMean());
//				onAccelerometerChanged(event.values[0], event.values[1], event.values[2]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onSensorChanged(SensorEvent event) {
		write_data(event);
	}

}
