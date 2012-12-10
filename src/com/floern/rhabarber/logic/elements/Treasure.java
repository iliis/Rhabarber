package com.floern.rhabarber.logic.elements;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.Vector;

import android.opengl.GLES10;
import android.util.FloatMath;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXMatrix;
import at.emini.physics2D.util.FXVector;

public class Treasure extends MovableElement {

	/*
	 * physical properties of treasures as used by physics engine assume
	 * treasures have a round "hitbox"
	 */
	private static final int hitCircleWidth = 10;
	private static final int mass = 20;
	private static final int elasticity = 30; // "bouncyness", 0% to 100% energy
												// conserved
	private static final int friction = 10; // 0% to 100%
	
	//value as in score
	private int value;

	public Treasure(FXVector pos, int value) {
		super(pos, Shape.createCircle(hitCircleWidth));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.value = value;
	}

	public Treasure(int x, int y, int value) {
		super(x, y, Shape.createCircle(hitCircleWidth));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}

	
	public void draw(GL10 gl) {
		Vertexes verts = new Vertexes(starVertices.toArray(new FXVector[starVertices.size()]), positionFX(), getRotationMatrix());
		
		if (value < 0) // bad treasure -> draw red
			gl.glColor4f(1, .1f, 0, 1);
		else // good treasure -> draw yellow
			gl.glColor4f(1, .9f, 0, 1);
		
		verts.setMode(GLES10.GL_LINE_LOOP);
		verts.setThickness(2);
		verts.draw(gl);
	}
	
	
	@SuppressWarnings("serial")
	private final List<FXVector> starVertices = new ArrayList<FXVector>(){{
		// compute initial star spike coordinates
		int radius = hitCircleWidth;
		float angleStep = ((float) Math.PI) / 5;
		float currAngle = 0f;
		for (int i=0; i<2*5; ++i) {
			float scale = radius * (i%2==0?1f:.4f);
			float x = FloatMath.cos(currAngle) * scale;
			float y = FloatMath.sin(currAngle) * scale;
			add(new FXVector(FXMath.floatToFX(x), FXMath.floatToFX(y)));
			currAngle += angleStep;
		}
	}};
	
}
