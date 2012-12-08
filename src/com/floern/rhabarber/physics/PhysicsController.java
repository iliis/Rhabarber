package com.floern.rhabarber.physics;

import java.io.InputStream;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES10;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.Event;
import at.emini.physics2D.Landscape;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXMatrix;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;

import com.floern.rhabarber.graphic.primitives.IGLPrimitive;
import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;
import com.floern.rhabarber.logic.elements.Treasure;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.Vector;

public class PhysicsController {
	
	private GameWorld world;
	
	// tmp
	Vertexes outline;
	private float[] acceleration = new float[3];
	
	long last_tick;
	
	public float min_x, max_x, min_y, max_y; // size of landscape
	
	
	public PhysicsController(InputStream level, Player p) {

		loadLevel(level);
		world.addPlayer(p);
		
		// test treasure
		world.addTreasure(new Treasure(300, 300, 42), new PhysicsEventListener() {
			public void eventTriggered(Event arg0, Object arg1) { }
		});

		outline = new Vertexes();
		outline.setMode(GLES10.GL_LINES); // disconnected bunch of lines
		outline.setThickness(3);
		
		
		for(int i = 0; i < world.getLandscape().segmentCount(); ++i) {
			FXVector A = world.getLandscape().startPoint(i);
			FXVector B = world.getLandscape().endPoint(i);
			
			outline.addPoint(A.xAsFloat(), A.yAsFloat());
			outline.addPoint(B.xAsFloat(), B.yAsFloat());
			
			min_x = Math.min(Math.min(A.xAsFloat(), B.xAsFloat()), min_x); max_x = Math.max(Math.max(A.xAsFloat(), B.xAsFloat()), max_x);
			min_y = Math.min(Math.min(A.yAsFloat(), B.yAsFloat()), min_y); max_y = Math.max(Math.max(A.yAsFloat(), B.yAsFloat()), max_y);
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

			
			if (b[i] instanceof IGLPrimitive) {
				// draw element
				((IGLPrimitive)b[i]).draw(gl);
			}
			//else {
				// draw shape
				FXVector[] vertsFX = b[i].shape().getCorners();
				FXVector pos = b[i].positionFX();
				Vertexes verts = new Vertexes(); verts.setMode(GLES10.GL_LINE_LOOP);
				FXMatrix rot = b[i].getRotationMatrix();
				for (FXVector v: vertsFX) {
					FXVector vr = rot.mult(v);
					verts.addPoint(vr.xAsFloat()+pos.xAsFloat(), vr.yAsFloat()+pos.yAsFloat());
				}
				gl.glColor4f(1, 1, 1, 1);
				verts.draw(gl);
			//}
			
			
			
			/* // old draw code
			if (b[i] instanceof Player) {
				gl.glColor4f(1, 0.2f, 0, 1);
				
				Player p = (Player) b[i];
				p.skeleton.position = new Vector(p.positionFX().xAsFloat(), p.positionFX().yAsFloat()-6);
				p.skeleton.rotation = FXMath.FX2toFloat(p.rotation2FX());
				p.skeleton.draw(gl);
				
				gl.glColor4f(1, 0.2f, 0, 0.6f);
				
			} else {
				gl.glColor4f(1, 1, 1, 1);
			}
			verts.draw(gl);
			*/
		}
	}
	
	public GameWorld getWorld()
	{
		return world;
	}
}
