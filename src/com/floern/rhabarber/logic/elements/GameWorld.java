package com.floern.rhabarber.logic.elements;

import java.util.ArrayList;
import java.util.Random;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.GameBodyUserData;

import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Event;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.UserData;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;

public class GameWorld extends World {

	// separate list of players for easier retrieval of players
		// more than 4 players are probably not feasible anyway
	private ArrayList<Player> players = new ArrayList<Player>(4);
	private Random rand = new Random();
	private int max_x;
	private int max_y;
	
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
				
				addBody((Body) convertBody(b[i]));
			}
		}
	}
	
	// Factory for game Elements, reads the UserData and creates the appropriate Element
	// TODO: change this into void and add newly created element directly to this world (by usint addPlayer(), addTreasure() etc.)
	private Element convertBody(Body b) {
		GameBodyUserData userdata = (GameBodyUserData) b.getUserData();
		assert (userdata.is_game_element == true);
		
		Log.d("foo", "has key element? "+(userdata.data.containsKey("element")?"yes":"no"));
		
		assert (userdata.data.containsKey("element"));
		
		if (userdata.data.get("element").equals("treasure")) {
			return new Treasure(b.positionFX(), Integer.parseInt(userdata.data.get("value")));
		}
		
		Log.e("foo", "Unknown element of type '"+userdata.data.get("element")+"' in GameWorld.convertBody()");
		return null;
	}

	private void setBotLeft() {
		FXVector[] corners = this.getLandscape().elementStartPoints();
		int FXmax_x = 0;
		int FXmax_y = 0;
		for (FXVector fxVector : corners) {
			if (fxVector != null) {
				if (fxVector.xFX > FXmax_x) {
					FXmax_x = fxVector.xFX;
				}
				if (fxVector.yFX > FXmax_y) {
					FXmax_y = fxVector.yFX;
				}
			}
		}
		max_x = FXmax_x >> 12;
		max_y = FXmax_y >> 12;
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
		if (max_x == 0 && max_y == 0) {
			setBotLeft();
		}
		int x = rand.nextInt(max_x - (max_x/10));
		x += max_x/20;
		int y = rand.nextInt(max_y - (max_y/10));
		y += max_y/20;
		Treasure t = new Treasure(x, y, treasureValue);
		this.addTreasure(t, l);
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
