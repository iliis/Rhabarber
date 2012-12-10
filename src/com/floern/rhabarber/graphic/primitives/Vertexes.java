package com.floern.rhabarber.graphic.primitives;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.util.DynamicFloatBuffer;
import com.floern.rhabarber.util.Vector;

import android.opengl.GLES10;
import android.util.FloatMath;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.util.FXMatrix;
import at.emini.physics2D.util.FXVector;

public class Vertexes implements IGLPrimitive {
	
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
	
	public Vertexes(FXVector[] vs, FXVector displacement, FXMatrix rotation) {
		if(vs.length == 1) {
			// it's a circle!
			float r = vs[0].yAsFloat(); if (r<0) r=-r;
			float incr = (float) Math.PI / 16*2;
			float a = 0;
			vertices = new DynamicFloatBuffer(16*vertexDim);
			for(int i=0;i<16;i++) {
				addPoint(FloatMath.sin(a)*r+displacement.xAsFloat(), FloatMath.cos(a)*r+displacement.yAsFloat());
				a += incr;
			}
		}
		else {
			vertices = new DynamicFloatBuffer(vs.length*vertexDim);		
			for (FXVector v : vs) {
				FXVector vr = rotation.mult(v);
				vr.add(displacement);
				addPoint(vr.xAsFloat(), vr.yAsFloat());
			}
		}
	}
	
	public Vertexes(Body b) {
		this(b.shape().getCorners(), b.positionFX(), b.getRotationMatrix());
	}
	
	public void addPoint(float x, float y) {
		vertices.put(x);
		vertices.put(y);
		++length;
	}
	
	public void addPoint(Vector pos) {
		addPoint(pos.x, pos.y);
	}
	
	public void removePoint() {
		--length;
	}

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
