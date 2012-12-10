package com.floern.rhabarber.logic.elements;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.graphic.primitives.IGLPrimitive;
import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.GameBodyUserData;

import android.opengl.GLES10;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Event;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;

public class GameWorld extends World {

	// separate list of players for easier retrieval of players
		// more than 4 players are probably not feasible anyway
	private ArrayList<Player> players = new ArrayList<Player>(4);
	private Random rand = new Random();
	
	private ArrayList<FXVector> spawnpoints_player   = new ArrayList<FXVector>();
	private ArrayList<FXVector> spawnpoints_treasure = new ArrayList<FXVector>();
	
	public final float G = 100; // gravity
	

	Vertexes outline;
	private float[] acceleration = new float[3];
	
	long last_tick;
	
	public float min_x, max_x, min_y, max_y; // size of landscape
	

	public GameWorld(InputStream level, Player p) {

		loadLevel(level);
		addPlayer(p);
		
		// test treasure
		addTreasure(new Treasure(300, 300, 42), new PhysicsEventListener() {
			public void eventTriggered(Event arg0, Object arg1) { }
		});

		outline = new Vertexes();
		outline.setMode(GLES10.GL_LINES); // disconnected bunch of lines
		outline.setThickness(3);
		
		
		for(int i = 0; i < getLandscape().segmentCount(); ++i) {
			FXVector A = getLandscape().startPoint(i);
			FXVector B = getLandscape().endPoint(i);
			
			outline.addPoint(A.xAsFloat(), A.yAsFloat());
			outline.addPoint(B.xAsFloat(), B.yAsFloat());
			
			min_x = Math.min(Math.min(A.xAsFloat(), B.xAsFloat()), min_x); max_x = Math.max(Math.max(A.xAsFloat(), B.xAsFloat()), max_x);
			min_y = Math.min(Math.min(A.yAsFloat(), B.yAsFloat()), min_y); max_y = Math.max(Math.max(A.yAsFloat(), B.yAsFloat()), max_y);
		}
		
		last_tick = System.nanoTime();
	}
	
	
	private void loadLevel(InputStream level)
	{
		this.addWorld(World.loadWorld(new PhysicsFileReader(level), new GameBodyUserData()));
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
	
	
	
	
	
	//calculates next state of world
	@Override
	public void tick()
	{
		// what about overflows? (so far I hadn't any bugs)
		long dt = System.nanoTime() - last_tick;
		last_tick = System.nanoTime();
		
		applyPlayerGravities(getTimestepFX(), acceleration);
		super.tick();
		
		for(Player p: getPlayers()) {
			p.animate(((float) dt) / 1000000000);
		}
	}
	
	public void setAccel(float[] g) {
		this.acceleration = g;
	}
	
	
	public void draw(GL10 gl)
	{
		gl.glColor4f(0.6f, 0.7f, 1, 1);
		outline.draw(gl);
		
		Body[] b = getBodies();
		for(int i = 0; i < getBodyCount(); i++) {
			if (b[i] instanceof IGLPrimitive) {
				// draw element
				((IGLPrimitive) b[i]).draw(gl);
			}
			else {
				// draw shape
				
				Vertexes verts = new Vertexes(b[i]);
				gl.glColor4f(1, 1, 1, 1);
				verts.setMode(GLES10.GL_LINE_LOOP);
				verts.draw(gl);
			}
		}
	}

}
