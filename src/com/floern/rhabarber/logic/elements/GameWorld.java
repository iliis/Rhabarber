package com.floern.rhabarber.logic.elements;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.R;
import com.floern.rhabarber.graphic.primitives.IGLPrimitive;
import com.floern.rhabarber.graphic.primitives.SkeletonKeyframe;
import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.network2.ClientStateAccumulator;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection;
import com.floern.rhabarber.network2.ClientStateAccumulator.Acceleration;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.Message;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.GameBodyUserData;

import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLES10;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Contact;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;

public class GameWorld extends World {

	// separate list of players for easier retrieval of players
	// more than 4 players are probably not feasible anyway
	public ArrayList<Player> players = new ArrayList<Player>(4);
	public ArrayList<Treasure> treasures = new ArrayList<Treasure>(2);
	private Random rand = new Random();
	private Resources resources = null;

	private boolean isServer;
	private int playerIdx = -1; // ID of the player on this device

	private FXVector sharedGravity;

	private ArrayList<FXVector> spawnpoints_player = new ArrayList<FXVector>();
	private Iterator<FXVector> playerSpawnIterator;
	private ArrayList<Body> spawnpoints_treasure = new ArrayList<Body>();

	private static final int[] playerColors = { Color.RED, Color.BLUE,
			Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.GRAY };
	private int colorIdx = 0;

	public final float G = 10; // gravity

	Vertexes outline; //< this is also used as a lock for receiving state from the server!
	private float[] acceleration = new float[3];

	long last_tick;

	public float min_x, max_x, min_y, max_y; // size of landscape

	// maybe push to phy file? yeah, later...
	private static final int WINNING_SCORE = 1000;
	private boolean game_finished = false;
	private int winner = -1;
	
	
	public boolean isFinished() {return game_finished;}
	public int     getWinner()  {return winner;}
	
	public void cancelGame() 			{game_finished = true; this.winner = -1;}
	public void stopGame(int winner)	{game_finished = true; this.winner = winner;}
	
	

	public GameWorld(InputStream level, Resources resources,
			boolean isServer, int playerIdx) {
		this.resources = resources;
		this.playerIdx = playerIdx;
		this.isServer = isServer;
		loadLevel(level);
		
		outline = new Vertexes();
		outline.setMode(GLES10.GL_LINES); // disconnected bunch of lines
		outline.setThickness(3);

		for (int i = 0; i < getLandscape().segmentCount(); ++i) {
			FXVector A = getLandscape().startPoint(i);
			FXVector B = getLandscape().endPoint(i);

			outline.addPoint(A.xAsFloat(), A.yAsFloat());
			outline.addPoint(B.xAsFloat(), B.yAsFloat());

			min_x = Math.min(Math.min(A.xAsFloat(), B.xAsFloat()), min_x);
			max_x = Math.max(Math.max(A.xAsFloat(), B.xAsFloat()), max_x);
			min_y = Math.min(Math.min(A.yAsFloat(), B.yAsFloat()), min_y);
			max_y = Math.max(Math.max(A.yAsFloat(), B.yAsFloat()), max_y);
		}
		this.playerSpawnIterator = spawnpoints_player.iterator();
		this.sharedGravity = this.getGravity();
		last_tick = System.nanoTime();
	}

	
	
	// call this once, as old data is not deleted!
	private void loadLevel(InputStream level) {
		this.addWorld(World.loadWorld(new PhysicsFileReader(level),
				new GameBodyUserData()));
		convertBodies();
	}

	
	
	// reads the additional UserData set in the Editor and uses it to create the
	// appropriate objects out of them
	// eg. it converts all Bodies marked as 'Treasure' into instances of
	// Treasure
	private void convertBodies() {
		Body[] b = getBodies().clone();
		final int N = getBodyCount();
		for (int i = 0; i < N; i++) {
			if (((GameBodyUserData) b[i].getUserData()).is_game_element) {
				removeBody(b[i]);
				convertAndAddBody(b[i]);
			}
		}
	}

