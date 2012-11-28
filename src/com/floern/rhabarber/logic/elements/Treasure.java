package com.floern.rhabarber.logic.elements;

import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

public class Treasure extends MovableElement {

	/*
	 * physical properties of treasures as used by physics engine assume
	 * treasures have a round "hitbox"
	 */
	private static final int hitCircleWidth = 10;
	private static final int mass = 20;
	private static final int elasticity = 30; // "bouncyness", 0% to 100% energy
												// conserved
	private static final int friction = 10; // 0% to 100%

	public FXVector playerGravity;

	public Treasure(FXVector pos, int playerIdx) {
		super(pos, Shape.createCircle(hitCircleWidth));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.playerGravity = new FXVector(0, 10);
	}

	public Treasure(int x, int y, int playerIdx) {
		super(x, y, Shape.createCircle(hitCircleWidth));
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.playerGravity = new FXVector(0, 10);
	}

}
