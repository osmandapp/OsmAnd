package net.osmand.data;

public class QuadPoint {
	public float x;
	public float y;

	public QuadPoint() {
	}

	public QuadPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public QuadPoint(QuadPoint a) {
		this(a.x, a.y);
	}

	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "x " + x + " y " + y;
	}
}