	// Factory for game Elements, reads the UserData and creates the appropriate
	// Element
	// TODO: change this into void and add newly created element directly to
	// this world (by usint addPlayer(), addTreasure() etc.)
	private void convertAndAddBody(Body b) {
		GameBodyUserData userdata = (GameBodyUserData) b.getUserData();
		assert (userdata.is_game_element == true);
		assert (userdata.data.containsKey("element"));

		String type = userdata.data.get("element");

		if (type.equals("treasure")) {
			addTreasure(new Treasure(b.positionFX(),
					Integer.parseInt(userdata.data.get("value"))));
		} else if (type.equals("playerspawn")) {
			spawnpoints_player.add(b.positionFX());
		} else if (type.equals("treasurespawn")) {
			spawnpoints_treasure.add(b);
		} else
			Log.e("foo",
					"Unknown element of type '" + userdata.data.get("element")
							+ "' in GameWorld.convertBody()");
	}

	private void addPlayer(Player p) {
		this.addBody(p);
		players.add(p);
	}

	// don't forget to send word to clients! (GameNetworkingProtocolConnection.sendInsertTreasureMessage())
	public void addTreasure(Treasure t) {
		this.addBody(t);
		this.treasures.add(t);
	}

	public void addTreasureRandomly() {
		Treasure t = new Treasure(new FXVector(), 0);
		moveTreasureRandomly(t);
		addTreasure(t);
	}

	public void moveTreasureRandomly(Treasure t) {
		if (!spawnpoints_treasure.isEmpty()) {

			Body spawnpoint = this.spawnpoints_treasure.get(rand
					.nextInt(spawnpoints_treasure.size()));
			GameBodyUserData d = (GameBodyUserData) spawnpoint.getUserData();

			t.setPositionFX(spawnpoint.positionFX());
			t.setValue(Integer.parseInt(d.data.get("value")));
		} else {
			Log.e("foo",
					"No treasure spawnpoints defined in map (GameWorld.addTreasureRandomly)");
		}

	}
	
	public int createPlayer() {
		if (!playerSpawnIterator.hasNext()) {
			this.playerSpawnIterator = spawnpoints_player.iterator();
		}
		
		colorIdx++;
		if (colorIdx >= playerColors.length) {
			colorIdx = 0;

		}
		
		return addPlayer(playerSpawnIterator.next(), players.size(), playerColors[colorIdx], 1000);
	}

	public int addPlayer(FXVector pos, int Idx, int color, int winning_score)
	// return index of added player
	{
		Player p = new Player(pos, Idx, resources.openRawResource(R.raw.player),color, winning_score);
		p.anim_running_left  = SkeletonKeyframe.loadSKAnimation(p.skeleton, resources.openRawResource(R.raw.player_running_left));
		p.anim_running_right = SkeletonKeyframe.loadSKAnimation(p.skeleton, resources.openRawResource(R.raw.player_running_right));
		p.anim_standing      = SkeletonKeyframe.loadSKAnimation(p.skeleton, resources.openRawResource(R.raw.player_standing));
		p.setActiveAnim(p.anim_running_right);
		
		addPlayer(p);
		
		if(p.getIdx() == this.playerIdx)
			p.is_local_player = true;
		
		return p.getIdx();
	}

	public void applyPlayerGravities(int timestep, float[] acceleration) {
		// pushed sharedGravity calculation to copyInputsFromAccumulator because
		// there the individual player gravities get handled anyway
		for (Player p : players) {
			p.applyAcceleration(p.playerGravity, timestep);
			p.setRotationFromGravity();
		}

		setGravity(sharedGravity);
	}

	public ArrayList<Player> getPlayers() {
		return players;
	}

	private void processGame() {
		// rhabarberbarbarabar
		checkTreasureCollected();
	}

	private void checkTreasureCollected() {
		for (Treasure t : treasures) {
			Contact[] contacts = getContactsForBody(t);
			for (Contact c : contacts) {
				if (c != null) {
					if (c.body1() != null && c.body1() instanceof Player)
						onTreasureCollected((Player) c.body1(), t);
					else if (c.body2() != null && c.body2() instanceof Player)
						onTreasureCollected((Player) c.body2(), t);
				}
			}
		}
	}

	private void onTreasureCollected(Player p, Treasure t) {
		moveTreasureRandomly(t);
		p.score += t.getValue();

		if (p.score >= WINNING_SCORE)
			onGameFinished(p);
	}

	private void onGameFinished(Player winner) {
		this.game_finished = true;
		this.winner = winner.getIdx();

	}

