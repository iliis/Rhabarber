package com.floern.rhabarber.network2;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public final class NetworkUtils {

    
    /** RegEx to match an IPv4 address */
    private static final String VALIDATE_IPv4_REGEX = 
    		"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
    		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    /** Pattern to match an IPv4 address */
    private static final Pattern VALIDATE_IPv4_PATTERN = Pattern.compile(VALIDATE_IPv4_REGEX);

    /** RegEx to match an IPv6 address */
    private static final String VALIDATE_IPv6_REGEX = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
    /** Pattern to match an IPv6 address */
    private static final Pattern VALIDATE_IPv6_PATTERN = Pattern.compile(VALIDATE_IPv6_REGEX);
    
    
    
    /**
     * Validate format of an IPv4 address
     * @param ip IPv4 address
     * @return true if valid
     */
    public static boolean validateIPv4AddressFormat(final String ip) {
    	Matcher matcher = VALIDATE_IPv4_PATTERN.matcher(ip);
    	return matcher.matches();
    }
    
    
    /**
     * Validate format of an IPv4 address
     * @param ip IPv4 address
     * @return true if valid
     */
    public static boolean validateIPv6AddressFormat(final String ip) {
    	Matcher matcher = VALIDATE_IPv6_PATTERN.matcher(ip);
    	return matcher.matches();
    }
    
    
    /**
     * Validate format of an IPv4 or IPv6 address
     * @param ip IPv4 or IPv6 address
     * @return true if valid
     */
    public static boolean validateIPAddressFormat(final String ip) {
    	return validateIPv4AddressFormat(ip) || validateIPv6AddressFormat(ip);
    }
    

    /**
     * Validate a host address (Name or IP)
     * @param host Name or IP
     * @return true if valid
     */
    public static boolean validateNetAddress(final String host) {
    	try {
    		InetAddress.getByName(host);
    	} catch (Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }
    
    
    /**
     * Convert a IPv4 adress to its binary representation
     * @param ip IP address
     * @return bytes
     */
    public static byte[] IPv4ToBinary(String ip) {
    	if (validateIPv4AddressFormat(ip)) {
    		// IPv4
    		String[] nets = ip.split(".");
    		byte[] ipBinary = new byte[4];
    		for (int i=0; i<4; ++i) {
    			try {
    				ipBinary[i] = (byte) (0xFF & Integer.parseInt(nets[i]));
    			} catch (NumberFormatException e) {
    				ipBinary[i] = 0;
    			}
    		}
    		return ipBinary;
    	}
    	return null;
    }
    
    
    /**
     * Get the local IP address of this device
     * @return IP address
     */
    public static String getLocalIPAddress() {
    	//www.droidnova.com/get-the-ip-address-of-your-device,304.html
    	String ip = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                    	Log.i("getLocalIPAddress()", "IP: " + inetAddress.getHostAddress().toString());
                    	ip = inetAddress.getHostAddress().toString().split("%", 2)[0];
                    }
                }
            }
        } catch (SocketException ex) { }
        return ip;
    }
    
    
    /**
     * Get the local host name address of this device
     * @return host name or IP address if no name available
     */
    public static String getLocalHostName() {
    	//www.droidnova.com/get-the-ip-address-of-your-device,304.html
    	String name = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                    	Log.i("getLocalHostName()", "Hostname: " + inetAddress.getHostName());
                    	name = inetAddress.getHostName();
                    }
                }
            }
        } catch (SocketException ex) { }
        return name;
    }
    
    
	/**
	 * Check ehether a port on localhost is available
	 * @param port Port to check
	 * @return truw if available
	 */
	public static boolean portAvailable(int port) {
	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }
	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	            }
	        }
	    }
	    return false;
	}
	
}
