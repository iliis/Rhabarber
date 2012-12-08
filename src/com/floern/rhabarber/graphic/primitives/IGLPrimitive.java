package com.floern.rhabarber.graphic.primitives;

import javax.microedition.khronos.opengles.GL10;

public interface IGLPrimitive {
	
	/**
	 * Draw the element with OpenGL
	 * @param gl
	 */
	void draw(GL10 gl);

}
