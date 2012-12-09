package com.floern.rhabarber.logic.elements;

import java.util.ArrayList;
import java.util.Random;
import com.floern.rhabarber.util.FXMath;

import android.util.Log;
import at.emini.physics2D.Event;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;

public class GameWorld extends World {

	// separate list of players for easier retrieval of players
	private ArrayList<Player> players;
	private Random rand;
	private int max_x;
	private int max_y;

	public final float G = 100; // gravity

	public GameWorld() {
		super();
		// more than 4 players are probably not feasible anyway
		this.players = new ArrayList<Player>(4);
		rand = new Random();
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
