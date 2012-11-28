package com.floern.rhabarber.physics;

import java.io.File;
import java.util.ArrayList;
import at.emini.physics2D.Landscape;
import at.emini.physics2D.util.FXVector;
import at.emini.physics2D.util.PhysicsFileReader;

import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;

public class PhysicsController {
	
	private GameWorld world;
	//separate list of players for easier retrieval of players
	private ArrayList<Player> players;
	
	public PhysicsController(String levelname) {
		this.world = new GameWorld();
		//more than 4 players are probably not feasible anyway
		this.players = new ArrayList<Player>(4);
		loadLevel(levelname);
	}
	
	
	//TODO adapt to android file handling
	private void loadLevel(String levelname)
	{
		File levelFile = new File(levelname);
		PhysicsFileReader reader = new PhysicsFileReader(levelFile);
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
	
	private void applyPlayerGravities(int timestep)
	{
		//shared gravity is normal (global) gravity and players are just not affected by normal gravity
		FXVector sharedGravity = new FXVector(0,0);
		
		for (Player p : players) {
			p.applyAcceleration(p.playerGravity, timestep);
			sharedGravity.add(p.playerGravity);
		}
		
		//sharedGravity.normalize()?
		world.setGravity(sharedGravity);
	}
	
	public void draw()
	{
		/*
		 * TODO: Flo, insert openGL magic here
		 * world.getBodies(), world.getLandscape() might come in handy
		 * 
		 * Also beware of fixed point vectors (FXVector) -> see emini physics documentation
		 */
	}
}
