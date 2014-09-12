package com.haojian.accelerometer.app;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.androidplot.xy.XYPlot;
import com.haojian.plot.DynamicLinePlot;
import com.haojian.plot.PlotColor;
import com.haojian.sensorplayground.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class DirectionDetector_v1 extends Activity implements SensorEventListener, Runnable {
	public final static String TAG = DirectionDetector_v1.class.getName();

	TextView textView;
	StringBuilder builder = new StringBuilder();

	float threshold = 0.3f;
	float MIN_ACC = 0.1f;
	public enum DIRECTION{STATIC, POSITIVE, NEGATIVE};

	DIRECTION [] direction = {DIRECTION.STATIC, DIRECTION.STATIC, DIRECTION.STATIC};
	private DescriptiveStatistics stat_x = new DescriptiveStatistics(10);
	private DescriptiveStatistics stat_y = new DescriptiveStatistics(10);
	private DescriptiveStatistics stat_z = new DescriptiveStatistics(10);

	private boolean motionFlag = false;
	
	private float[] acc_val = new float[3];
	private int[] static_counter = new int[3];
	
	// Graph plot for the UI outputs
	private DynamicLinePlot dynamicPlot;
	
	
	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";
	
	// Plot keys for the acceleration plot
	private final static int PLOT_ACCEL_X_AXIS_KEY = 0;
	private final static int PLOT_ACCEL_Y_AXIS_KEY = 1;
	private final static int PLOT_ACCEL_Z_AXIS_KEY = 2;
	
	
	// Color keys for the acceleration plot
	// Plot colors
	private int plotAccelXAxisColor = Color.BLUE;
	private int plotAccelYAxisColor = Color.GREEN;
	private int plotAccelZAxisColor = Color.RED;

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		textView = new TextView(this);
		setContentView(R.layout.activity_directiondetector);
//		setContentView(textView);
		textView = (TextView)findViewById(R.id.lbl_directions);
		// Create the graph plot
		XYPlot plot = (XYPlot) findViewById(R.id.plot_sensor);
		plot.setTitle("Acceleration");
		dynamicPlot = new DynamicLinePlot(plot);
		dynamicPlot.setMaxRange(5);
		dynamicPlot.setMinRange(-5);

		addAccelerationPlot();
		SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor accelerometer = manager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).get(0);
		manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		handler = new Handler();
		handler.post(this);
	}
	
	/**
	 * Create the output graph line chart.
	 */
	private void addAccelerationPlot()
	{
		addPlot(plotAccelXAxisTitle, PLOT_ACCEL_X_AXIS_KEY, plotAccelXAxisColor);
		addPlot(plotAccelYAxisTitle, PLOT_ACCEL_Y_AXIS_KEY, plotAccelYAxisColor);
		addPlot(plotAccelZAxisTitle, PLOT_ACCEL_Z_AXIS_KEY, plotAccelZAxisColor);
	}
	
	private void addPlot(String title, int key, int color)
	{
		dynamicPlot.addSeriesPlot(title, key, color);
	}

	
	private void plotData()
	{
		dynamicPlot.setData(acc_val[0], PLOT_ACCEL_X_AXIS_KEY);
//		dynamicPlot.setData(acc_val[1], PLOT_ACCEL_Y_AXIS_KEY);
//		dynamicPlot.setData(acc_val[2], PLOT_ACCEL_Z_AXIS_KEY);
		dynamicPlot.draw();

	}
	
	private float[] mechFilter(float m[]) {
		for (int i=0; i<3; ++i)
			if (!(m[i]>MIN_ACC || m[i]<-MIN_ACC))
				m[i]=0;
		return m;
	}
	

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			stat_x.addValue(event.values[0]);
			stat_y.addValue(event.values[1]);
			stat_z.addValue(event.values[2]);
			acc_val[0] = (float) stat_x.getMean();
			acc_val[1] = (float) stat_y.getMean();
			acc_val[2] = (float) stat_z.getMean();
			acc_val = mechFilter(acc_val);
//			Log.v(TAG, Arrays.toString(acc_val));
			for(int i=0; i<3; ++i){
				if(acc_val[i] != 0 ){
					static_counter[i] = 0;
					if(acc_val[i] > threshold && direction[i] == DIRECTION.STATIC){
						//init one movement.
						direction[i] = DIRECTION.POSITIVE;
					}else if(acc_val[i] < -threshold && direction[i] == DIRECTION.STATIC){
						direction[i] = DIRECTION.NEGATIVE;
					}
				}else{
					static_counter[i]++;
					if(static_counter[i] > 10 ){
						direction[i] = DIRECTION.STATIC;
					}
				}
			}

			builder.setLength(0);
			builder.append("x: ");
			builder.append(direction[0]);
			builder.append("\ny: ");
			builder.append(direction[1]);
			builder.append("\nz: ");
			builder.append(direction[2]);

			textView.setText(builder.toString());
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		handler.postDelayed(this, 100);
		plotData();
//		logData();
	}
}
