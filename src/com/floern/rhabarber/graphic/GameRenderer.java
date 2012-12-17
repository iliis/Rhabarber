package com.floern.rhabarber.graphic;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.GameActivity;
import com.floern.rhabarber.logic.elements.GameWorld;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;

public class GameRenderer implements GLSurfaceView.Renderer {
	
	private float
		world_min_x = 0,
		world_min_y = 0,
		world_max_x = 800,
		world_max_y = 480;
	
	public GameActivity render_callback;

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background frame color
        gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);

        // Alphablending
	    gl.glEnable(GLES10.GL_BLEND);
	    gl.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);

        // Enable use of vertex arrays
        gl.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
		
        // try some smoothing (AA)
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
	}
	
	public void setRendererCallback(GameActivity a) {
		render_callback = a;
	}
	
	public void readLevelSize(GameWorld game) {
		world_min_x = game.min_x;
		world_max_x = game.max_x;
		world_min_y = game.min_y;
		world_max_y = game.max_y;
	}
	

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		
		gl.glDisable(GLES10.GL_DEPTH_TEST); // this is a 2D game, layering is handled by drawing order
		
		gl.glViewport(0, 0, width, height);
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		
		
		// map OpenGL coordinates to world coordinates (minimum being top left) respecting screen size & density
		
		final float aspect = ((float) width) / height;
		final float world_width  = world_max_x-world_min_x; 
		final float world_height = world_max_y-world_min_y; 
		
		if (world_width >= world_height * aspect) {
			// map is wider than high (even considering a non-quadratic screen)
			final float h  = world_width/aspect;
			gl.glOrthof(world_min_x,
						world_max_x,
						world_min_y+h+(world_height-h)/2,
						world_min_y+  (world_height-h)/2,
						-1f, 1f);
		} else {
			// map is higher than wide (even considering a non-quadratic screen)
			final float w  = world_height*aspect;
			gl.glOrthof(world_min_x+  (world_width -w)/2,
						world_min_x+w+(world_width -w)/2,
						world_max_y,
						world_min_y,
						-1f, 1f);
		}
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	public void onDrawFrame(GL10 gl) {
		
		gl.glClear(GLES10.GL_COLOR_BUFFER_BIT);
		
		render_callback.onDraw(gl);
	}
}
