package com.floern.rhabarber.util;

import at.emini.physics2D.util.FXUtil;

public class FXMath {
	
	public static int floatToFX(float v) {
		return (int) (v * (1 << FXUtil.DECIMAL));
	}
	
	// use FXVector.xGetAsFloat() whenever possible
	public static float FXtoFloat(int v) {
		return ((float) v) / (1 << FXUtil.DECIMAL);
	}
	
	// FX2 uses 24 lower bits (for small values)
	public static float FX2toFloat(int v) {
		return ((float) v) / (1 << FXUtil.DECIMAL2);
	}
}
