package com.floern.rhabarber.network2;

public final class BinaryUtils {
	
	/**
	 * Get a hexadecimal String representation of a given byte array
	 * @param bytes binary data
	 * @return String with space-separated hex representation
	 */
	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		if (bytes.length > 0)
			sb.append(String.format("%02x", bytes[0] & 0xff));
		for (int i=1; i<bytes.length; ++i)
			sb.append(String.format(" %02x", bytes[i] & 0xff));
		return sb.toString();
	}
	
	
	/**
	 * Concat multiple byte arrays
	 * @param arrays
	 * @return
	 */
	public static byte[] concatArrays(byte[]... arrays) {
	    // result array length
	    int totalLength = 0;
	    for (int i = 0; i < arrays.length; ++i)
	        totalLength += arrays[i].length;
	    // create the result array
	    byte[] result = new byte[totalLength];
	    // copy the source arrays into the result array
	    int currentIndex = 0;
	    for (int i = 0; i < arrays.length; ++i) {
	        System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
	        currentIndex += arrays[i].length;
	    }
	    return result;
	}
	
}