	// calculates next state of world
	@Override
	public void tick() {
		
		// what about overflows? (so far I hadn't any bugs)
		final long         t = System.nanoTime();
		final long        dt = t - last_tick;
		           last_tick = t;
		
		if (isServer) {
			applyPlayerGravities(getTimestepFX(), acceleration);
			super.tick(); // simulate physics

			for (Player p : getPlayers()) {
				p.update();
				p.animate(((float) dt) / 1000000000); // convert ns to seconds
			}

			this.processGame();
			
			
		} else {
			for (Player p : getPlayers()) {
				// this is done on server only, sets correct speed // p.update();
				p.animate(((float) dt) / 1000000000); // convert ns to seconds
			}
		}
	}

	// server side
	public void copyInputsFromAccumulator(ClientStateAccumulator stateAccumulator) {
		ClientStateAccumulator copy;
		synchronized (stateAccumulator) {
			copy = stateAccumulator.copy();
		}

		// shared gravity is normal (global) gravity and players are just not
		// affected by normal gravity
		// as of
		// https://trello.com/card/gemeinsame-gravitation-normalisieren/50a0c9d75e0399ad5e0201ca/20
		sharedGravity.mult(0);
		
		// add playerindex to clientAccumulator so the values get matched to the right player?
		// for now, it's working with just using the array indices. this should be correct.
		for(int i = 0; i < copy.accels.length; ++i) {
		
			final Player       p = players.get(i);
			final Acceleration a = copy.accels[i];
			
			if(a != null) {
				p.playerGravity = new FXVector(	FXMath.floatToFX(a.x),
												FXMath.floatToFX(a.y));
				p.playerGravity.normalize();
				p.playerGravity.multFX(FXMath.floatToFX(G));
			}
			
			if(copy.inputs[i] != null)
				p.walk(copy.inputs[i]);

			sharedGravity.add(p.playerGravity);
		}

		sharedGravity.normalize();
		sharedGravity.multFX(FXMath.floatToFX(G));
	}

	// server side
	public void sendStateToClients(ArrayList<GameNetworkingProtocolConnection> clients) {
		for(GameNetworkingProtocolConnection c: clients) {
			c.sendServerState(getBodiesAsList(), players, treasures);
		}
	}

	// client side
	public void receiveStateFromServer(Message m) {
		// TODO: run this in OpenGL thread somehow
		synchronized (outline) {
			GameNetworkingProtocolConnection.receiveServerState(m, this);
		}
	}

	public void setAccel(float[] g) {
		this.acceleration = g;
	}
	
	/**
	 * Convert the internal Body[]-Array into a List<Body> which does not contain null elements.
	 * @return all bodies (inclusive players and treasures) of this world
	 */
	public ArrayList<Body> getBodiesAsList() {
		ArrayList<Body> b = new ArrayList<Body>(getBodyCount());
		
		for(int i = 0; i < getBodies().length; ++i) {
			if(getBodies()[i] != null)
				b.add(getBodies()[i]);
		}
		
		return b;
	}
	
	public Body getBodyByID(int id) {
		for(Body b: getBodies()) {
			if(b != null && b.getId() == id)
				return b;
		}
		
		return null;
	}

	// use this only in clients
	/*public void walk(ClientStateAccumulator.UserInputWalk direction) {
				if (playerIdx >= 0) {
				players.get(playerIdx).walk(direction);
		}
	}*/

	public void draw(GL10 gl) {
		synchronized (outline) {
			gl.glColor4f(0.6f, 0.7f, 1, 1);
			outline.draw(gl);
	
			for (Body b: getBodiesAsList()) {
				if (b instanceof IGLPrimitive) {
					// draw element
					((IGLPrimitive) b).draw(gl);
				} else {
					// draw shape
	
					Vertexes verts = new Vertexes(b);
					gl.glColor4f(1, 1, 1, 1);
					verts.setMode(GLES10.GL_LINE_LOOP);
					verts.draw(gl);
				}
			}
		}
	}
	
	public void logBodies() {
		for (Body b: getBodiesAsList()) {
			if (b instanceof Player) {
				Log.d("foo", "PLAYER:");
			} else if (b instanceof Treasure) {
				Log.d("foo", "TREASURE:");
			}
			
			Body bb = getBodies()[b.getId()];
			Log.d("foo", "["+b.getId()+" = "+(bb!=null?bb.getId():"NULL")+"] position: "+b.positionFX().xAsFloat()+" \t "+b.positionFX().yAsFloat());
		}
	}

}
