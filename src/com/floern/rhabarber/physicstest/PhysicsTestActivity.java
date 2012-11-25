package com.floern.rhabarber.physicstest;

import com.floern.rhabarber.R;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.support.v4.app.NavUtils;
import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXUtil;
import at.emini.physics2D.util.FXVector;

public class PhysicsTestActivity extends Activity implements Callback,
		SensorEventListener {

	private SurfaceHolder myHolder;
	private PhysicsTask task;
	private SensorManager sensorManager;
	private RenderWorld myWorld;
	private volatile FXVector tmpgravity; // gravity (constant amplitutde of 1G)
	private volatile FXVector tmpaccel;   // 0 when device is not moved
	private float gravity[] = new float[3], linear_acceleration[] = new float[3]; // used for low/high-pass filter of accelerometer

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_physics_test);
		SurfaceView physicsSurfaceView = (SurfaceView) findViewById(R.id.physicsSurfaceView);
		myHolder = physicsSurfaceView.getHolder();
		myHolder.addCallback(this);
		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		tmpgravity = new FXVector(0, 10);
		tmpaccel   = new FXVector(0, 0);
		myWorld = new RenderWorld();
		sensorEnable();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		task.stop = true;
		sensorDisable();
	}

	private class PhysicsTask extends AsyncTask<Void, Void, Void> {
		private int width;
		private int height;

		private Canvas physicsCanvas;

		// to be set to true in onDestroy
		public volatile boolean stop;

		@Override
		protected void onPreExecute() {
			physicsCanvas = myHolder.lockCanvas();
			width = physicsCanvas.getWidth();
			height = physicsCanvas.getHeight();
			
			//define shapes
			Shape bigbox_long = Shape.createRectangle(width - 10, 10);
			Shape bigbox_high = Shape.createRectangle(10, height - 10);
			Shape smallbox = Shape.createRectangle(20, 20);
			
			//make them bouncy
			smallbox.setElasticity(80);
			bigbox_long.setElasticity(80);
			bigbox_high.setElasticity(80);

			
			//create bodies based on shapes above
			Body anchor_top   = new Body(width / 2, 5,  bigbox_long, false);
			Body anchor_left  = new Body(5, height / 2, bigbox_high, false);
			Body anchor_right = new Body(width - 5, height / 2, bigbox_high,
					false);
			Body anchor_bot   = new Body(width / 2, height - 5, bigbox_long,
					false);

			Body boxbody    = new Body(width / 2, height / 2, smallbox, true);
			Body boxbody2   = new Body(width / 4, height / 2, smallbox, true);
			
			Body spherebody = new Body(width / 3, height / 3, Shape.createCircle(30), true);

			//add bodies to world
			myWorld.addBody(anchor_top);
			myWorld.addBody(anchor_bot);
			myWorld.addBody(anchor_left);
			myWorld.addBody(anchor_right);
			myWorld.addBody(boxbody);
			myWorld.addBody(boxbody2);
			myWorld.addBody(spherebody);
			stop = false;
			myHolder.unlockCanvasAndPost(physicsCanvas);
			
			//myWorld.setGravity(0); // disable gravity if we apply the acceleration direct
		}

		@Override
		protected Void doInBackground(Void... arg0) {

			long sleep = 0;
			long start = 0;

			while (myWorld != null && !stop) {
				start = SystemClock.elapsedRealtime();

				// get gravity
				// TODO: is this synchronized really necessary here? we only read the gravity-vector
				synchronized(tmpgravity)
				{
					myWorld.setGravity(tmpgravity);
					//applyAcceleration(tmpaccel);
				}
				
				// simulate
				myWorld.tick();
				// initiate draw
				publishProgress();

				// sleep
				sleep = 30 - (SystemClock.elapsedRealtime() - start);
				sleep = Math.max(sleep, 0);

				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					Log.d("Rhabarber", "interrupted");
					stop = true;
				}

			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// draw in uiThread because of android policy
			if (myHolder != null) {
				physicsCanvas = myHolder.lockCanvas();
				if (physicsCanvas != null) {
					myWorld.draw(physicsCanvas);
				}
				
				// TODO: This causes an IllegalArgumentException when closing the Activity
				myHolder.unlockCanvasAndPost(physicsCanvas);
			}
		}

	}
	
	public void applyAcceleration(FXVector v) {
		int bodyCount = myWorld.getBodyCount();
		Body[] bodies = myWorld.getBodies();
		
		for (int i = 0; i < bodyCount; i++) {
			bodies[i].applyAcceleration(v, myWorld.getTimestepFX());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_physics_test, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// nothing to do here since orientation is locked

	}

	public void surfaceCreated(SurfaceHolder holder) {
		task = new PhysicsTask();
		task.execute();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		task.stop = true;
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}
	
	public static int floatToFX(float v) {
		return (int) (v * (1 << FXUtil.DECIMAL));
	}

	public void onSensorChanged(SensorEvent event) {
		/*
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			return; // sensor data unreliable
		}
		*/
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			
			// low/high-pass according to https://developer.android.com/reference/android/hardware/SensorEvent.html#values
			final float alpha = 0.7f;
			
			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
			
			linear_acceleration[0] = event.values[0] - gravity[0];
			linear_acceleration[1] = event.values[1] - gravity[1];
			linear_acceleration[2] = event.values[2] - gravity[2];
			
			
			// update acceleration values
			synchronized (tmpgravity) {
				tmpgravity.assignFX(floatToFX(gravity[1]), floatToFX(gravity[0]));
				//tmpgravity.normalize();
				
				// sensor gives value in m/s^2, physics engine takes gravity as pixel/s^2
				tmpgravity.mult(20);
			}
			
			synchronized (tmpaccel) {
				tmpaccel.assignFX(floatToFX(linear_acceleration[1]), floatToFX(linear_acceleration[0]));
				tmpaccel.mult(-200);
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

}
