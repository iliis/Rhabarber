package com.floern.rhabarber.logic.elements;

import javax.microedition.khronos.opengles.GL10;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

public class Fog extends AtmosphericElement {

	public Fog(Body b) {
		super(b);
	}

	public Fog(FXVector pos, Shape shape) {
		super(pos, shape, false);
	}

	public Fog(int x, int y, Shape shape) {
		super(x, y, shape, false);
	}

	
	public void draw(GL10 gl) {
		// TODO
	}

}
