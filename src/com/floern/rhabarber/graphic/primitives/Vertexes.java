package com.floern.rhabarber.graphic.primitives;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.util.DynamicFloatBuffer;

import android.opengl.GLES10;

public class Vertexes extends GLPrimitive {
	
	static final int vertexDim = 2;   // two floats per vertex
	
	private DynamicFloatBuffer vertices; // every vertex has two floats (x and y)
	private int length = 0;       // in points
	
	private int mode = GLES10.GL_LINE_STRIP;
	private float thickness = 1;
	
	public Vertexes() {
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
		gl.glDrawArrays(mode,  0, length);
	}
	
	
	public Vertexes setThickness(float t) {
		this.thickness = t;
		return this;
	}
	
	public Vertexes setMode(int m) {
		this.mode = m;
		return this;
	}

}
