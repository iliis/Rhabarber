package com.floern.rhabarber.physicstest;

import com.floern.rhabarber.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.graphics.Canvas;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.support.v4.app.NavUtils;
import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;

public class PhysicsTestActivity extends Activity implements Callback {

	private SurfaceHolder myHolder;
	private PhysicsTask task;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_physics_test);
		SurfaceView physicsSurfaceView = (SurfaceView) findViewById(R.id.physicsSurfaceView);
		myHolder = physicsSurfaceView.getHolder();
		myHolder.addCallback(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		task.stop = true;
	}

	private class PhysicsTask extends AsyncTask<Void, Void, Void> {
		private int width;
		private int height;
		private RenderWorld myWorld;
		private Canvas physicsCanvas;

		// to be set to true in onDestroy
		public boolean stop;

		@Override
		protected void onPreExecute() {
			// set up World
			myWorld = new RenderWorld();
			physicsCanvas = myHolder.lockCanvas();
			width = physicsCanvas.getWidth();
			height = physicsCanvas.getHeight();
			
			myWorld.setGravity(1);
			
			Log.d("bla", width + " " + height);

			Shape bigbox = Shape.createRectangle(width-10,height-10);
			Shape smallbox = Shape.createRectangle(20,20);
			smallbox.setElasticity(80);
			Body anchor = new Body(width/2, height/2, bigbox, false);
			Body boxbody = new Body(width / 2, height / 2, smallbox, true);
			
			myWorld.addBody(anchor);
			myWorld.addBody(boxbody);
			stop = false;
			myHolder.unlockCanvasAndPost(physicsCanvas);
		}

		@Override
		protected Void doInBackground(Void... arg0) {

			long sleep = 0;
			long start = 0;

			while (myWorld != null && !stop) {
				start = SystemClock.elapsedRealtime();

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
				if(physicsCanvas!=null)
				{
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

}
