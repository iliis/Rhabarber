package com.floern.rhabarber.logic.elements;

import java.util.ArrayList;

import com.floern.rhabarber.util.FXMath;

import at.emini.physics2D.Event;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;

public class GameWorld extends World{
	
	//separate list of players for easier retrieval of players
	private ArrayList<Player> players;
	
	public GameWorld()
	{
		super();
		//more than 4 players are probably not feasible anyway
		this.players = new ArrayList<Player>(4);
	}
	
	public void addPlayer(Player p)
	{
		this.addBody(p);
		players.add(p);
	}
	
	public void addTreasure(Treasure t, PhysicsEventListener l)
	{
		this.addBody(t);
		Event collectedEvent = Event.createBodyEvent(t, t.shape(), Event.TYPE_BODY_COLLISION, 1, 1, 1, 1);
		this.addEvent(collectedEvent);
		this.setPhysicsEventListener(l);
		
	}
	
	public void applyPlayerGravities(int timestep, float[] acceleration)
	{
		//shared gravity is normal (global) gravity and players are just not affected by normal gravity
		FXVector sharedGravity = new FXVector(FXMath.floatToFX(acceleration[1]), FXMath.floatToFX(acceleration[0]));
		
		for (Player p : players) {
			//TODO: change when distributed gravity is ready
			p.playerGravity = sharedGravity;
			p.applyAcceleration(p.playerGravity, timestep);
			p.setRotationFromGravity();
			//sharedGravity.add(p.playerGravity);
		}
		
		//sharedGravity.normalize()?
		setGravity(sharedGravity);
	}
	
	public ArrayList<Player> getPlayers()
	{
		return players;
	}
	
}
