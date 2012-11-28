package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

public abstract class FixedElement extends ForegroundElement {

	public FixedElement(Body b) {
		super(b);
	}
	
	public FixedElement(FXVector pos, Shape shape) {
		super(pos, shape, false);
	}

	public FixedElement(int x, int y, Shape shape) {
		super(x, y, shape, false);
	}
}
