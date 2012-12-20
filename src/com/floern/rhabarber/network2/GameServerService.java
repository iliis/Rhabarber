package com.floern.rhabarber.network2;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;
import com.floern.rhabarber.logic.elements.Treasure;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.*;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class GameServerService extends Service {
	
	/** Port for receiving multicast messages from game server */
	public final static int MULTICAST_RECEIVE_PORT = 5733;
	/** Group (Multicast IP) */
	public final static String MULTICAST_GROUP = "228.0.57.33";
	/** Multicast advertiser */
	private ServerAdvertisingMulticaster multicaster;
	
	/** Server's Port (default value) */
	public final static int SERVER_PORT = 5734;
	
	/** Server's Port to listen for users */;
	private int serverPort;
	
	/** Thread of the register listener */
	private Thread clientListenerThread;
	/** Socket wrapper for the register listener */
	private GameNetworkingProtocolConnection.ClientAcceptor gnpClientAcceptor;
	
	/** Callback for user register events */
	private UserListEventListener userRegisteredListener;
	
	/** List of registered clients */
	private ArrayList<GameNetworkingProtocolConnection> clientConnections = new ArrayList<GameNetworkingProtocolConnection>();
	
	/** data received from all clients */
	public final ClientStateAccumulator client_states = new ClientStateAccumulator();
	
	/** Flag, true if server is running */
	private volatile boolean serverIsRunning = false;
	
	/** Tag name for passing extra via Intent */
	public static final String EXTRAS_SERVER_PORT = "serverPort";

	/** Binder interface to this Service */
	private final GameServerBinder mBinder = new GameServerBinder();
	
	/** The actual simulation, this is the root instance of the game.
	 *  Every client (including the local one) just copies the state of this world.
	 */
	private GameWorld game;
	
	/** keep simulation speed slower or equal to this
	 * (time in nanoseconds a frame of the simulation has to take)
	 */
	private final long MIN_FRAMETIME = 10*1000*1000;
	
	/**
	 * Do not delay a single frame for less time than this (if < MIN_FRAMETIME)
	 * This is to prevent unnecessary short delays;
	 * (time in nanoseconds)
	 */
	private final long FRAMETIME_COARSENESS = 10*1000;
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// get Server Port
		// TODO: this sometimes throws a NullPointerException... seems like some sort of race condition
		Log.d("foo", "onStartCommand(): intent = "+intent);
		serverPort = intent.getIntExtra(EXTRAS_SERVER_PORT, 0);
		
		if (serverPort <= 0 || serverPort > 0xFFFF) {
			stopSelf();
			serverIsRunning = false;
			throw new IllegalArgumentException("Invalid Server Port specified: " + serverPort);
		}
		
		startupServer();
		
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	
	
	/**
	 * Startup Game Server
	 */
	private void startupServer() {
		if (serverIsRunning) {
			Log.i(getClass().getSimpleName()+".startupServer()", "Server already running");
			return;
		}
		
		serverIsRunning = true;
		
		setUpClientListener();
		
		// setup multicaster
		multicaster = new ServerAdvertisingMulticaster(serverPort, 
				GameServerService.MULTICAST_GROUP, GameServerService.MULTICAST_RECEIVE_PORT);
		multicaster.start();
	}
	
	
	/**
	 * Setup Thread and Socket to listen for Users to join the game.
	 */
	private void setUpClientListener() {
		// close socket if open
		if (gnpClientAcceptor != null) {
			gnpClientAcceptor.close();
		}
		// interrupt thread if running
		if (clientListenerThread != null && !clientListenerThread.isInterrupted()) {
			clientListenerThread.interrupt();
		}
		// setup new thread
		clientListenerThread = new Thread(new Runnable() {
			public void run() {
				setUpClientListenerSocket();
			}
		});
		clientListenerThread.start();
	}
	

	/**
	 * Setup Socket to listen for Users to join the game.
	 */
	private void setUpClientListenerSocket() {
		try {
			gnpClientAcceptor = new GameNetworkingProtocolConnection.ClientAcceptor(serverPort);
			while (serverIsRunning) {
				// wait for new client
				final GameNetworkingProtocolConnection newUser = gnpClientAcceptor.accept();
				// set message listener
				newUser.setIncomingMessageListener(new IncomingMessageListener() {
					public void onTimeout() {
						handleRemovedUser(newUser);
					}
					public void onReceive(Message message) {
						if (message.type == Message.TYPE_REGISTRATION_REQUEST) {
							Log.i("onReceive()", "Registration request received");
							handleClientRegistration(newUser);
						}
						else if (message.type == Message.TYPE_IDLE) {
							Log.i("onReceive()", "Idle received");
							// send idle response
							newUser.sendIdleMessage();
						}
						else if (message.type == Message.TYPE_UNREGISTER) {
							Log.i("onReceive()", "Unregister message received");
							// disconnect
							newUser.disconnect();
							// remove user out of the game
							handleRemovedUser(newUser);
						}
						else if (  message.type == Message.TYPE_CLIENT_ACCELERATION
								|| message.type == Message.TYPE_CLIENT_INPUT
								|| message.type == Message.TYPE_CLIENT_READY) {
							// update state of client with input received from him
							client_states.update(message);
						}
						else if (message.type == Message.TYPE_GAME_END) {
							int winner = GameNetworkingProtocolConnection.parseEndGameMessage(message);
							game.stopGame(winner);
						}
						else {
							Log.i("onReceive()", "Mistimed/unknown Message received, Type: "+message.type+" Hex: "+message.hexDump());
						}
					}
					public void onConnectionClosed() {
						handleRemovedUser(newUser);
					}
					public void onConnectionError(Exception e) {
						Log.w("IncomingMessageListener", "onConnectionError(): " + e.getMessage());
					}
				});
				newUser.startReceiver();
			}
		} catch (SocketException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Register a Client
	 * @param newClient The client to register
	 */
	private void handleClientRegistration(GameNetworkingProtocolConnection newClient) {
		if (game != null) {
			// game already started
			return;
		}
		
		// ckeck if user already registered
		for (GameNetworkingProtocolConnection client : clientConnections) {
			if (client.equals(newClient)) {
				// user already registered
				Log.w("setUpClientListenerSocket()", "A user on "+newClient.targetIP+":"+newClient.targetPort+" is already registered: kick old");
				// kick the old client
				client.disconnect();
				clientConnections.remove(client);
			}
		}
		// add client to list
		clientConnections.add(newClient);

		// send confirmation
		newClient.sendRegistrationConfirmation();
		
		// post event
		ArrayList<UserInfo> currentUsers = new ArrayList<UserInfo>(clientConnections.size());
		for (GameNetworkingProtocolConnection client : clientConnections) {
			currentUsers.add(new UserInfo(client.targetIP, client.targetPort));
		}
		if (userRegisteredListener != null)
			userRegisteredListener.onUserListChanged(currentUsers);
		
		// send user list to clients
		for (GameNetworkingProtocolConnection client : clientConnections) {
			client.sendUserList(clientConnections);
		}
	}
	
	
	
	/**
	 * Start the Game
	 * @param map to play on
	 */
	private void initGame(String gameMap) {
		
		try {
			game = new GameWorld(this.getAssets().open("level/"+gameMap), this.getResources(), true, -1);
			
		} catch (IOException e) {
			Toast.makeText(this, "Cannot load level '"+gameMap+"'", Toast.LENGTH_LONG).show();
			e.printStackTrace();
			return;
		}
		
		client_states.allocate(clientConnections.size());
		
		for (@SuppressWarnings("unused") GameNetworkingProtocolConnection client : clientConnections) {
			game.createPlayer();
		}
		
		game.addTreasureRandomly();
		
		// send user list to clients
		int i = 0;
		for (GameNetworkingProtocolConnection client : clientConnections) {
			client.sendInitGameMessage(i++, gameMap);
		}
			
		
		
		// wait for all clients to start
		// maybe do this synchronized ( copy() )
		while (!client_states.all_ready()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
				
		for (GameNetworkingProtocolConnection client : clientConnections) {
			for (Player p: game.players) {
				client.sendInsertPlayer(p);
			}
			
			for (Treasure t: game.treasures) {
				client.sendInsertTreasure(t);
			}
		}
		
		for (GameNetworkingProtocolConnection client : clientConnections) {
			client.sendStartGameMessage();
		}
		
		// start local server without graphics
		// this is the mainloop for the simulation (this does not draw anything):
		while(!game.isFinished()) {
			
			final long starttime = System.nanoTime(); 
			
			game.copyInputsFromAccumulator(client_states);
			
			game.tick();
			
			game.sendStateToClients(clientConnections);
			
			final long undertime = MIN_FRAMETIME - (System.nanoTime() - starttime);
			if (undertime > FRAMETIME_COARSENESS) {
				try {
					Thread.sleep(undertime / 1000000, (int) (undertime % 1000000));
				} catch (InterruptedException e) {
					// thats ok...
				}
			}
		}
		
		for(GameNetworkingProtocolConnection client: clientConnections) {
			client.sendEndGameMessage(game.getWinner());
		}
		
		game = null;
	}
	
	
	
	/**
	 * A user disconnected
	 * @param user user who disconnected
	 */
	public void handleRemovedUser(GameNetworkingProtocolConnection user) {
		// remove user from list
		clientConnections.remove(user);
		
		if (game == null) {
			ArrayList<UserInfo> currentUsers = new ArrayList<UserInfo>(clientConnections.size());
			for (GameNetworkingProtocolConnection client : clientConnections) {
				currentUsers.add(new UserInfo(client.targetIP, client.targetPort));
			}
			if (userRegisteredListener != null)
				userRegisteredListener.onUserListChanged(currentUsers);
			
			// send user list to clients
			for (GameNetworkingProtocolConnection client : clientConnections) {
				client.sendUserList(clientConnections);
			}
		}
	}
	
	
	
	/**
	 * Shutdown server, close all Sockets and end Threads.
	 * If it's already stopped nothing happens.
	 */
	public void shutdownServer() {
		if (serverIsRunning) {
			// set running flag to false
			serverIsRunning = false;
			// close sockets
			if (gnpClientAcceptor != null) {
				gnpClientAcceptor.close();
			}
			// end threads
			if (clientListenerThread != null) {
				clientListenerThread.interrupt();
			}
			
			if (game != null && !game.isFinished()) {
				game.cancelGame();
				
				for (GameNetworkingProtocolConnection user : clientConnections) {
					user.sendEndGameMessage(-1);
				}
			}
			
			// disconnect all users
			for (GameNetworkingProtocolConnection user : clientConnections) {
				user.disconnect();
			}
		}
		// stop multicast advertiser
		if (multicaster != null) {
			multicaster.stop();
		}
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		shutdownServer();
	}
	
	
	
	/**
	 * Check whether an instance of this Service is running
	 * @return true, if the service is running
	 * @author http://stackoverflow.com/a/5921190/559745
	 */
	public static boolean isRunning(Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (GameServerService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	
	
	/** Binder interface to this Service */
	public class GameServerBinder extends Binder {
		/**
		 * Get the Server's Port
		 */
		public int getServerPort() {
			return serverPort;
		}
		/**
		 * Set a listener for user register events
		 * @param listener Callback for user register events
		 */
		public void setUserListEventListener(UserListEventListener listener) {
			GameServerService.this.userRegisteredListener = listener;
		}
		/**
		 * Start the Game
		 * @param gameMap map to play on
		 */
		public void initGame(final String gameMap) {
			new Thread(new Runnable() {
				public void run() {
					GameServerService.this.initGame(gameMap);
				}
			}).start();
		}
		/**
		 * Check whether the Server (not service) is running
		 */
		public boolean isRunning() {
			return GameServerService.this.serverIsRunning;
		}
		/**
		 * Stop the Server (and Service)
		 */
		public void stopService() {
			shutdownServer();
			GameServerService.this.stopSelf();
		}
	}
	
	
	
	/**
	 * Callback for user register events
	 */
	public static interface UserListEventListener {
		/**
		 * A user was registered or removed
		 * @param userList List of users
		 */
		void onUserListChanged(ArrayList<UserInfo> userList);
	}

	
	
	public static class UserInfo {
		/** User's IP Address */
		public String ip;
		/** User's Port */
		public int port;
		
		/**
		 * @param ip User's IP Address
		 * @param port User's port
		 */
		public UserInfo(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof UserInfo))
				return false;
			UserInfo othr = (UserInfo) o;
			if (!this.ip.equals(othr.ip) || this.port != othr.port)
				return false;
			return true;
		}

	    /**
	     * Check whether two Lists contain same elements
	     * @param a List A
	     * @param b List B
	     * @return
	     */
	    public static boolean listsContainSameElements(ArrayList<UserInfo> a, ArrayList<UserInfo> b) {
			if (a.size() != b.size()) {
				// not same size
				return false;
			}
			else {
				check: for (Object aO : a) {
					for (Object bO : b)
						if (aO.equals(bO))
							continue check;
					// aO is not in b
					return false;
				}
			}
			// equal
			return true;
	    }
	}
	
}
