package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

public abstract class BackgroundElement extends Element{

	public BackgroundElement(Body b) {
		super(b);
		this.layer = -1;
	}
	
	public BackgroundElement(FXVector pos, Shape shape, boolean dynamic)
	{
		super(pos, shape, dynamic);
		this.layer = -1;
	}
	
	public BackgroundElement(int x, int y, Shape shape, boolean dynamic)
	{
		super(x,y,shape,dynamic);
		this.layer = -1;
	}

}
