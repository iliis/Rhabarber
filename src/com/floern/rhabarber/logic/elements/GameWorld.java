package com.floern.rhabarber.logic.elements;

import java.util.ArrayList;
import java.util.Random;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.GameBodyUserData;

import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Event;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;

public class GameWorld extends World {

	// separate list of players for easier retrieval of players
		// more than 4 players are probably not feasible anyway
	private ArrayList<Player> players = new ArrayList<Player>(4);
	private Random rand = new Random();
	
	private ArrayList<FXVector> spawnpoints_player   = new ArrayList<FXVector>();
	private ArrayList<FXVector> spawnpoints_treasure = new ArrayList<FXVector>();
	
	public final float G = 100; // gravity

	public GameWorld() {
		super();
	}
	
	public GameWorld(World w)
	{
		super(w);
		convertBodies();
	}
	
	// reads the additional UserData set in the Editor and uses it to create the appropriate objects out of them
	// eg. it converts all Bodies marked as 'Treasure' into instances of Treasure 
	private void convertBodies() {
		Body[] b = getBodies().clone();
		final int N = getBodyCount();
		for(int i = 0; i < N; i++) {
			if (((GameBodyUserData) b[i].getUserData()).is_game_element) {
				Log.d("foo", "removing a body");
				removeBody(b[i]);
				
				convertAndAddBody(b[i]);
			}
		}
	}
	
	// Factory for game Elements, reads the UserData and creates the appropriate Element
	// TODO: change this into void and add newly created element directly to this world (by usint addPlayer(), addTreasure() etc.)
	private void convertAndAddBody(Body b) {
		GameBodyUserData userdata = (GameBodyUserData) b.getUserData();
		assert (userdata.is_game_element == true);
		assert (userdata.data.containsKey("element"));
		
		String type = userdata.data.get("element");
		
		if (type.equals("treasure")) {
			addTreasure(new Treasure(b.positionFX(), Integer.parseInt(userdata.data.get("value"))), new PhysicsEventListener() {
				public void eventTriggered(Event arg0, Object arg1) { }
			});
		}
		else
		if (type.equals("playerspawn")) {
			spawnpoints_player.add(b.positionFX());
		}
		else
		if (type.equals("treasurespawn")) {
			spawnpoints_treasure.add(b.positionFX());
		}
		else
			Log.e("foo", "Unknown element of type '"+userdata.data.get("element")+"' in GameWorld.convertBody()");
	}

	public void addPlayer(Player p) {
		this.addBody(p);
		players.add(p);
	}

	public void addTreasure(Treasure t, PhysicsEventListener l) {
		this.addBody(t);
		Event collectedEvent = Event.createBodyEvent(t, t.shape(),
				Event.TYPE_BODY_COLLISION, 1, 1, 1, 1);
		this.addEvent(collectedEvent);
		this.setPhysicsEventListener(l);
	}

	public void addTreasureRandomly(int treasureValue, PhysicsEventListener l) {
		/*if (max_x == 0 && max_y == 0) {
			setBotLeft();
		}
		int x = rand.nextInt(max_x - (max_x/10));
		x += max_x/20;
		int y = rand.nextInt(max_y - (max_y/10));
		y += max_y/20;*/
		
		if (!spawnpoints_treasure.isEmpty()) {
			
			FXVector pos = this.spawnpoints_treasure.get(rand.nextInt(spawnpoints_treasure.size()));
			Treasure t   = new Treasure(pos, treasureValue);
			this.addTreasure(t, l);
		} else {
			Log.e("foo", "No treasure spawnpoints defined in map (GameWorld.addTreasureRandomly)");
		}
	}

	public void applyPlayerGravities(int timestep, float[] acceleration) {
		// shared gravity is normal (global) gravity and players are just not
		// affected by normal gravity
		FXVector sharedGravity = new FXVector(
				FXMath.floatToFX(acceleration[1]),
				FXMath.floatToFX(acceleration[0]));

		// as of
		// https://trello.com/card/gemeinsame-gravitation-normalisieren/50a0c9d75e0399ad5e0201ca/20
		sharedGravity.normalize();
		sharedGravity.multFX(FXMath.floatToFX(G));

		for (Player p : players) {
			// TODO: change when distributed gravity is ready
			p.playerGravity = sharedGravity;
			p.applyAcceleration(p.playerGravity, timestep);
			p.setRotationFromGravity();
			// sharedGravity.add(p.playerGravity);
		}

		setGravity(sharedGravity);
	}

	public ArrayList<Player> getPlayers() {
		return players;
	}

}
