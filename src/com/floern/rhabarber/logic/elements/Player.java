package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;


public class Player extends MovableElement {
	/*
	 * physical properties of players as used by physics engine
	 */
	private static final int hitBoxWidth = 10;
	private static final int hitBoxHeight = 20;
	private static final int mass = 50;
	private static final int elasticity = 30; //"bouncyness", 0% to 100% energy conserved
	private static final int friction = 10; // 0% to 100%
	
	//defines playernumber
	private int playerIdx;
	
	public FXVector playerGravity;
	
	public Player(FXVector pos,int playerIdx) {
		super(pos, Shape.createRectangle(hitBoxWidth, hitBoxHeight));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.playerIdx = playerIdx;
		this.playerGravity = new FXVector(0, 10);
		this.setGravityAffected(false);
	}

	public Player(int x, int y,int playerIdx) {
		super(x, y, Shape.createRectangle(hitBoxWidth, hitBoxHeight));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.playerIdx = playerIdx;
		this.playerGravity = new FXVector(0, 10);
		this.setGravityAffected(false);
	}
	
	public int getIdx()
	{
		return playerIdx;
	}
	
	//TODO add rotation stuff
}
