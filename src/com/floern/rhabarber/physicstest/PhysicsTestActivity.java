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
import android.view.SurfaceView;
import android.support.v4.app.NavUtils;
import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.World;

public class PhysicsTestActivity extends Activity {

	private SurfaceHolder myHolder;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physics_test);
        SurfaceView physicsSurfaceView = (SurfaceView) findViewById(R.id.physicsSurfaceView);
        myHolder = physicsSurfaceView.getHolder();
    }
    
    
    private class PhysicsTask extends AsyncTask<Void,Void,Void>
    {
    	private int width;
    	private int height;
    	private World myWorld;
    	private Canvas physicsCanvas;
    	
    	//to be set to true in onDestroy
    	public boolean stop;
    	
    	@Override
    	protected void onPreExecute() {
    		//set up World
    		myWorld = new World();
    		physicsCanvas = myHolder.lockCanvas();
    		width = physicsCanvas.getWidth();
    		height = physicsCanvas.getHeight();
    		
    		Shape smallbox = Shape.createRectangle(2, 2);
    		Shape ball = Shape.createCircle(10);
    		ball.setElasticity(80);
    		Body anchor = new Body(0, 0, smallbox, false);
    		Body ballBody = new Body(width/2,height/2,ball,true);
    		
    		myWorld.addBody(anchor);
    		myWorld.addBody(ballBody);
    		stop = false;
    		
    		myHolder.unlockCanvasAndPost(physicsCanvas);
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			
			long sleep = 0;
			long start = 0;
			
			while(myWorld!=null && !stop)
			{
				start = SystemClock.elapsedRealtime();
				
				myWorld.tick();
				sleep = 50 - (SystemClock.elapsedRealtime()-start);
				sleep = Math.max(sleep, 0);
				
				try{
					Thread.sleep(sleep);
				}
				catch (InterruptedException e)
				{
					Log.d("Rhabarber","interrupted");
				}
				
			}
			return null;
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

}
