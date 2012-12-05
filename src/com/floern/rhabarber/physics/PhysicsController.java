package com.floern.rhabarber.physics;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.opengl.GLES10;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Landscape;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXMatrix;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;

import com.floern.rhabarber.R;
import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.Vector;

public class PhysicsController {
	
	private GameWorld world;
	//separate list of players for easier retrieval of players
	private ArrayList<Player> players;
	
	
	// tmp
	Vertexes outline;
	private float[] acceleration = new float[3];
	
	long last_tick;
	
	
	public PhysicsController(InputStream level, Player p) {
		
		World tmpWorld = World.loadWorld(new PhysicsFileReader(level));
		this.world = new GameWorld();
		world.setLandscape(tmpWorld.getLandscape());s
		
		//more than 4 players are probably not feasible anyway
		this.players = new ArrayList<Player>(4);
		addPlayer(p);
		//loadLevel(level);
		
		
		outline = new Vertexes();
		outline.setMode(GLES10.GL_LINES); // disconnected bunch of lines
		outline.setThickness(3);
		
		
		for(int i = 0; i < world.getLandscape().segmentCount(); ++i) {
			FXVector A = world.getLandscape().startPoint(i);
			FXVector B = world.getLandscape().endPoint(i);
			
			outline.addPoint(A.xAsFloat(), A.yAsFloat());
			outline.addPoint(B.xAsFloat(), B.yAsFloat());
		}
		
		last_tick = System.nanoTime();
	}
	
	
	//TODO adapt to android file handling
	private void loadLevel(File data)
	{
		
		// for more advanced stuff, maybe AssetManager would be better
		PhysicsFileReader reader = new PhysicsFileReader(data);
		world.setLandscape(Landscape.loadLandscape(reader));
	}
	
	public void addPlayer(Player p)
	{
		world.addBody(p);
		players.add(p);
	}
	
	//calculates next state of world
	public void tick()
	{
		// what about overflows? (so far I hadn't any bugs)
		long dt = System.nanoTime() - last_tick;
		last_tick = System.nanoTime();
		
		int timestep = world.getTimestepFX();
		applyPlayerGravities(timestep);
		world.tick();
		
		for(Player p: players) {
			p.animate(((float) dt) / 1000000000);
		}
	}
	
	public void setAccel(float[] g) {
		this.acceleration = g;
	}
	
	private void applyPlayerGravities(int timestep)
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
		world.setGravity(sharedGravity);
	}
	
	public void draw(GL10 gl)
	{
		/*
		 * TODO: Flo, insert openGL magic here
		 * world.getBodies(), world.getLandscape() might come in handy
		 * 
		 * Also beware of fixed point vectors (FXVector) -> see emini physics documentation
		 */
		
		gl.glColor4f(0.6f, 0.7f, 1, 1);
		outline.draw(gl);
		
		Body[] b = world.getBodies();
		for(int i = 0; i < world.getBodyCount(); i++) {
			FXVector[] vertsFX = b[i].shape().getCorners();
			FXVector pos = b[i].positionFX();
			Vertexes verts = new Vertexes(); verts.setMode(GLES10.GL_LINE_LOOP);
			FXMatrix rot = b[i].getRotationMatrix();
			for(FXVector v: vertsFX) {
				FXVector vr = rot.mult(v);
				verts.addPoint(vr.xAsFloat()+pos.xAsFloat(), vr.yAsFloat()+pos.yAsFloat());
			}
			
			if (b[i] instanceof Player) {
				gl.glColor4f(1, 0.2f, 0, 1);
				
				Player p = (Player) b[i];
				p.skeleton.position = new Vector(p.positionFX().xAsFloat(), p.positionFX().yAsFloat()-4);
				p.skeleton.rotation = FXMath.FX2toFloat(p.rotation2FX());
				p.skeleton.draw(gl);
				
				gl.glColor4f(1, 0.2f, 0, 0.6f);
				
			} else {
				gl.glColor4f(1, 1, 1, 1);
			}
			
			verts.draw(gl);
		}
	}
}
