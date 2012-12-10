package com.floern.rhabarber.network2;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.Message;

/**
 * This class accumulates data from clients during a tick.
 * After the server has finished an iteration of the gameloop it collects the accumulated state from here.
 * @author Samuel
 *
 */
public class ClientStateAccumulator {
	
	public static enum UserInputWalk {
		LEFT,
		RIGHT,
		NONE;
	}
	
	public static class Acceleration {
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
	
	/**
	 * Receives a message and stores it in accumulator for later retrieval
	 * @param m the message
	 */
	@SuppressLint("UseValueOf")
	public void update(Message m) {
		
		if (m.type == Message.TYPE_CLIENT_ACCELERATION) {
			
			Integer playerIdx = new Integer(-1);
			Acceleration a = GameNetworkingProtocolConnection.parseAccelerationMessage(m, playerIdx);
			this.accels.set(playerIdx, a);
			
		} else if (m.type == Message.TYPE_CLIENT_INPUT) {
			
			Integer playerIdx = new Integer(-1);
			UserInputWalk a = GameNetworkingProtocolConnection.parseUserInputMessage(m, playerIdx);
			this.inputs.set(playerIdx, a);
			
		}
	}
	
	/**
	 * creates a deep copy of this
	 * @return deep copy
	 */
	public ClientStateAccumulator copy() {
		ClientStateAccumulator c = new ClientStateAccumulator();
		
		for(UserInputWalk w: inputs)
			c.inputs.add(w);
		
		for(Acceleration a: accels)
			c.accels.add(a.copy());
		
		return c;
	}
}
