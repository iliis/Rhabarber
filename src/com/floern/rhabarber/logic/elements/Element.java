package com.floern.rhabarber.logic.elements;

import com.floern.rhabarber.graphic.primitives.IGLPrimitive;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

public abstract class Element extends Body implements IGLPrimitive {

	/*
	 * defines how Elements stack on each other
	 * higher number means drawn in front, player layer is 0
	 */
	public int layer;

	public Element(Body b) {
		super(b);
	}

	public Element(FXVector pos, Shape shape, boolean dynamic) {
		super(pos, shape, dynamic);
	}

	public Element(int x, int y, Shape shape, boolean dynamic) {
		super(x, y, shape, dynamic);
	}
}
