package com.haojian.accelerometer.app;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class DirectionDetector_v1 extends Activity implements SensorEventListener {
    TextView textView;
    StringBuilder builder = new StringBuilder();
    
    float threshold = 0.5f;
    float MIN_ACC = 0.2f;
    public enum DIRECTION{STATIC, POSITIVE, NEGATIVE};
    
    DIRECTION [] direction = {DIRECTION.STATIC, DIRECTION.STATIC, DIRECTION.STATIC};
    private DescriptiveStatistics stat_x = new DescriptiveStatistics(10);
    private DescriptiveStatistics stat_y = new DescriptiveStatistics(10);
    private DescriptiveStatistics stat_z = new DescriptiveStatistics(10);
    
    float[] a_v= new float [3];
    Long t0;
    int motionFlag;
    int count;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textView = new TextView(this);
        setContentView(textView);

	    t0= System.currentTimeMillis();
	    motionFlag=0;
	    
        SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = manager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).get(0);
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

	  private float[] mechFilter(float m[]) {
		  for (int i=0; i<3; ++i)
			  if (!(m[i]>MIN_ACC || m[i]<-MIN_ACC))
				  m[i]=0;
			  else 
				  m[i]-=offset[i];
		  return m;
	  }

	  @Override
	  public void onSensorChanged(SensorEvent event) {
		  // TODO Auto-generated method stub
		  if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			  float delta=(float)(event.timestamp-t0)/1000000000;
			  a_v=mechFilter(event.values);
			  for (int i=0; i<3; ++i) {
				  if (a_v[i]!=0){
					  float xChange = history[0] - event.values[0];
					  float yChange = history[1] - event.values[1];

					  history[0] = event.values[0];
					  history[1] = event.values[1];

					  if (xChange > threshold &&  direction[0] == "Static") {
						  direction[0] = "LEFT";
					  } else if (xChange < -threshold &&  direction[0] == "Static") {
						  direction[0] = "RIGHT";
					  }

					  if (yChange > threshold &&  direction[1] == "Static") {
						  direction[1] = "BOT";
					  } else if (yChange < -threshold &&  direction[1] == "Static") {
						  direction[1] = "TOP";
					  }
					  
					  if (yChange > threshold &&  direction[2] == "Static") {
						  direction[2] = "BOT";
					  } else if (yChange < -threshold &&  direction[2] == "Static") {
						  direction[2] = "TOP";
					  }
					  motionFlag=0;
				  }
				  else if (motionFlag==0)
					  motionFlag=count;
				  if (motionFlag!=0 && count-motionFlag>10)
					  direction[i] = "Static";
			  }


			  builder.setLength(0);
			  builder.append("x: ");
			  builder.append(direction[0]);
			  builder.append("\ny: ");
			  builder.append(direction[1]);
			  builder.append("\nz: ");
			  builder.append(direction[2]);

			  textView.setText(builder.toString());
			  t0=event.timestamp; 
			  count++;
		  }
	  }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}
