package com.floern.rhabarber.network2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import at.emini.physics2D.Body;
import at.emini.physics2D.util.FXVector;

import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;
import com.floern.rhabarber.logic.elements.Treasure;
import com.floern.rhabarber.util.DynamicFloatBuffer;
import com.floern.rhabarber.util.IntRef;

import android.util.Log;

/**
 * Protocol to handle the Communication between the Game Server and a Client/User
 */
public class GameNetworkingProtocolConnection {
	
	/** Protocol Version */
	public static final byte VERSION = 1;
	
	/** Timeout in milliseconds until connection gets aborted/closed */
	public static final int IDLE_TIMEOUT = 8000;
	
	/** TCP Socket */
	private final Socket socket;
	/** Input Stream from Client */
	private final InputStream inputStream;
	/** Output Stream to Client */
	private final OutputStream outputStream;
	
	/** Thread to listen for incoming Messages on the Socket */
	private final Thread receiverThread;

	/** Client's IP Address */
	public final String targetIP;
	/** Client's Port */
	public final int targetPort;
	
	/** Callback for incoming messages */
	private IncomingMessageListener receiveCallback;
	
	/** Timestamp when the last message was sent */
	public long lastSendActivity = 0;
	
	/**
	 * Create a new GameNetworkingProtocol connection
	 * @param clientSocket TCP socket
	 * @throws IOException Error while processing the socket
	 */
	public GameNetworkingProtocolConnection(Socket clientSocket) throws IOException {
		// setup socket
		socket = clientSocket;
		socket.setSoTimeout(IDLE_TIMEOUT);
		socket.setTcpNoDelay(true);
		
		// setup streams
		outputStream = socket.getOutputStream();
		inputStream  = socket.getInputStream();
		
		// get client info
		targetIP   = socket.getInetAddress().getHostAddress();
		targetPort = socket.getPort();
		
		// input listener/reader thread
		receiverThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						Message msg = read();
						//Log.d("received message", msg.hexDump());
						receiveCallback.onReceive(msg);
					}
				} catch (IncompatibleProtocolVersionException e) {
					e.printStackTrace();
					disconnect();
					receiveCallback.onConnectionError(e);
				} catch (InterruptedIOException e) {
					e.printStackTrace();
					disconnect();
					receiveCallback.onTimeout();
				} catch (SocketException e) {
					//e.printStackTrace();
					disconnect();
					receiveCallback.onConnectionError(e);
				} catch (IOException e) {
					e.printStackTrace();
					disconnect();
					receiveCallback.onConnectionError(e);
				}
			}
		});
	}
	
	
	/**
	 * Create a new GameNetworkingProtocol connection
	 * @param serverIP Target IP
	 * @param serverPort Target port
	 * @throws IOException Error while setting up the socket
	 */
	public GameNetworkingProtocolConnection(String serverIP, int serverPort) throws IOException {
		this(new Socket(serverIP, serverPort));
	}
	
	
	/**
	 * Set a new listener for incoming messages
	 * @param listener Callback for incoming messages
	 */
	public void setIncomingMessageListener(IncomingMessageListener listener) {
		receiveCallback = listener;
	}
	
	
	/**
	 * Start the receiver
	 */
	public void startReceiver() {
		receiverThread.start();
	}
	
	
	/**
	 * Get a message by reading from the input stream
	 * @return
	 * @throws IOException
	 * @throws InterruptedIOException read timeout
	 * @throws IncompatibleProtocolVersionException wrong protcol version
	 */
	private Message read() throws IOException, InterruptedIOException, IncompatibleProtocolVersionException {
		// read protocol version
		byte version = readByteFromStream(inputStream);
		// check protocol version
		if (version != VERSION)
			throw new IncompatibleProtocolVersionException();
		// read message type
		byte msgType = readByteFromStream(inputStream);
		// read message length
		byte lengthHi = readByteFromStream(inputStream);
		byte lengthLo = readByteFromStream(inputStream);
		int payloadLength = (0xFF00 & ((int)lengthHi << 8)) + (0xFF & (int)lengthLo);
		// get all data
		byte[] payload = null;
		// read payload
		if (payloadLength > 0) {
			payload = new byte[payloadLength];
			readBytesFromStream(inputStream, payload);
		}
		// return message wrapper
		return new Message(msgType, payload);
	}
	
	
	/**
	 * InputStream.read() wrapper that throws an Exception when the connection is finished
	 * @param stream the InputStream to read from
	 * @return read byte
	 * @throws InterruptedIOException read timeout
	 * @throws IOException
	 */
	private byte readByteFromStream(InputStream stream) throws InterruptedIOException, IOException {
		int b = stream.read();
		if (b == -1)
			throw new SocketException("Socket is closed");
		else
			return (byte) b;
	}
	
	
	/**
	 * InputStream.read() wrapper that throws an Exception when the connection is finished
	 * @param stream the InputStream to read from
	 * @param result buffer for read bytes
	 * @throws InterruptedIOException read timeout
	 * @throws IOException
	 */
	private void readBytesFromStream(InputStream stream, byte[] result) throws InterruptedIOException, IOException {
		int bytesRead = 0;
		// force filling whole buffer
		while (bytesRead < result.length) {
			int len = stream.read(result, bytesRead, result.length-bytesRead);
			if (len == -1)
				throw new SocketException("Socket is closed");
			bytesRead += len;
		}
	}
	
	
	/**
	 * Send Message: Registration Request
	 */
	public void sendRegistrationRequest() {
		Message requestMessage = new Message(Message.TYPE_REGISTRATION_REQUEST, null);
		sendMessage(requestMessage);
	}
	
	
	/**
	 * Send Message: Registration Confirmation
	 */
	public void sendRegistrationConfirmation() {
		Message confirmationMessage = new Message(Message.TYPE_REGISTRATION_CONFIRM, null);
		sendMessage(confirmationMessage);
	}
	
	
	/**
	 * Send Message: Idle, avoid timeout
	 */
	public void sendIdleMessage() {
		Message confirmationMessage = new Message(Message.TYPE_IDLE, null);
		sendMessage(confirmationMessage);
	}
	
	
	/**
	 * Send Message: Unregister at server
	 */
	public void sendUnregisterMessage() {
		Message unregisterMessage = new Message(Message.TYPE_UNREGISTER, null);
		sendMessage(unregisterMessage);
	}
	
	
	/**
	 * Send Message: User List
	 * @param userlist List of all registered Users
	 */
	public void sendUserList(List<GameNetworkingProtocolConnection> userlist) {
		StringBuilder ipList = new StringBuilder(16 * userlist.size());
		for (GameNetworkingProtocolConnection user : userlist) {
			ipList.append(user.targetIP);
			ipList.append(',');
		}
		byte[] binaryUserList = ipList.toString().getBytes();
		Message userlistMessage = new Message(Message.TYPE_USERLIST, binaryUserList);
		sendMessage(userlistMessage);
	}
	
	/**
	 * Send Message: Acceleration data from client
	 * @param playerIdx Index of player sending the data
	 * @param accel Raw data from Sensor
	 */
	public void sendAccelerationData(int playerIdx, ClientStateAccumulator.Acceleration accel) {
		byte[] arr = new byte[4*4]; // 3 floats and one int, each 4 bytes
		ByteBuffer buf = ByteBuffer.wrap(arr);
		buf.putInt(playerIdx);
		buf.putFloat(accel.x);
		buf.putFloat(accel.y);
		buf.putFloat(accel.z);
		
		Message accelMessage = new Message(Message.TYPE_CLIENT_ACCELERATION, arr);
		sendMessage(accelMessage);
	}
	
	/**
	 * Send Message: Input data from client (for moving the player around)
	 * @param input walking direction
	 */
	public void sendUserInputData(int playerIdx, ClientStateAccumulator.UserInputWalk input) {
		byte[] arr = new byte[4+1]; // one int with 4 bytes, one enum
		ByteBuffer buf = ByteBuffer.wrap(arr);
		buf.putInt(playerIdx);
		
		switch(input) {
		case LEFT:
			buf.put((byte) 0x00); break;
		case RIGHT:
			buf.put((byte) 0x01); break;
		case NONE:
			buf.put((byte) 0x02); break;
		default:
			Log.e("sendUserInputData()", "received invalid UserInputData ("+input+").");
			return;
		}
		
		sendMessage(new Message(Message.TYPE_CLIENT_INPUT, arr));
	}
	
	/**
	 * Send the current state of the world to a client. This includes the following:
	 * - ID, positionFX, RotationFX of all Bodies (inclusive Players and Treasures)
	 * - ID, score, velocity (for animation) of all Players
	 * @param bodies
	 * @param players
	 */
	public void sendServerState(List<Body> bodies, List<Player> players, List<Treasure> treasures) {
		byte[] arr = new byte[3*4	// number of bodies, number of players, number of treasures
							+ 4*4*(bodies.size())		// for each body: id (int), position (2 ints FX), rotation (1 int 2FX)
							+ 4*3*(players.size())		// for each player: id (int), score (int), velocity (float)
							+ 2*4*(treasures.size())	//  for each treasure: id (int), value (int) (rest is contained in bodies)
							];
		ByteBuffer buf = ByteBuffer.wrap(arr);
		buf.putInt(bodies.size());
		buf.putInt(players.size());
		buf.putInt(treasures.size());
		
		for(Body b: bodies) {
				buf.putInt(b.getId());
				buf.putInt(b.positionFX().xFX);
				buf.putInt(b.positionFX().yFX);
				buf.putInt(b.rotation2FX());
		}
		
		for(Player p: players) {
			buf.putInt(p.getIdx());
			buf.putInt(p.score);
			buf.putFloat(p.getAlignedSpeed());
		}
		
		for(Treasure t: treasures) {
			buf.putInt(t.getId());
			buf.putInt(t.getValue());
		}
		
		sendMessage(new Message(Message.TYPE_SERVER_GAMESTATE, arr));
	}
	
	/**
	 * Send word to clients that there is a new player on the field.
	 * @param p the chosen one
	 */
	public void sendInsertPlayer(Player p) {
		
		byte[] arr = new byte[ 4	// ID
		                      +2*4	// Position
		                      +4	// Color
		                      +4	// Winning Score
		                      ];
		ByteBuffer buf = ByteBuffer.wrap(arr);
		
		buf.putInt(p.getIdx());
		buf.putInt(p.positionFX().xFX);
		buf.putInt(p.positionFX().yFX);
		buf.putInt(p.color);
		buf.putInt(p.WINNING_SCORE);
		
		sendMessage(new Message(Message.TYPE_INSERT_PLAYER, arr));
	}
	
	/**
	 * Notify clients that there is a new treasure with this message.
	 * @param t
	 */
	public void sendInsertTreasure(Treasure t) {
		byte[] arr = new byte[    4 // ID
		                       +2*4 // Position
		                       +4	// Value
		                       ];
		ByteBuffer buf = ByteBuffer.wrap(arr);
		
		buf.putInt(t.getId());
		buf.putInt(t.positionFX().xFX);
		buf.putInt(t.positionFX().yFX);
		buf.putInt(t.getValue());
		
		sendMessage(new Message(Message.TYPE_INSERT_TREASURE, arr));
	}
	
	/**
	 * Starts a new game and tells the client which data to load
	 * @param playerIdx ID the client gets assigned
	 * @param map Level to load (which contains everything necessary to initialize a shared world)
	 */
	public void sendStartGameMessage(int playerIdx, String map) {
		byte[] stringdata = map.getBytes();
		byte[] arr = new byte[4+4+stringdata.length]; // index, string length, string
		ByteBuffer buf = ByteBuffer.wrap(arr);
		
		buf.putInt(playerIdx);
		buf.putInt(map.length());
		buf.put(stringdata);
		
		sendMessage(new Message(Message.TYPE_GAME_START, arr));
	}
	
	/**
	 * Ends a game and tells the clients who has won.
	 * @param winnerIdx Id of winning player (-1 if aborted)
	 */
	public void sendEndGameMessage(int winnerIdx) {
		byte[] arr = new byte[4];
		ByteBuffer buf = ByteBuffer.wrap(arr);
		buf.putInt(winnerIdx);
		sendMessage(new Message(Message.TYPE_GAME_END, arr));
	}
	
	
	/**
	 * Send a Message
	 * @param msg
	 */
	private void sendMessage(Message msg) {
		//Log.d("sendMessage()", msg.hexDump());
		try {
			outputStream.write(msg.getBytes());
			outputStream.flush();
			lastSendActivity = System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
			receiveCallback.onConnectionError(e);
		}
	}
	
	
	/**
	 * Disconnect the client
	 */
	public void disconnect() {
		try {
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Compare two Clients
	 * @param o Object to compare to
	 * @return true, if IP Address and Port are equal
	 */
	public boolean equals(Object o) {
		if (!(o instanceof GameNetworkingProtocolConnection))
			return false;
		GameNetworkingProtocolConnection othr = (GameNetworkingProtocolConnection) o;
		if (this.targetPort != othr.targetPort || !this.targetIP.equals(othr.targetIP))
			return false;
		return true;
	}
	
	
	/**
	 * Get message for server info broadcasting
	 * @param serverPort server's preferred port
	 * @return message
	 */
	public static Message getServerInfoBroadcastMessage(int serverPort) {
		byte[] payload = new byte[]{ (byte)((serverPort >> 8) & 0xFF), (byte)(serverPort & 0xFF) };
		return new Message(Message.TYPE_SERVERINFOBROADCAST, payload);
	}
	
	
	/**
	 * Get port number out of the message (type TYPE_SERVERINFOBROADCAST)
	 * @param message Binary message data
	 * @return Server's port number, or 0 on error
	 */
	public static int parseServerInfoBroadcastMessage(byte[] message) {
		Message msg = new Message(message);
		// wrong message type
		if (msg.type != Message.TYPE_SERVERINFOBROADCAST)
			return 0;
		byte[] portBinary = msg.payload;
		int port = ((0xFF & portBinary[0]) << 8) | (0xFF & portBinary[1]);
		// invalid port
		if (port < 0 || 0xFFFF < port)
			return 0;
		return port;
	}
	
	
	
	public static String[] parseUserListMessage(Message msg) {
		// wrong message type
		if (msg.type != Message.TYPE_USERLIST)
			return null;
		String userListString = new String(msg.payload);
		String[] userList = userListString.split(",");
		return userList;
	}
	
	
	public static ClientStateAccumulator.Acceleration parseAccelerationMessage(Message msg, IntRef playerIdxOut) {
		// wrong message type?
		if (msg.type != Message.TYPE_CLIENT_ACCELERATION)
			return null;
		
		ByteBuffer b = ByteBuffer.wrap(msg.payload);
		playerIdxOut.value = b.getInt();
		
		ClientStateAccumulator.Acceleration a = new ClientStateAccumulator.Acceleration();
		
		a.x = b.getFloat();
		a.y = b.getFloat();
		a.z = b.getFloat();
		
		return a;
	}
	
	public static ClientStateAccumulator.UserInputWalk parseUserInputMessage(Message msg, IntRef playerIdxOut) {
		IntBuffer ibuf = ByteBuffer.wrap(msg.payload).asIntBuffer();
		playerIdxOut.value = ibuf.get();
		
		switch(msg.payload[4]) {
		case 0x00:
			return ClientStateAccumulator.UserInputWalk.LEFT;
		case 0x01:
			return ClientStateAccumulator.UserInputWalk.RIGHT;
		case 0x02:
			return ClientStateAccumulator.UserInputWalk.NONE;
		default:
			Log.e("parseUserInputMessage()", "Received invalid UserInputWalk ("+msg.payload[4]+")");
			return ClientStateAccumulator.UserInputWalk.NONE;
		}
	}
	
	/**
	 * Updates the state of client's world to the one from the server
	 * @param m Message from server.
	 * @param w GameWorld of client, will be updated with data from message
	 */
	public static void receiveServerState(Message m, GameWorld w) {
		// number of bodies, number of players, number of treasures
		// for each body: id (int), position (2 ints FX), rotation (1 int 2FX)
		// for each player: id (int), score (int), velocity (float)
		// for each treasure: id (int), value (int)
				
		ByteBuffer buf = ByteBuffer.wrap(m.payload);
		int body_count   = buf.getInt();
		int player_count = buf.getInt();
		int treas_count  = buf.getInt();
		
		for(; body_count > 0; --body_count) {
			int id = buf.getInt();
			int x  = buf.getInt(),
				y  = buf.getInt(),
				a  = buf.getInt();
			
			Body b = w.getBodyByID(id);
			if (b != null) { // TODO: remove this check and fix underlying issue (ids not identical?)
			b.setPositionFX(new FXVector(x, y));
			b.setRotation2FX(a);} else {
				Log.e("receiveServerState()", "got a Body with unknown ID ("+id+").");
			}
		}
		
		for(; player_count > 0; --player_count) {
			int id    = buf.getInt(); if (id < 0) Log.e("foo", "player id = "+id+" in receiveServerState()");
			int score = buf.getInt();
			float speed = buf.getFloat();
			
			Player p = w.getPlayers().get(id); // assuming player IDs correspond to position in array (should be the case accoring to GameWorld.AddPlayer())
			p.score = score;
			p.setAlignedSpeed(speed);
		}
		
		for(; treas_count > 0; --treas_count) {
			int id    = buf.getInt();
			int value = buf.getInt();
			
			Body t = w.getBodyByID(id);
			if(t instanceof Treasure) {
				((Treasure) t).setValue(value);
			} else {
				Log.e("receiveServerState()", "ServerState contains Treasure with invalid ID ("+id+")");
			}
		}
	}
	
	/**
	 * adds a new Player to the world w
	 * @param m
	 * @param w
	 */
	public static void receiveInsertPlayerMessage(Message m, GameWorld w) {
		ByteBuffer buf = ByteBuffer.wrap(m.payload);
		
		int idx    = buf.getInt();
		int posx   = buf.getInt();
		int posy   = buf.getInt();
		int color  = buf.getInt();
		int wscore = buf.getInt();
		
		w.addPlayer(new FXVector(posx, posy), idx, color, wscore);
	}
	
	public static void receiveInsertTreasureMessage(Message m, GameWorld w) {
		ByteBuffer buf = ByteBuffer.wrap(m.payload);
		
		//int id     = buf.getInt();// not used, the physics engine creates a new one itself.
									// you may however check if the new Treasure got the correct ID
		int posx   = buf.getInt();
		int posy   = buf.getInt();
		int value  = buf.getInt();
		
		w.addTreasure(new Treasure(posx, posy, value));
	}
	
	/**
	 * Start a new game.
	 * @param msg The message from the server.
	 * @param playerIdxOut The index this client got assigned from the server.
	 * @return The filename of the map to load
	 */
	public static String parseStartGameMessage(Message msg, IntRef playerIdxOut) {
		ByteBuffer buf   = ByteBuffer.wrap(msg.payload);
		
		playerIdxOut.value = buf.getInt();
		int stringsize     = buf.getInt();
		
		byte[] strbuf = new byte[stringsize];
		buf.get(strbuf);
		String s = new String(strbuf);
		
		return s;
	}

	/**
	 * When game is finished
	 * @param m The message from the server.
	 * @return PlayerID of winner. -1 for canceled game.
	 */
	public static int parseEndGameMessage(Message m) {
		ByteBuffer buf = ByteBuffer.wrap(m.payload);
		return buf.getInt();
	}
	
	
	/**
	 * Wrapper for a {@link ServerSocket} to accept clients for {@link GameNetworkingProtocolConnection}
	 */
	public static class ClientAcceptor {
		private final ServerSocket clientListenerSocket;
		
		/**
		 * Init a Client Listener
		 * @param serverPort Port to listen to
		 * @throws IOException
		 */
		public ClientAcceptor(int serverPort) throws IOException {
			clientListenerSocket = new ServerSocket(serverPort);
		}

		/**
		 * Wait for a new client (blocking)
		 * @return Accepted client
		 * @throws SocketException when abort()'ed
		 * @throws IOException other Socket error
		 */
		public GameNetworkingProtocolConnection accept() throws SocketException, IOException {
			Socket clientSocket = clientListenerSocket.accept();
			GameNetworkingProtocolConnection user = new GameNetworkingProtocolConnection(clientSocket);
			return user;
		}
		
		/**
		 * Stop accepting clients. Close ServerSocket; when it's already closed then nothing happens
		 */
		public void close() {
			if (isClosed())
				return;
			try {
				clientListenerSocket.close();
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
		
		/**
		 * Returns whether this server socket is closed or not.
		 * @return true if this listener socket is closed, false otherwise.
		 */
		public boolean isClosed() {
			return clientListenerSocket.isClosed();
		}
	}
	
	
	
	/**
	 * Listener with Callback Methods for incoming Messages
	 */
	public static interface IncomingMessageListener {
		/**
		 * Receive message
		 * @param message
		 */
		void onReceive(Message message);
		/**
		 * Connection timed out
		 */
		void onTimeout();
		/**
		 * Client closed the connection
		 */
		void onConnectionClosed();
		/**
		 * An Error occurred
		 */
		void onConnectionError(Exception e);
	}
	
	

	/**
	 * Message data wrapper
	 */
	public static class Message {
		
		/** Size of the Message header in bytes */
		private static final int HEADER_SIZE = 4;

		private static byte i = 0;
		/** Message Type */
		public static final byte
			TYPE_SERVERINFOBROADCAST = ++i, 
			TYPE_REGISTRATION_REQUEST = ++i,
			TYPE_REGISTRATION_CONFIRM = ++i,
			TYPE_UNREGISTER = ++i,
			TYPE_IDLE = ++i,
			TYPE_USERLIST = ++i,
			
			// client --> server
			TYPE_CLIENT_ACCELERATION = ++i,
			TYPE_CLIENT_INPUT = ++i,
			
			// server --> client
			TYPE_GAME_START = ++i,
			TYPE_SERVER_GAMESTATE = ++i,
			TYPE_INSERT_PLAYER = ++i,
			TYPE_INSERT_TREASURE = ++i,
			TYPE_GAME_END = ++i
			;
		
		/** Empty payload byte array */
		private final static byte[] EMPTY_DATA = new byte[0];
		
		/** Protocol Version */
		public final byte version;
		/** Message Type, see Message.TYPE_* fields */
		public final byte type;
		/** Binary Message Header */
		public final byte[] header;
		/** Binary Message Data/Payload */
		public final byte[] payload;

		/**
		 * Message data wrapper
		 * @param data binary message data
		 */
		private Message(byte[] data) {
			version = data[0];
			type = data[1];
			header = new byte[HEADER_SIZE];
			System.arraycopy(data, 0, header, 0, HEADER_SIZE);
			int payloadLength = (0xFF00 & ((int)data[2] << 8)) + (0xFF & (int)data[3]);
			payload = new byte[payloadLength];
			System.arraycopy(data, HEADER_SIZE, payload, 0, payloadLength);
		}
		
		/**
		 * Message data wrapper
		 * @param type message type
		 * @param payload payload data
		 */
		private Message(byte type, byte[] payload) {
			this.version = VERSION;
			this.type = type;
			if (payload == null)
				payload = EMPTY_DATA;
			this.header = new byte[]{VERSION, type, (byte)((payload.length >> 8) & 0xFF), (byte)(payload.length & 0xFF)};
			this.payload = payload;
		}
		
		/**
		 * Get binary message data for packet
		 * @return binary message data (including header)
		 */
		public byte[] getBytes() {
			byte[] data = new byte[HEADER_SIZE + payload.length];
			System.arraycopy(header, 0, data, 0, HEADER_SIZE);
			System.arraycopy(payload, 0, data, HEADER_SIZE, payload.length);
			return data;
		}
		
		
		/**
		 * Get message data as a hexadecimal string
		 * @return hex string with whitespace-separated bytes
		 */
		public String hexDump() {
			StringBuilder sb = new StringBuilder((HEADER_SIZE + payload.length) * 3);
			sb.append(String.format("%02x", header[0] & 0xff));
			for (int i=1; i<HEADER_SIZE; ++i)
				sb.append(String.format(" %02x", header[i] & 0xff));
			for (int i=0; i<payload.length; ++i)
				sb.append(String.format(" %02x", payload[i] & 0xff));
			return sb.toString();
		}
		
	}
	
	
	/**
	 * Protocol Version is not the same
	 */
	public static class IncompatibleProtocolVersionException extends IOException {
		private static final long serialVersionUID = 5260180904394646613L;
	}
	
	
}
