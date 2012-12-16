package com.floern.rhabarber.sensorgltest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;


/**
 * @author Flo
 */
public class SensorTestOpenGLActivity extends Activity implements SensorEventListener {

	private TestSurfaceView surfaceView;
	
	private SensorManager sensorManager;
	
	private boolean deviceIsLandscapeDefault;

    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// avoid screen turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // fullscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// set View
        surfaceView = new TestSurfaceView(this);
        setContentView(surfaceView);
        
        // setup sensor manager
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		// check default device orientation
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int orientation = (display.getWidth() <= display.getHeight()) ?
				  Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE;
		// sensor vector is rotated on landscape-default devices (some tablets)
		int rotation = display.getRotation();
		deviceIsLandscapeDefault = (orientation == Configuration.ORIENTATION_LANDSCAPE && (rotation == Surface.ROTATION_0  || rotation == Surface.ROTATION_180))
				                || (orientation == Configuration.ORIENTATION_PORTRAIT  && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270));

	}
	

	/**
	 * Called when sensor values have changed.
	 * @param event SensorEvent
	 */
    @SuppressLint("FloatMath")
	public void onSensorChanged(SensorEvent event) {
    	// some devices always report UNRELIABLE, making it unusable with this code:
    	/*if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
    		return; // sensor data unreliable
    	}*/
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// update acceleration values 
			if (deviceIsLandscapeDefault) {
				// rotate X and Y
				TestRenderer.acceleration[0] = event.values[1];
				TestRenderer.acceleration[1] = -event.values[0];
				TestRenderer.acceleration[2] = event.values[2];
			}
			else {
	        	// update acceleration values
	        	System.arraycopy(event.values, 0, TestRenderer.acceleration, 0, 3);
			}
        }
    }

    
    /**
     * Register sensor listener
     */
    public void sensorEnable() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    
    /**
     * Unregister sensor listener
     */
    public void sensorDisable() {
    	sensorManager.unregisterListener(this);
    }

    
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	
    @Override
    protected void onResume() {
    	super.onResume();
        surfaceView.onResume();
    	sensorEnable();
    }

    
    @Override
    protected void onPause() {
    	sensorDisable();
    	surfaceView.onPause();
    	super.onPause();
    	System.gc();
    }

    
    @Override
    protected void onDestroy() {
    	sensorDisable();
    	super.onDestroy();
    }
	
}
