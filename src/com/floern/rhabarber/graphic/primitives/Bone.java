package com.floern.rhabarber.graphic.primitives;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import android.util.FloatMath;
import at.emini.physics2D.util.FXVector;

import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.Vector;


public class Bone {
	
	public List<Bone> children = new LinkedList<Bone>();
	public Bone parent = null;
	
	public String name;
	
	float angle, length;
	
	public Bone() {
		angle  = 0;
		length = 0;
		setRandomName();
	}
	
	public Bone(float a, float l, String n) {
		angle  = a;
		length = l;
		name   = n;
	}
	
	
	
	
	public void setRandomName() {
		this.name = "b_" + Integer.toString((int) (Math.random()*Integer.MAX_VALUE));
	}
	
	public void addBone(Bone b) {
		this.children.add(b);
		b.setParent(this);
	}
	
	public boolean empty() {
		return children.isEmpty();
	}
	
	public boolean hasParent() {
		return this.parent != null;
	}
	
	public void setParent(Bone b) {
		this.parent = b;
	}
	
	public Vector getVect() {
		float a = this.getAngle();
		return new Vector(	FloatMath.cos(a)*length,
							FloatMath.sin(a)*length);
	}
	
	public Vector getAbsoluteVect() {
		if(this.hasParent()) {
			Vector pv = this.parent.getAbsoluteVect();
			pv.add(this.getVect());
			return pv;
		} else {
			return this.getVect();
		}
	}
	
	
	public float getAngle() {
		if(this.hasParent()) {
			return this.parent.getAngle() + this.angle;
		} else {
			return this.angle;
		}
	}
	
	
	public void getAllVectors(List<Vector> out) {
		if(this.hasParent())
			out.add(parent.getAbsoluteVect());
		else
			out.add(new Vector());
		
		out.add(this.getAbsoluteVect());
		for(Bone b: this.children) {
			b.getAllVectors(out);
		}
	}
}
