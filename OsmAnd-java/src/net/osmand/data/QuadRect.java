package net.osmand.data;

public class QuadRect {
	public double left;
	public double right;
	public double top;
	public double bottom;

	public QuadRect(double left, double top, double right, double bottom) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}

	public QuadRect(QuadRect a) {
		this(a.left, a.top, a.right, a.bottom);
	}

	public QuadRect() {
	}

	public double width() {
		return right - left;
	}

	public double height() {
		return bottom - top;
	}

	public boolean contains(double left, double top, double right, double bottom) {
		return this.left < this.right && this.top < this.bottom && this.left <= left && this.top <= top && this.right >= right
				&& this.bottom >= bottom;
	}

	public boolean contains(QuadRect box) {
		return contains(box.left, box.top, box.right, box.bottom);
	}

	public static boolean intersects(QuadRect a, QuadRect b) {
		return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom;
	}

	public static boolean trivialOverlap(QuadRect a, QuadRect b) {
		return !((a.right < b.left) || (a.left > b.right) || (a.top < b.bottom) || (a.bottom > b.top));
	}

	public double centerX() {
		return (left + right) / 2;
	}

	public double centerY() {
		return (top + bottom) / 2;
	}

	public void offset(double dx, double dy) {
		left += dx;
		top += dy;
		right += dx;
		bottom += dy;

	}

	public void inset(double dx, double  dy) {
		left += dx;
		top += dy;
		right -= dx;
		bottom -= dy;
	}
	
	@Override
	public String toString() {
		return "[" + (float) left + "," + (float) top + " - " + (float) right + "," + (float) bottom + "]";
	}

}