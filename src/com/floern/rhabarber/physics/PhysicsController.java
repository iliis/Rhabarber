package com.floern.rhabarber.physics;

import java.io.InputStream;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES10;
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
import com.floern.rhabarber.util.Vector;

public class PhysicsController {
	
	private GameWorld world;
	
	// tmp
	Vertexes outline;
	private float[] acceleration = new float[3];
	
	long last_tick;
	
	
	public PhysicsController(InputStream level, Player p) {

		loadLevel(level);
		world.addPlayer(p);

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
	
	
	private void loadLevel(InputStream level)
	{
		World tmp = World.loadWorld(new PhysicsFileReader(level));
		this.world = new GameWorld();
		world.setLandscape(tmp.getLandscape());
	}
	
	//calculates next state of world
	public void tick()
	{
		// what about overflows? (so far I hadn't any bugs)
		long dt = System.nanoTime() - last_tick;
		last_tick = System.nanoTime();
		
		int timestep = world.getTimestepFX();
		world.applyPlayerGravities(timestep, acceleration);
		world.tick();
		
		for(Player p: world.getPlayers()) {
			p.animate(((float) dt) / 1000000000);
		}
	}
	
	public void setAccel(float[] g) {
		this.acceleration = g;
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
