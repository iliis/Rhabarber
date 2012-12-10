package com.floern.rhabarber.network2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

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
		inputStream = socket.getInputStream();
		
		// get client info
		targetIP = socket.getInetAddress().getHostAddress();
		targetPort = socket.getPort();
		
		// input listener/reader thread
		receiverThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						Message msg = read();
						Log.d("received message", msg.hexDump());
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
	 * Send Message: Init Game
	 * @param map to play on
	 */
	public void sendInitGameMessage(String gameMap) {
		Message initGameMessage = new Message(Message.TYPE_GAME_INIT, gameMap.getBytes());
		sendMessage(initGameMessage);
	}
	
	
	/**
	 * Send a Message
	 * @param msg
	 */
	private void sendMessage(Message msg) {
		Log.d("sendMessage()", msg.hexDump());
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
	
	
	/**
	 * Get port number out of the message (type TYPE_SERVERINFOBROADCAST)
	 * @param message Binary message data
	 * @return Server's port number, or 0 on error
	 */
	public static String[] parseUserListMessage(Message msg) {
		// wrong message type
		if (msg.type != Message.TYPE_USERLIST)
			return null;
		String userListString = new String(msg.payload);
		String[] userList = userListString.split(",");
		return userList;
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
			TYPE_GAME_INIT = ++i,
			TYPE_GAME_START = ++i,
			TYPE_SENSOR_PITCH = ++i,
			TYPE_PLAYERS_STATE = ++i,
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
