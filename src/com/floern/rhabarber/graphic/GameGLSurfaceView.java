package com.floern.rhabarber.graphic;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GameGLSurfaceView extends GLSurfaceView {
	
	public final GameRenderer renderer;
	
    public GameGLSurfaceView(Context context){
        super(context);

        // Set the Renderer for drawing on the GLSurfaceView
        renderer = new GameRenderer();
        setRenderer(renderer);
        
        // Create an OpenGL ES 2.0 context
        // If you use this, declare it in the manifest as well!
        // You would also have to change a few other things
        // (namely implement the glOrtho as a vertex shader, implement a fragment shader) 
        // setEGLContextClientVersion(2);
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
    	setRenderMode(RENDERMODE_WHEN_DIRTY);
	}
	

    /**
     * Continue Rendering
     */
	public void resumeRendering() {
    	setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

}
