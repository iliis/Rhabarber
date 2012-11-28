package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

/*
 * everything dynamic
 */

public abstract class MovableElement extends ForegroundElement {

	public MovableElement(Body b) {
		super(b);
	}
	
	public MovableElement(FXVector pos, Shape shape) {
		super(pos, shape, true);
	}

	public MovableElement(int x, int y, Shape shape) {
		super(x, y, shape, true);
	}

}
