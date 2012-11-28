package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Body;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;


public abstract class AtmosphericElement extends Element {

	public AtmosphericElement(Body b) {
		super(b);
		//atmospheric layers are drawn on top of players (eg fog)
		this.layer = 1;
	}
	
	public AtmosphericElement(FXVector pos, Shape shape, boolean dynamic)
	{
		super(pos, shape, dynamic);
		this.layer = 1;
	}
	
	public AtmosphericElement(int x, int y, Shape shape, boolean dynamic)
	{
		super(x,y,shape,dynamic);
		this.layer = 1;
	}
	
}
