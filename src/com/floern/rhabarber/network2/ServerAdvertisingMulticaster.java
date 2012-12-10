package com.floern.rhabarber.network2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import android.util.Log;

public class ServerAdvertisingMulticaster {
	
	/** Thread for networking */
	private Thread multicastThread;
	
	/** true if multicast thread is running */
	private volatile boolean isMulticasting = false;

	/** Multicast Group */
	private final String multicastGroupIP;
	/** Client's multicast receiver port */
	private final int clientTargetPort;
	
	/** Server's Port */
	private int serverPort;
	
	/** Amout of time in milliseconds to wait until resending packet */
	private final static int SEND_FREQUENCY = 2000;
	
	
	/**
	 * Setup multicast advertiser
	 * @param serverPort Server's preferred receiver port
	 * @param multicastGroupIP Multicast Group (IP adress)
	 * @param clientTargetPort Client's multicast receiver port
	 */
	public ServerAdvertisingMulticaster(int serverPort, String multicastGroupIP, int clientTargetPort) {
		this.serverPort = serverPort;
		this.multicastGroupIP = multicastGroupIP;
		this.clientTargetPort = clientTargetPort;
	}
	
	
	/**
	 * Start multicasting
	 */
	public void start() {
		if (!isMulticasting) {
			isMulticasting = true;
			multicastThread = new Thread(new Runnable() {
				public void run() {
					try {
						Log.d("ServerAdvertisingMulticaster", "Setup Multicast ("+multicastGroupIP+":"+clientTargetPort+")");
						Log.d("ServerAdvertisingMulticaster", "Server Game Port is "+serverPort);
						// get message to be broadcasted
						byte[] data = GameNetworkingProtocolConnection.getServerInfoBroadcastMessage(serverPort).getBytes();
						// setup socket
						InetAddress group = InetAddress.getByName(multicastGroupIP);
						MulticastSocket socket = new MulticastSocket();
						socket.joinGroup(group);
						while (isMulticasting) {
							Log.d("ServerAdvertisingMulticaster", "Send Multicast (Group "+multicastGroupIP+":"+clientTargetPort+")");
							// send a packet
							DatagramPacket packet = new DatagramPacket(data, data.length, group, clientTargetPort);
							socket.send(packet);
							try {
								Thread.sleep(SEND_FREQUENCY);
							} catch (InterruptedException e) {
							}
						}
						// stop multicasting
						socket.leaveGroup(group);
						socket.close();
					} catch (IOException e) {
						//e.printStackTrace();
						isMulticasting = false;
					}
					Log.d("ServerAdvertisingMulticaster", "Multicast Thread exited");
				}
			});
			multicastThread.start();
		}
	}
	
	
	/**
	 * Stop multicasting. Nothing happens if already stopped
	 */
	public void stop() {
		isMulticasting = false;
		if (multicastThread != null) {
			multicastThread.interrupt();
			multicastThread = null;
		}
	}
	
	
}
