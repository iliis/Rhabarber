package com.floern.rhabarber.network2;

import java.util.ArrayList;

import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.Message;

public class ClientStateAccumulator {
	
	enum UserInputWalk {
		LEFT,
		RIGHT,
		NONE;
	}
	
	private class Acceleration {
		public float x,y,z;
		
		public Acceleration copy() {
			Acceleration a = new Acceleration();
			
			a.x = this.x;
			a.y = this.y;
			a.z = this.y;
			
			return a;
		}
	}
	
	public ArrayList<UserInputWalk> inputs = new ArrayList<UserInputWalk>();
	public ArrayList<Acceleration>  accels = new ArrayList<Acceleration>();
	
	public void update(Message m) {
		// TODO
	}
	
	// creates a deep copy of this
	public ClientStateAccumulator copy() {
		ClientStateAccumulator c = new ClientStateAccumulator();
		
		for(UserInputWalk w: inputs)
			c.inputs.add(w);
		
		for(Acceleration a: accels)
			c.accels.add(a.copy());
		
		return c;
	}
}
