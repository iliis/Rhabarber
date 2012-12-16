package com.floern.rhabarber.network2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.IncomingMessageListener;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.Message;
import com.floern.rhabarber.network2.GameServerService.UserInfo;
import com.floern.rhabarber.util.IntRef;

import android.util.Log;

/** Thread that handles all the Client's neworking logic */
public class ClientNetworkingLogic {

	/** Server's IP */
	private final String serverIP;
	/** Server's Port */
	private final int serverPort;

	/** Socket of the register listener */
	public GameNetworkingProtocolConnection serverConnection;
	
	/** Callback for register events */
	private final GameRegisterEventListener registerEventListener;
	/** Callback for game updates events */
	private final GameUpdateEventListener updateEventListener;
	
	public volatile GameWorld game;
	

	/** Code that is executed to setup a connection to the server */
	private final Runnable connectionSetupRunnable = new Runnable() {
		public void run() {
			try {
				serverConnection = new GameNetworkingProtocolConnection(serverIP, serverPort);
				serverConnection.setIncomingMessageListener(messageListener);
				serverConnection.startReceiver();
			} catch (IOException e) {
				e.printStackTrace();
				serverConnection = null;
				registerEventListener.onNetworkingError(e.getMessage());
			}
		}
	};
	
	/** Listener for incoming Messages over a {@link GameNetworkingProtocolConnection} */
	private final IncomingMessageListener messageListener = new IncomingMessageListener() {
		public void onTimeout() {
			registerEventListener.onNetworkingError("Connection Timeout");
		}
		public void onReceive(Message message) {
			
			if (message.type == Message.TYPE_REGISTRATION_CONFIRM) {
				// confirm registration
				registerEventListener.onRegisterSuccess();
			}
			else if (message.type == Message.TYPE_USERLIST) {
				// update userlist
				String[] userListArray = GameNetworkingProtocolConnection.parseUserListMessage(message);
				ArrayList<UserInfo> userList = new ArrayList<UserInfo>(userListArray.length);
				for (String user : userListArray)
					userList.add(new UserInfo(user, 0));
				registerEventListener.onUserListChange(userList);
			}
			else if (message.type == Message.TYPE_IDLE) {
				// idle received, ignore
			}
			else if (message.type == Message.TYPE_GAME_START) {
				
				// init game
				IntRef playerIdx = new IntRef(-1);
				String map = GameNetworkingProtocolConnection.parseStartGameMessage(message, playerIdx);
				
				updateEventListener.onInitGame(map, playerIdx.value);
			}
			else if (message.type == Message.TYPE_SERVER_GAMESTATE) {
				GameNetworkingProtocolConnection.receiveServerState(message, game);
			}
			else if (message.type == Message.TYPE_INSERT_PLAYER) {
				GameNetworkingProtocolConnection.receiveInsertPlayerMessage(message, game);
			}
			else if (message.type == Message.TYPE_INSERT_TREASURE) {
				GameNetworkingProtocolConnection.receiveInsertTreasureMessage(message, game);
			} // TODO: add missing message types
			else if (message.type == Message.TYPE_GAME_END) {
				int winner = GameNetworkingProtocolConnection.parseEndGameMessage(message);
				game.stopGame(winner);
			}
			else {
				Log.i("onReceive()", "Mistimed/unknown Message received, Type: "+message.type+"; Hex: "+message.hexDump());
			}
		}
		public void onConnectionError(Exception e) {
			stopAvoidTimeout();
			registerEventListener.onNetworkingError(e.getMessage());
		}
		public void onConnectionClosed() {
			stopAvoidTimeout();
			registerEventListener.onNetworkingError("Connection closed");
		}
	};
	
	/** true to execute idle-message sender loop */
	private volatile boolean needsToAvoidTimeout = false;
	
	/** Loop code that sends idle-messages to avoid a timeout */
	private final Runnable timeoutAviodanceLooper = new Runnable() {
		public void run() {
			try {
				// send idle message every x seconds to avoid timeout
				int timeToWait = GameNetworkingProtocolConnection.IDLE_TIMEOUT * 3 / 4;
				while (needsToAvoidTimeout) {
					int timeElapsedSinceLastSend = (int)(System.currentTimeMillis() - serverConnection.lastSendActivity);
					// check for enough time elapsed
					if (timeElapsedSinceLastSend >= timeToWait) {
						serverConnection.sendIdleMessage();
						timeElapsedSinceLastSend = 0;
					}
					// sleep until enough time elapsed
					Thread.sleep(timeToWait - timeElapsedSinceLastSend);
				}
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
		}
	};
	
	
	
	/**
	 * Thread that handles all the Client's neworking logic
	 * @param serverIP Server's IP
	 * @param serverPort Server's Port
	 * @param registerEventListener Callback for register events
	 * @param updateEventListener Callback for game updates events
	 */
	public ClientNetworkingLogic(String serverIP, int serverPort, 
			GameRegisterEventListener registerEventListener, GameUpdateEventListener updateEventListener) {
		this.serverIP   = serverIP;
		this.serverPort = serverPort;
		this.registerEventListener = registerEventListener;
		this.updateEventListener   = updateEventListener;
		
		// setup connection to the server 
		connectionSetupRunnable.run();
	}
	
	
	public boolean connectionEstablished() {
		return (serverConnection != null);
	}
	
	
	/**
	 * Send a registration message to the server
	 */
	public void registerAtServer() {
		serverConnection.sendRegistrationRequest();
	}
	
	
	/**
	 * Send a idle message every x seconds to avoid a connection timeout. Should be called in a separate Thread
	 */
	public void startAvoidTimeout() {
		needsToAvoidTimeout = true;
		// start looper
		timeoutAviodanceLooper.run();
	}
	
	
	/**
	 * Stop sending anti-timeout-messages
	 */
	public void stopAvoidTimeout() {
		needsToAvoidTimeout = false;
	}
	
	
	/**
	 * Send an unregister message and close the connection
	 */
	public void unregisterAtServer() {
		serverConnection.sendUnregisterMessage();
		serverConnection.disconnect();
	}
	
	
	
	/**
	 * Callback for register events
	 */
	public static interface GameRegisterEventListener {
		/**
		 * User was registered
		 */
		void onRegisterSuccess();
		/**
		 * Userlist was updated
		 * @param userList List of users
		 */
		void onUserListChange(ArrayList<UserInfo> userList);
		/**
		 * An error happened while networking
		 * @param errorMessage Error message
		 */
		void onNetworkingError(String errorMessage);
	}

	
	/**
	 * Callback for game updates events
	 */
	public static interface GameUpdateEventListener {
		/**
		 * Game has to be initialized
		 * @param gameMap map to play on
		 * @param playerIdx ID of this client (assigned by server)
		 */
		void onInitGame(final String gameMap, final int playerIdx);
		/**
		 * User was registered
		 */
		void onPlayerDataUpdate();
	}
	
}
