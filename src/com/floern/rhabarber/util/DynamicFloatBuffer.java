package com.floern.rhabarber.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class DynamicFloatBuffer {
	
	static final int sizeofFloat = 4; // four bytes per float
	
	private FloatBuffer buf;
	private int size = 0;
	
	public DynamicFloatBuffer(int initsize) {
		buf = ByteBuffer.allocateDirect(initsize*sizeofFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}
	
	public void put(float v) {
		
		if(size >= buf.capacity()) {
			
			// copy into bigger array
			FloatBuffer old = buf;
			buf = ByteBuffer.allocateDirect(old.capacity()*sizeofFloat*2).order(ByteOrder.nativeOrder()).asFloatBuffer();
			old.rewind();
			buf.put(old);
		}
		
		// always keep its internal pointer to 0 and handle size manually
		buf.rewind().position(size);
		buf.put(v);
		buf.rewind();
		++size;
	}
	
	public FloatBuffer get() {
		return buf;
	}
}
