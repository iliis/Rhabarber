package com.floern.rhabarber.network2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

public class ServerAdvertisingListener {

	/** Flag, true if a Server discovery Thread is running */
	private volatile boolean serverDiscoveryRunning;
	/** Multicast socket for receiving game server broadcasts */
	private volatile MulticastSocket serverDiscoverySocket;


	/** Group (Multicast IP) */
	private final String multicastGroup;
	/** Port for receiving multicast messages from game server */
	private final int multicastPort;
	/** MulticastLock needed to receive multicast packets */
	private final MulticastLock multicastLock;
	
	
	/**
	 * Multicast listener to discover game server
	 * @param multicastGroup Group (Multicast IP)
	 * @param multicastPort Port for receiving multicast messages from game server
	 */
	public ServerAdvertisingListener(final Context context, String multicastGroup, int multicastPort) {
		this.multicastGroup = multicastGroup;
		this.multicastPort = multicastPort;
		// setup MulticastLock
		multicastLock = ((WifiManager)context.getSystemService(Context.WIFI_SERVICE))
						.createMulticastLock(getClass().getCanonicalName());
		multicastLock.setReferenceCounted(false);
	}
	
	
	/**
	 * Listen for Game Server broadcasts
	 * @param discoveryEventListener Callback for server discovery events
	 */
	public void startDiscoverServer(final ServerDiscoveryEventListener discoveryEventListener) {
		if (!serverDiscoveryRunning) {
			serverDiscoveryRunning = true;
			
			// acquire multicast lock
			multicastLock.acquire();
			
			// receiver thread
			new Thread(new Runnable() {
				public void run() {
					try {
						Log.d("ServerAdvertisingListener", "Setup Multicast Receiver");
						// setup receiver socket
						serverDiscoverySocket = new MulticastSocket(multicastPort);
						serverDiscoverySocket.setReuseAddress(true);
						InetAddress group = InetAddress.getByName(multicastGroup);
						serverDiscoverySocket.joinGroup(group);
						serverDiscoverySocket.setSoTimeout(15000);
						DatagramPacket packet;
						byte[] buffer = new byte[1024];
						// receive broadcasts
						while (serverDiscoveryRunning) {
							try {
								// wait for and receive packet
								packet = new DatagramPacket(buffer, buffer.length);
								serverDiscoverySocket.receive(packet);
								byte[] data = packet.getData();
								int port = GameNetworkingProtocolConnection.parseServerInfoBroadcastMessage(data);
								if (port == 0) {
									Log.w("ClientNetworkingLogic", "Received multicast packet is invalid: " + BinaryUtils.bytesToHex(data));
									continue;
								}
								// invoke callback
								ServerInfo server = new ServerInfo(packet.getAddress().getHostName(), port);
								discoveryEventListener.onServerUpdate(server);
								Log.d("ClientNetworkingLogic", "Server at " + packet.getAddress().getHostAddress() + ":" + port);
							} catch (InterruptedIOException e) {
								// catch timeout
								//e.printStackTrace();
							}
						}
						serverDiscoverySocket.leaveGroup(group);
						serverDiscoverySocket.close();
					} catch (IOException e) {
						//e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	
	/**
	 * Stop listening for Game Server broadcasts.
	 */
	public void stopDiscoverServer() {
		serverDiscoveryRunning = false;
		// close multicast receiver socket
		if (serverDiscoverySocket != null && !serverDiscoverySocket.isClosed()) {
			serverDiscoverySocket.close();
		}
		// release MulticastLock
		if (multicastLock.isHeld()) {
			multicastLock.release();
		}
	}
	
	
	
	/**
	 * Callback for server discovery events
	 */
	public static interface ServerDiscoveryEventListener {
		/**
		 * A server was discovered
		 * @param server Server data
		 */
		void onServerUpdate(ServerInfo server);
	}

	
	
	public static class ServerInfo {
		/** Server's IP Address */
		public String address;
		/** Server's game port */
		public int port;
		
		/**
		 * @param address Server's Host Name or IP Address
		 * @param port Server's game Port
		 */
		public ServerInfo(String address, int port) {
			this.address = address;
			this.port = port;
		}
		
		/**
		 * Check equality
		 * @param o Object to be compared
		 * @return true if server address and port is equal
		 */
		public boolean equals(Object o) {
			if (!(o instanceof ServerInfo))
				return false;
			ServerInfo othr = (ServerInfo) o;
			if (!this.address.equals(othr.address) || this.port != othr.port)
				return false;
			return true;
		}
	}
	
}
