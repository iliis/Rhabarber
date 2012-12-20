package com.floern.rhabarber.network2;

import android.annotation.SuppressLint;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.Message;
import com.floern.rhabarber.util.IntRef;

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
		
		public Acceleration(float[] v) {
			x = v[0];
			y = v[1];
			z = v[2];
		}
		
		public Acceleration(){};
		
		public Acceleration copy() {
			Acceleration a = new Acceleration();
			
			a.x = this.x;
			a.y = this.y;
			a.z = this.z;
			
			return a;
		}
	}
	
	
	public UserInputWalk[] inputs = null;
	public Acceleration[]  accels = null;
	public boolean[]       ready  = null;
	
	public void allocate(int size) {
		inputs = new UserInputWalk[size];
		accels = new Acceleration [size];
		ready  = new boolean[size];
	}
	
	/**
	 * Receives a message and stores it in accumulator for later retrieval
	 * @param m the message
	 */
	@SuppressLint("UseValueOf")
	public void update(Message m) {
		
		if (m.type == Message.TYPE_CLIENT_ACCELERATION) {
			
			IntRef playerIdx = new IntRef(-1);
			Acceleration a = GameNetworkingProtocolConnection.parseAccelerationMessage(m, playerIdx);
			
			this.accels[playerIdx.value] =  a;
			
		} else if (m.type == Message.TYPE_CLIENT_INPUT) {
			
			IntRef playerIdx = new IntRef(-1);
			UserInputWalk a = GameNetworkingProtocolConnection.parseUserInputMessage(m, playerIdx);
			this.inputs[playerIdx.value] = a;
			
		} else if (m.type == Message.TYPE_CLIENT_READY) {
			int playerIdx = GameNetworkingProtocolConnection.parseClientReadyMessage(m);
			this.ready[playerIdx] = true;
		}
	}
	
	/**
	 * creates a deep copy of this
	 * @return deep copy
	 */
	public ClientStateAccumulator copy() {
		ClientStateAccumulator c = new ClientStateAccumulator();
		c.allocate(inputs.length);
		
		System.arraycopy(this.inputs, 0, c.inputs, 0, this.inputs.length);
		System.arraycopy(this.accels, 0, c.accels, 0, this.accels.length);
		System.arraycopy(this.ready,  0, c.ready,  0, this.ready .length);
		
		return c;
	}
	
	public boolean all_ready() {
		for (boolean b: ready) 
			if (b) return true;
		
		return false;
	}
}
