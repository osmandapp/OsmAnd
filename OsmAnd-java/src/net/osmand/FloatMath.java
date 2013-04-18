package net.osmand;

/**
 * This class can be replaced with more effective on ARM devices 
 */
public class FloatMath {

	public static float PI = (float) Math.PI;
	
	public static float cos(float a) {
		return (float) Math.cos(a);
	}
	
	public static float sin(float a) {
		return (float) Math.sin(a);
	}
	
	public static float abs(float a) {
		return (float) Math.abs(a);
	}

	public static float atan2(float py, float px) {
		return (float) Math.atan2(py, px);
	}

	public static float sqrt(float f) {
		return (float) Math.sqrt(f);
	}

	public static float max(float a, float b) {
		if(a > b) {
			return a;
		}
		return b;
	}
	
	
}
