package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.World;

public class GameWorld extends World{
	
	public GameWorld()
	{
		super();
		
		//disable global Gravity
		this.setGravity(0);
	}

}
