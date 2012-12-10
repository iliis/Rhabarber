package com.floern.rhabarber;

import java.util.ArrayList;

import com.floern.rhabarber.network2.GameServerService;
import com.floern.rhabarber.network2.GameServerService.GameServerBinder;
import com.floern.rhabarber.network2.GameServerService.UserInfo;
import com.floern.rhabarber.network2.GameServerService.UserListEventListener;
import com.floern.rhabarber.network2.NetworkUtils;
import com.floern.rhabarber.network2.UserListAdapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
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
			if (!UserInfo.listsContainSameElements(newUserList, userList)) {
				// update list
				runOnUiThread(new Runnable() {
					public void run() {
						userList = newUserList;
						if (userListView != null)
							userListView.setAdapter(new UserListAdapter(ServerSetupActivity.this, userList));
					}
				});
			}
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
		}};
	
	private String deviceAddress = null;
	
	
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
			userListView = (ListView) findViewById(R.id.list_userlist);
			userListView.setAdapter(new UserListAdapter(this, userList));
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
		setUiServerOffline(serverBinder.getServerPort());
		// shutdown game serverserverBinder
		serverBinder.stopService();
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