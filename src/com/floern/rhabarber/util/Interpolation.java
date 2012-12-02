package com.floern.rhabarber.util;

import android.util.FloatMath;



public class Interpolation {
	
	public static float
	linear_interpolate(float val1, float val2, float percent)
	{
		return val1*(1-percent) + val2*percent;
	};
	
	
	public static float
	smooth_interpolate(float val1, float val2, float percent)
	{
		/// y1 + (y2-y1) * (1-cos( x/delta * PI ))/2
		return val1 + (val2-val1) * (1-FloatMath.cos(percent*((float) Math.PI)))/2;
	}
	
	public static float
	interpolate_angle(float a1, float a2, float percent) {
		final float PI = (float) Math.PI;
		
		float a;

		if(Math.abs(a2-a1) <= PI)
			a = linear_interpolate(a1,a2,percent);
		else
		{
			if(a1<a2)
				a = linear_interpolate(a1+2*PI,a2,percent);
			else
				a = linear_interpolate(a1,a2+2*PI,percent);
		}

		if(a<0)
			return a+2*PI;
		else if(a>2*PI)
			return a-2*PI;
		else
			return a;
	}
}
