package com.floern.rhabarber.graphic.primitives;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.util.DynamicFloatBuffer;

import android.opengl.GLES10;

public class Line extends GLPrimitive {
	
	static final int vertexDim = 2;   // two floats per vertex
	
	private DynamicFloatBuffer vertices; // every vertex has two floats (x and y)
	private int length = 0;       // in points
	
	private boolean closed = false;
	private float thickness = 1;
	
	public Line() {
		vertices = new DynamicFloatBuffer(2*vertexDim);
	}
	
	public void addPoint(float x, float y) {
		vertices.put(x);
		vertices.put(y);
		++length;
	}
	
	public void removePoint() {
		--length;
	}

	@Override
	public void draw(GL10 gl) {
		
		// add color here
		gl.glLineWidth(thickness);
		
		gl.glVertexPointer(2, GLES10.GL_FLOAT, 0, vertices.get());
		if (closed)
			gl.glDrawArrays(GLES10.GL_LINE_LOOP,  0, length);
		else
			gl.glDrawArrays(GLES10.GL_LINE_STRIP, 0, length);
	}
	
	
	public Line setThickness(float t) {
		this.thickness = t;
		return this;
	}
	
	public Line setClosed(boolean closed) {
		this.closed = closed;
		return this;
	}

}
