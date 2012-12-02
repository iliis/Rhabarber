package com.floern.rhabarber;

import java.io.File;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.graphic.GameGLSurfaceView;
import com.floern.rhabarber.physics.PhysicsController;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.annotation.SuppressLint;
import android.app.Activity;


/* contains the game itself, starts open gl (which calls the physics and logic on every frame)
 * 
 */
public class GameActivity extends Activity implements SensorEventListener {

	private GameGLSurfaceView surfaceView;
	
	private SensorManager sensorManager;
	
	PhysicsController physics;
	
	private float[] acceleration = new float[3];

    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// avoid screen turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // fullscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// set View
        surfaceView = new GameGLSurfaceView(this);
        surfaceView.setRendererCallback(this);
        setContentView(surfaceView);
        
        // setup sensor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        
        // setup up the actual game
        // TODO: nicely implement this loading of ressources 
        physics = new PhysicsController(this.getResources().openRawResource(R.raw.testworld),
        								this.getResources().openRawResource(R.raw.player));
        
        //File f = new File("/mnt/sdcard/testworld.phy");
        //physics = new PhysicsController(f);

	}
	
	public void onDraw(GL10 gl) {
		physics.tick();
		physics.setAccel(acceleration);
		physics.draw(gl);
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
        	System.arraycopy(event.values, 0, acceleration, 0, 3);
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
