package com.floern.rhabarber.physics;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES10;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Landscape;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXMatrix;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;

import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;
import com.floern.rhabarber.util.FXMath;

public class PhysicsController {
	
	private World world;
	//separate list of players for easier retrieval of players
	private ArrayList<Player> players;
	
	
	// tmp
	Vertexes outline;
	private float[] acceleration = new float[3];
	
	
	public PhysicsController(InputStream level) {
		
		// Use GameWorld here (eg. do not use a modified world, or implement a loading function to GameWorld)
		this.world = World.loadWorld(new PhysicsFileReader(level));// new GameWorld();
		
		//more than 4 players are probably not feasible anyway
		this.players = new ArrayList<Player>(4);
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
		int timestep = world.getTimestepFX();
		applyPlayerGravities(timestep);
		world.tick();
	}
	
	public void setAccel(float[] g) {
		this.acceleration = g;
	}
	
	private void applyPlayerGravities(int timestep)
	{
		//shared gravity is normal (global) gravity and players are just not affected by normal gravity
		FXVector sharedGravity = new FXVector(FXMath.floatToFX(acceleration[1]), FXMath.floatToFX(acceleration[0]));
		
		for (Player p : players) {
			p.applyAcceleration(p.playerGravity, timestep);
			sharedGravity.add(p.playerGravity);
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
			verts.draw(gl);
		}
	}
}
