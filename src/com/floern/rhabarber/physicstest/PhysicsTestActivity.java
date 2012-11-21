package com.floern.rhabarber.physicstest;

import com.floern.rhabarber.R;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.graphics.Canvas;
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
import at.emini.physics2D.util.FXVector;

public class PhysicsTestActivity extends Activity implements Callback,
		SensorEventListener {

	private SurfaceHolder myHolder;
	private PhysicsTask task;
	private SensorManager sensorManager;
	private RenderWorld myWorld;
	private FXVector tmpgravity;

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
		public boolean stop;

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
			Body anchor_top = new Body(width / 2, 5, bigbox_long, false);
			Body anchor_left = new Body(5, height / 2, bigbox_high, false);
			Body anchor_right = new Body(width - 5, height / 2, bigbox_high,
					false);
			Body anchor_bot = new Body(width / 2, height - 5, bigbox_long,
					false);

			Body boxbody = new Body(width / 2, height / 2, smallbox, true);
			Body boxbody2 = new Body(width / 4, height / 2, smallbox, true);

			//add bodies to world
			myWorld.addBody(anchor_top);
			myWorld.addBody(anchor_bot);
			myWorld.addBody(anchor_left);
			myWorld.addBody(anchor_right);
			myWorld.addBody(boxbody);
			myWorld.addBody(boxbody2);
			stop = false;
			myHolder.unlockCanvasAndPost(physicsCanvas);
		}

		@Override
		protected Void doInBackground(Void... arg0) {

			long sleep = 0;
			long start = 0;

			while (myWorld != null && !stop) {
				start = SystemClock.elapsedRealtime();

				// get gravity
				synchronized(tmpgravity)
				{
					myWorld.setGravity(tmpgravity);
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
				myHolder.unlockCanvasAndPost(physicsCanvas);
			}
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

	public void onSensorChanged(SensorEvent event) {
		/*
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			return; // sensor data unreliable
		}
		*/
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// update acceleration values
			synchronized (tmpgravity) {
				tmpgravity.assignFX((int) event.values[1], (int) event.values[0]);
				tmpgravity.normalize();
				tmpgravity.mult(50);
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
