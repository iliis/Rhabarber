package com.floern.rhabarber.logic.elements;

import java.io.InputStream;

import com.floern.rhabarber.graphic.primitives.Skeleton;

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
	
	// graphics properties (yeah, this may belong somewhere else...
	public Skeleton skeleton;
	
	public Player(FXVector pos, int playerIdx, InputStream skeleton) {
		this(pos.xFX, pos.yFX, playerIdx, skeleton);
	}

	public Player(int x, int y,int playerIdx, InputStream skeleton) {
		super(x, y, Shape.createRectangle(hitBoxWidth, hitBoxHeight));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.playerIdx = playerIdx;
		this.playerGravity = new FXVector(0, 10);
		//this.setGravityAffected(false);
		this.skeleton = new Skeleton(skeleton, 0.04f);
	}
	
	public int getIdx()
	{
		return playerIdx;
	}
	
	//TODO add rotation stuff
}
