package com.floern.rhabarber;

import java.util.ArrayList;

import com.floern.rhabarber.network2.ClientNetworkingLogic;
import com.floern.rhabarber.network2.ClientNetworkingLogic.GameRegisterEventListener;
import com.floern.rhabarber.network2.GameServerService;
import com.floern.rhabarber.network2.GameServerService.UserInfo;
import com.floern.rhabarber.network2.NetworkUtils;
import com.floern.rhabarber.network2.ServerAdvertisingListener;
import com.floern.rhabarber.network2.ServerAdvertisingListener.ServerInfo;
import com.floern.rhabarber.network2.ServerListAdapter;
import com.floern.rhabarber.network2.UiUtils;
import com.floern.rhabarber.network2.UserListAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ServerJoinActivity extends Activity {
	
	/** ListView to list available servers */
	private ListView serverListView;
	/** List of servers (data for ListView) */
	private ArrayList<ServerInfo> serverList = new ArrayList<ServerInfo>();
	
	/** ListView to list available servers */
	private ListView userListView;
	/** List of servers (data for ListView) */
	private ArrayList<UserInfo> userList = new ArrayList<UserInfo>();
	
	/** Server discoverer */
	private ServerAdvertisingListener serverDiscoverer;
	
	/** Client networking logic */
	private ClientNetworkingLogic networkingLogic;
	
	private boolean isRegistered = false;
	
	/** Register event receiver */
	private final GameRegisterEventListener registerEventListener = new GameRegisterEventListener(){
		public void onRegisterSuccess() {
			Log.d("RegisterEventListener", "onRegisterSuccess()");
			runOnUiThread(new Runnable() {
				public void run() {
					isRegistered = true;
					setUiRegistered();
				}
			});
		}
		public void onUserListChange(final ArrayList<UserInfo> newUserList) {
			Log.d("RegisterEventListener", "onUserListChange()");
			if (!UserInfo.listsContainSameElements(newUserList, userList)) {
				// update list
				runOnUiThread(new Runnable() {
					public void run() {
						userList = newUserList;
			            userListView.setAdapter(new UserListAdapter(ServerJoinActivity.this, userList));
					}
				});
			}
		}
		public void onNetworkingError(final String errorMessage) {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(ServerJoinActivity.this, errorMessage, Toast.LENGTH_LONG).show();
					if (isRegistered) {
						// TODO: reset ui
					}
					else {
						// do nothing
					}
				}
			});
		}
	};
	
	
    /** onCreate */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setUiNotRegistered();
    }
    
    
    /**
     * OnClick method to join a server
     */
    public void onClickJoinServer(View v) {
    	EditText inputServerIP = (EditText) findViewById(R.id.input_serverip);
    	EditText inputServerPort = (EditText) findViewById(R.id.input_serverport);

    	final String serverIP = inputServerIP.getText().toString().trim();
    	int serverPort = -1;
    	try {
    		serverPort = Integer.valueOf(inputServerPort.getText().toString().trim());
    	} catch (NumberFormatException e) {
		}
    	
    	if (serverPort <= 0 || 0xFFFF < serverPort) {
    		Toast.makeText(this, "Invalid Server Port", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	final int serverPortF = serverPort; // need final here
    	
    	new Thread(new Runnable() {
			public void run() {
				// validate IP (may require networking)
		    	if (!NetworkUtils.validateNetAddress(serverIP)) {
		    		UiUtils.toastOnUiThread(ServerJoinActivity.this, "Invalid Server IP", Toast.LENGTH_SHORT);
		    		return;
		    	}

		    	stopDiscoverServer();
		    	
		    	// start client-side game networking logic
		    	networkingLogic = new ClientNetworkingLogic(serverIP, serverPortF, registerEventListener, null); // TODO: define listeners
		    	
		    	if (networkingLogic.connectionEstablished()) {
			    	// register
					networkingLogic.registerAtServer();
					networkingLogic.startAvoidTimeout();
		    	}
		    	// else an error occurred, will be handled by the registerEventListener
			}
		}).start();
    }
    
    
    /**
     * Set User Interface to non-registered state
     */
    private void setUiNotRegistered() {
        setContentView(R.layout.serverjoin);
        // server list
        serverListView = (ListView) findViewById(R.id.list_serverlist);
		serverListView.setAdapter(new ServerListAdapter(this, serverList));
		serverListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
				// copy chosen server data to form
				ServerInfo server = (ServerInfo) itemView.getTag();
		    	EditText inputServerIP = (EditText) findViewById(R.id.input_serverip);
		    	EditText inputServerPort = (EditText) findViewById(R.id.input_serverport);
		    	inputServerIP.setText(server.address);
		    	inputServerPort.setText(Integer.toString(server.port));
			}
		});
		serverListView.setEmptyView(findViewById(R.id.emptylist_serverlist));
    }
    
    
    /**
     * Set User Interface to registered state
     */
    private void setUiRegistered() {
        setContentView(R.layout.serverjoin_registered);
        // user list
        userListView = (ListView) findViewById(R.id.list_userlist);
        userListView.setAdapter(new UserListAdapter(this, userList));
        
        serverListView = null;
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
    	startDiscoverServer();
    }
    
    
    @Override
    protected void onPause() {
    	super.onPause();
    	stopDiscoverServer();
    }
    
    
    /**
     * Start listening for game server broadcasts
     */
    private void startDiscoverServer() {
    	if (serverDiscoverer == null) {
	    	serverDiscoverer = new ServerAdvertisingListener(this, GameServerService.MULTICAST_GROUP, GameServerService.MULTICAST_RECEIVE_PORT);
	    	serverDiscoverer.startDiscoverServer(new ServerAdvertisingListener.ServerDiscoveryEventListener() {
				public void onServerUpdate(final ServerInfo serverInfo) {
					// update server list, check for changes
					for (ServerInfo server : serverList) {
						if (serverInfo.equals(server)) {
							// server already in list
							return;
						}
					}
					runOnUiThread(new Runnable() {
						public void run() {
							// update list
							serverList.add(serverInfo);
							((ServerListAdapter)serverListView.getAdapter()).notifyDataSetChanged();
						}
					});
				}
			});
    	}    	
    }
    
    
    /**
     * Stop listening for game server broadcasts
     */
    private void stopDiscoverServer() {
    	if (serverDiscoverer != null) {
    		serverDiscoverer.stopDiscoverServer();
    		serverDiscoverer = null;
    	}
    }
    
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	// log off from server if registered
    	if (isRegistered) {
    		
    	}
    }
    
}