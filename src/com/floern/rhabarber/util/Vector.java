package com.floern.rhabarber.util;

import android.util.FloatMath;
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
	
	public Vector(FXVector vFX) {
		this.x = vFX.xAsFloat();
		this.y = vFX.yAsFloat();
	}

	public void add(Vector v) {
		this.x += v.x;
		this.y += v.y;
	}
	
	public Vector plus(Vector v) {
		return new Vector(this.x + v.x, this.y + v.y);
	}
	
	public Vector minus(Vector v) {
		return new Vector(this.x - v.x, this.y - v.y);
	}
	
	public Vector normalized() {
		return this.times(1.0f/this.length());
	}
	
	public Vector times(float v) {
		return new Vector(this.x * v, this.y * v);
	}
	
	public float length() {
		return FloatMath.sqrt(x*x + y*y);
	}
	
	public Vector rotCW() {
		return new Vector(this.y, -this.x);
	}
	
	public Vector rotCCW() {
		return new Vector(-this.y, this.x);
	}
	
	public Vector rotCCW(float a) {
		return new Vector(	FloatMath.cos(a)*this.x - FloatMath.sin(a)*this.y,
							FloatMath.sin(a)*this.x + FloatMath.cos(a)*this.y);
	}
	
	public Vector rotCW(float a) {
		return this.rotCCW(-a);
	}
	
	public FXVector asFX() {
		return new FXVector(FXMath.floatToFX(x), FXMath.floatToFX(y));
	}

	/**
	 * performs an element-wise multiplication
	 *     this.x * v.x
	 *     this.y * v.y
	 * @param v
	 * @return the result
	 */
	public Vector times(Vector v) {
		return new Vector(this.x * v.x, this.y * v.y);
	}
	
	public Vector divided(Vector v) {
		return new Vector(this.x / v.x, this.y / v.y);
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		result.append("[ ");
		result.append(this.x);
		result.append(" ,  ");
		result.append(this.y);
		result.append(" ]");
		
		return result.toString();
	}
}
