package com.floern.rhabarber.graphic.primitives;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.util.DynamicFloatBuffer;
import com.floern.rhabarber.util.Vector;

import android.opengl.GLES10;
import at.emini.physics2D.util.FXVector;

public class Vertexes extends GLPrimitive {
	
	static final int vertexDim = 2;   // two floats per vertex
	
	private DynamicFloatBuffer vertices; // every vertex has two floats (x and y)
	private int length = 0;       // in points
	
	private int mode = GLES10.GL_LINE_STRIP;
	private float thickness = 1;
	
	public Vertexes() {
		vertices = new DynamicFloatBuffer(2*vertexDim);
	}
	
	public Vertexes(FXVector[] vs) {
		vertices = new DynamicFloatBuffer(vs.length*vertexDim);
		for(FXVector v: vs) {
			addPoint(v.xAsFloat(), v.yAsFloat());
		}
	}
	
	public Vertexes(Vector[] vs) {
		vertices = new DynamicFloatBuffer(vs.length*vertexDim);
		for(Vector v: vs) {
			addPoint(v.x, v.y);
		}
	}
	
	public Vertexes(List<Vector> vs) {
		vertices = new DynamicFloatBuffer(vs.size()*vertexDim);
		for(Vector v: vs) {
			addPoint(v.x, v.y);
		}
	}
	
	public Vertexes(List<Vector> vs, Vector offset) {
		vertices = new DynamicFloatBuffer(vs.size()*vertexDim);
		for(Vector v: vs) {
			addPoint(v.x+offset.x, v.y+offset.y);
		}
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
		gl.glPointSize(thickness);
		
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
