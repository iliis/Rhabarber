package com.floern.rhabarber.sensorgltest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.hardware.SensorManager;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;

public class TestRenderer implements Renderer {
    
	/** Acceleration Vector, updated by sensor listener */
	public final static float[] acceleration = new float[]{0f, 0f, 0f};
	
	
	private FloatBuffer sensorLineVertexBuffer;
	
	
	public TestRenderer() {
		
	}
	
	
	/**
	 * Startup
	 */
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background frame color
        gl.glClearColor(0f, 0f, 0f, 1f);

        // Alphablending
	    gl.glEnable(GL10.GL_BLEND);
	    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        // Enable use of vertex arrays
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        
        // create vertex buffer for test line
        sensorLineVertexBuffer = ByteBuffer.allocateDirect(4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
    
    
    /**
     * Draw a frame
     */
    public void onDrawFrame(GL10 gl) {

	    // GAME LOGIC
	    gameLogic();
	    
		// DRAW LOGIC
	    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT); // Redraw background color
	    gl.glMatrixMode(GL10.GL_MODELVIEW); // Set GL_MODELVIEW transformation mode
	    gl.glLoadIdentity(); // reset the matrix to its default state
	    GLU.gluLookAt(gl, 0f, 0f, -5f, 0f, 0f, 0f, 0f, 1f, 0f); // When using GL_MODELVIEW, you must set the view point
	    
	    //gl.glScalef(1f, 1f, 1f);
	    //gl.glRotatef(0f, 0f, 1f, 0f);
	    //gl.glTranslatef(0f, 0f, 0f);
	    
	    // DRAW
	    draw(gl);
	    
    }
    
    
    /**
     * Game Logic
     */
    private void gameLogic() {
    	// set coords
        float x = -acceleration[1] / SensorManager.GRAVITY_EARTH;
        float y = -acceleration[0] / SensorManager.GRAVITY_EARTH;
    	sensorLineVertexBuffer.put(0f).put(0f).put(x).put(y);
        sensorLineVertexBuffer.position(0); // <- important
    }
    

    /**
     * Draw Scene
     * @param gl
     */
    private void draw(GL10 gl) {
    	// draw line vertex buffer
		gl.glLineWidth(4f);
		gl.glColor4f(.9f, .9f, .9f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, sensorLineVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINES, 0, 2);
    }
    

    /**
     * Surfacechange
     */
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        // make adjustments for screen ratio
        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);        // set matrix to projection mode
        gl.glLoadIdentity();                        // reset the matrix to its default state
        gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7);  // apply the projection matrix
    }

}
