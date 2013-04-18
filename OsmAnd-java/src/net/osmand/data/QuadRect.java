package net.osmand.data;

public class QuadRect {
	public float left;
	public float right;
	public float top;
	public float bottom;

	public QuadRect(float left, float top, float right, float bottom) {
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

	public float width() {
		return right - left;
	}

	public float height() {
		return bottom - top;
	}

	public boolean contains(float left, float top, float right, float bottom) {
		return this.left < this.right && this.top < this.bottom && this.left <= left && this.top <= top && this.right >= right
				&& this.bottom >= bottom;
	}

	public boolean contains(QuadRect box) {
		return contains(box.left, box.top, box.right, box.bottom);
	}

	public static boolean intersects(QuadRect a, QuadRect b) {
		return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom;
	}

	public float centerX() {
		return (left + right) / 2;
	}

	public float centerY() {
		return (top + bottom) / 2;
	}

	public void offset(float dx, float dy) {
		left += dx;
		top += dy;
		right += dx;
		bottom += dy;

	}

	public void inset(float dx, float dy) {
		left += dx;
		top += dy;
		right -= dx;
		bottom -= dy;
	}

}