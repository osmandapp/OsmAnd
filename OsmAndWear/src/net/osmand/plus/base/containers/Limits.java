package net.osmand.plus.base.containers;

public class Limits {

	private final float min;
	private final float max;

	public Limits(float min, float max) {
		this.min = min;
		this.max = max;
	}

	public float getMin() {
		return min;
	}

	public float getMax() {
		return max;
	}
}
