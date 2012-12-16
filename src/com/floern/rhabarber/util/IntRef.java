package com.floern.rhabarber.util;

/**
 * A wrapper for int, as Integer is immutable and not suitable for pass by reference.
 * @author samuel
 *
 */
public class IntRef {
	public int value;
	
	public IntRef()      {this.value = 0;}
	public IntRef(int v) {this.value = v;}
}
