package net.osmand.data;

public class PointInt31 {
	public int x;
	public int y;

//	public PointInt31() {
//	}

	public PointInt31(int x, int y) {
		this.x = x;
		this.y = y;
	}

//	public PointInt31(PointInt31 a) {
//		this(a.x, a.y);
//	}

//	public void set(int x, int y) {
//		this.x = x;
//		this.y = y;
//	}

	@Override
	public String toString() {
		return "x " + x + " y " + y;
	}
}
