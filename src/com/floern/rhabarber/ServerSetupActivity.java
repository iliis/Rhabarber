package com.floern.rhabarber;

import java.io.IOException;
import java.util.ArrayList;

import com.floern.rhabarber.network2.GameNetworkingProtocolConnection.Message;
import com.floern.rhabarber.network2.GameServerService;
import com.floern.rhabarber.network2.ClientNetworkingLogic.GameRegisterEventListener;
import com.floern.rhabarber.network2.ClientNetworkingLogic.GameUpdateEventListener;
import com.floern.rhabarber.network2.GameServerService.GameServerBinder;
import com.floern.rhabarber.network2.GameServerService.UserInfo;
import com.floern.rhabarber.network2.GameServerService.UserListEventListener;
import com.floern.rhabarber.network2.ClientNetworkingLogic;
import com.floern.rhabarber.network2.NetworkUtils;
import com.floern.rhabarber.network2.UiUtils;
import com.floern.rhabarber.network2.UserListAdapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ServerSetupActivity extends Activity {

	/** ListView to list registered users */
	private ListView userListView;
	/** List of servers (data for ListView) */
	private ArrayList<UserInfo> userList = new ArrayList<UserInfo>();
	
	/** Binder to the Service */
	private GameServerBinder serverBinder;
	
	/** Server Service is bound */
	private boolean boundSevice = false;
	
	/** Listener for User register events */
	private final UserListEventListener userRegisteredListener = new UserListEventListener() {
		public void onUserListChanged(final ArrayList<UserInfo> newUserList) {
			Log.d("UserListEventListener", "onUserListChanged() called");
			runOnUiThread(new Runnable() {
				public void run() {
					userList = newUserList;
					if (userListView != null) {
						Log.d("UserListEventListener", "userListView updated");
						userListView.setAdapter(new UserListAdapter(ServerSetupActivity.this, userList));
						userListView.invalidate();
					}
				}
			});
		}};
	
	/** Connection to Service for binding service */
	private final ServiceConnection serverConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			Toast.makeText(ServerSetupActivity.this, "onServiceConnected() called", Toast.LENGTH_SHORT).show();
			serverBinder = (GameServerBinder) service;
			serverBinder.setUserListEventListener(userRegisteredListener);
			// get port
			int serverPort = serverBinder.getServerPort();
			TextView textPort = (TextView) findViewById(R.id.text_serverport);
			if (textPort != null)
				textPort.setText("Port: " + serverPort);
		}
		public void onServiceDisconnected(ComponentName name) {
			Toast.makeText(ServerSetupActivity.this, "onServiceDisconnected() called", Toast.LENGTH_SHORT).show();
			serverBinder = null;
		}};
	
	/** The device's own IP address */
	private String deviceAddress = null;
	
	
	// CLIENT-SIDE STUFF:
	
	/** true if self joined the game */
	private boolean joinedSelf = false;

	/** Client networking logic */
	private ClientNetworkingLogic networkingLogic;
	
	/** Register event receiver (clientside) */
	private final GameRegisterEventListener registerEventListener = new GameRegisterEventListener(){
		public void onRegisterSuccess() {
			Log.d("RegisterEventListener", "onRegisterSuccess()");
			runOnUiThread(new Runnable() {
				public void run() {
					joinedSelf = true;
				}
			});
		}
		public void onUserListChange(final ArrayList<UserInfo> newUserList) {
			// ignore this, the server has implemented this by itself
		}
		public void onNetworkingError(final String errorMessage) {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(ServerSetupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
				}
			});
		}
	};
	
	/** Game state update eventlistener */
	private final GameUpdateEventListener gameUpdateEventListener = new GameUpdateEventListener() {
		// TODO: duplicate code in ServerJoinAvctivity and ServerSetupActivity
		public void onInitGame(final String gameMap, final int playerIdx) {
			// init game
			runOnUiThread(new Runnable() {
				public void run() {
					networkingLogic.stopAvoidTimeout();
					startGameActivity(gameMap, playerIdx);
				}
			});
		}
		public void onPlayerDataUpdate() {
			// TODO example method for receiving game state updates
		}
	};
	
	
	
	/** onCreate */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	
	@Override
	protected void onStart() {
		super.onStart();
		// get IP address
		deviceAddress = NetworkUtils.getLocalIPAddress();
		// set layout
		if (!GameServerService.isRunning(this)) {
			setUiServerOffline(GameServerService.SERVER_PORT);
		}
		else {
			setUiServerRunning(0); // port is set after service connected
			// connect to service
			Intent i = new Intent(this, GameServerService.class);
			boundSevice = bindService(i, serverConnection, BIND_ABOVE_CLIENT);
		}
	}


	/**
	 * OnClick event
	 * @param v
	 */
	public void onStartServer(View v) {
		// read input values
		EditText inputServerPort = (EditText) findViewById(R.id.input_serverport);
		int serverPort = -1;
		try {
			serverPort = Integer.valueOf(inputServerPort.getText().toString().trim());
		} catch (NumberFormatException e) { }
		// validate values
		if (serverPort <= 0 || serverPort > 0xFFFF) {
			Toast.makeText(this, "Invalid Server Port", Toast.LENGTH_SHORT).show();
			return;
		}
		if (!NetworkUtils.portAvailable(serverPort)) {
			Toast.makeText(this, "Port already in use", Toast.LENGTH_SHORT).show();
			return;
		}
		
		// modify UI
		setUiServerRunning(serverPort);
		
		// start gamer server service
		Intent i = new Intent(this, GameServerService.class);
		i.putExtra(GameServerService.EXTRAS_SERVER_PORT, serverPort);
		startService(i);
		boundSevice = bindService(i, serverConnection, BIND_ABOVE_CLIENT);
	}
	
	

	/**
	 * OnClick event join self
	 * @param v
	 */
	public void onJoinSelf(View v) {
		if (!joinedSelf) {
			// join
			
			if (serverBinder == null)
				return; // server not yet setup
			
			CheckBox chbox = (CheckBox) findViewById(R.id.chbox_join_self);
			chbox.setChecked(true);

	    	new Thread(new Runnable() {
	    		// TODO: partial duplicate code in ServerJoinAvctivity and ServerSetupActivity
				public void run() {
			    	// start client-side game networking logic
			    	networkingLogic = new ClientNetworkingLogic("localhost", serverBinder.getServerPort(), 
			    									registerEventListener, gameUpdateEventListener);
			    	
			    	if (networkingLogic.connectionEstablished()) {
				    	// register
						networkingLogic.registerAtServer();
						networkingLogic.startAvoidTimeout();
			    	}
			    	// else an error occurred, will be handled by the registerEventListener
				}
			}).start();
		}
		else {
			// leave
			joinedSelf = false;
			CheckBox chbox = (CheckBox) findViewById(R.id.chbox_join_self);
			chbox.setChecked(false);
			networkingLogic.stopAvoidTimeout();
			networkingLogic.unregisterAtServer();
		}
	}
	
	

	/**
	 * OnClick event start game
	 * @param v
	 */
	public void onStartGame(View v) {
		// get selected map
		Spinner mapChooser = (Spinner) findViewById(R.id.spinner_map);
		String  gameMap    = (String)  mapChooser.getSelectedItem();
		
		// start game
		serverBinder.initGame(gameMap);
		
		// disable start-game button
		Button b = (Button) findViewById(R.id.btn_start_game);
		b.setEnabled(false);
		b.setText("Server is running ...");
		
		if (joinedSelf) {
			// continue self to battlefield if joined self
			// should be handled by the client-logic
		}
	}
    
    
    
    /**
     * Game init, start Game's Activity
     * @param gameMap 
     * @param playerIdx 
     */
    public void startGameActivity(String gameMap, int playerIdx) {
		// TODO: duplicate code in ServerJoinAvctivity and ServerSetupActivity
    	Intent i = new Intent(this, GameActivity.class);
    	GameActivity.__clientNetworkingLogic = networkingLogic;
    	i.putExtra("level",     gameMap);
    	i.putExtra("playerIdx", playerIdx);
    	i.putExtra("isserver",  false); // handle local client as a normal networked one
    	startActivity(i);
    }
	
	

	/**
	 * Set UI when server is running
	 * @param serverPort
	 */
	private void setUiServerRunning(int serverPort) {
		setContentView(R.layout.serversetup_running);
		// port
		TextView textServerPort = (TextView) findViewById(R.id.text_serverport);
		if (serverPort != 0)
			textServerPort.setText("Port: " + serverPort);
		else
			textServerPort.setText("Port: unknown");
		// ip address
		TextView textServerIP = (TextView) findViewById(R.id.text_ipaddress);
		if (deviceAddress != null)
			textServerIP.setText("IP Address: " + deviceAddress);
		else
			textServerIP.setText("IP Address: unknown");
		
		// get user listview
		userListView = (ListView) findViewById(R.id.list_userlist);
		userListView.setAdapter(new UserListAdapter(this, userList));
		userListView.setEmptyView(findViewById(R.id.emptylist_userlist));
		
		// set map chooser list values
		Spinner mapChooser = (Spinner) findViewById(R.id.spinner_map);
		String[] levels;
		try {
			levels = this.getAssets().list("level");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Rhabarber", "Couldn't load levels from asset/level/");
			Toast.makeText(this, "Couldn't find any levels.", Toast.LENGTH_SHORT).show();
			levels = new String[0];
		}
		ArrayAdapter<String> maplistAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, levels);
		maplistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mapChooser.setAdapter(maplistAdapter);
	}

	
	/**
	 * Set UI when server is offline
	 * @param serverPort
	 * @param serverIP
	 */
	private void setUiServerOffline(int serverPort) {
		setContentView(R.layout.serversetup);
		// set Port
		EditText inputServerPort = (EditText) findViewById(R.id.input_serverport);
		inputServerPort.setText(String.valueOf(serverPort));
		// set IP
		TextView textServerIP = (TextView) findViewById(R.id.text_ipaddress);
		if (deviceAddress != null)
			textServerIP.setText("IP Address: " + deviceAddress);
		else
			textServerIP.setText("IP Address: unknown");
	}


	/**
	 * OnClick event
	 * @param v
	 */
	public void onStopServer(View v) {
		// revert UI
		//setUiServerOffline(serverBinder.getServerPort());
		// shutdown game serverserverBinder
		serverBinder.stopService();
		
		// recreate activity
		finish();
    	startActivity(getIntent());
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		// unbind game server service
		if (boundSevice) {
			unbindService(serverConnection);
			boundSevice = false;
		}
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
}
