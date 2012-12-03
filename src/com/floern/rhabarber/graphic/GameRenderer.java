package com.floern.rhabarber.graphic;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.GameActivity;
import com.floern.rhabarber.physics.PhysicsController;

import android.opengl.GLES10;
import android.opengl.GLSurfaceView;

public class GameRenderer implements GLSurfaceView.Renderer {
	
	public GameActivity render_callback;

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background frame color
        gl.glClearColor(0f, 0f, 0f, 1f);

        // Alphablending
	    gl.glEnable(GLES10.GL_BLEND);
	    gl.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);

        // Enable use of vertex arrays
        gl.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
		
        
	}
	
	public void setRendererCallback(GameActivity a) {
		render_callback = a;
	}
	

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		
		gl.glDisable(GLES10.GL_DEPTH_TEST); // this is a 2D game, layering is handled by drawing order
		
		gl.glViewport(0, 0, width, height);
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		// for now, map pixels to OpenGL coordinates (0,0 being top left)
		gl.glOrthof(0.0f, width, height, 0.0f, -1.0f, 1.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		
	}
	
	public void onDrawFrame(GL10 gl) {
		
		gl.glClear(GLES10.GL_COLOR_BUFFER_BIT);
		
		render_callback.onDraw(gl);
	}
}
