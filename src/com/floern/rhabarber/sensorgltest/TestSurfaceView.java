package com.floern.rhabarber.sensorgltest;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class TestSurfaceView extends GLSurfaceView {

	public final TestRenderer renderer;
	
    public TestSurfaceView(Context context){
        super(context);

        // Set the Renderer for drawing on the GLSurfaceView
        renderer = new TestRenderer();
        setRenderer(renderer);
    }
    
    
    @Override
    public void onPause(){
    	super.onPause();
    	pauseRendering();
    }
    
    
    @Override
    public void onResume(){
    	super.onResume();
    }
    

    /**
     * Pause Rendering
     */
	public void pauseRendering() {
    	setRenderMode(TestSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	

    /**
     * Continue Rendering
     */
	public void resumeRendering() {
    	setRenderMode(TestSurfaceView.RENDERMODE_CONTINUOUSLY);
	}

}
