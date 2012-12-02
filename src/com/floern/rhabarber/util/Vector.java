package com.floern.rhabarber.util;

import at.emini.physics2D.util.FXUtil;
import at.emini.physics2D.util.FXVector;

public class Vector {
	public float x, y;
	
	public Vector() {
		x = 0;
		y = 0;
	}
	
	public Vector(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public void add(Vector v) {
		this.x += v.x;
		this.y += v.y;
	}
	
	public FXVector asFX() {
		return new FXVector(FXMath.floatToFX(x), FXMath.floatToFX(y));
	}
}
