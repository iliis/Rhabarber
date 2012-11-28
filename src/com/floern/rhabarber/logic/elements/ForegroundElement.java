package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

/*
 * Class for main layer stuff (eg player layer)
 */
public abstract class ForegroundElement extends Element {
	
	public ForegroundElement(Body b) {
		super(b);
		this.layer = 0;
	}
	
	public ForegroundElement(FXVector pos, Shape shape, boolean dynamic)
	{
		super(pos, shape, dynamic);
		this.layer = 0;
	}
	
	public ForegroundElement(int x, int y, Shape shape, boolean dynamic)
	{
		super(x,y,shape,dynamic);
		this.layer = 0;
	}

}
