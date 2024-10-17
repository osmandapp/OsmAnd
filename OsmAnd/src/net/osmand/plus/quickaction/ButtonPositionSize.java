package net.osmand.plus.quickaction;

public class ButtonPositionSize {

	public static final int CELL_SIZE_DP = 8;
	public static final int DEF_MARGIN = 4;

	private static final int MAX_MARGIN_BITS = 10;
	private static final int MAX_SIZE_BITS = 6;
	private static final int MARGIN_MASK = (1 << MAX_MARGIN_BITS) - 1;
	private static final int SIZE_MASK = (1 << MAX_SIZE_BITS) - 1;

	public boolean left = true, top = true; // right, bottom false
	public int marginX = 0, marginY = 0; // in 8dp scale
	public int width = 7, height = 7; // in 8dp scale including shadow
	public boolean xMove = false, yMove = false;

	public int toIntValue() {
		int vl = 0;
		vl = (vl << 1) + (top ? 1 : 0);
		vl = (vl << 1) + (yMove ? 1 : 0);
		vl = (vl << MAX_MARGIN_BITS) + Math.max(marginY, MARGIN_MASK);
		vl = (vl << MAX_SIZE_BITS) + Math.max(height, SIZE_MASK);
		vl = (vl << 1) + (left ? 1 : 0);
		vl = (vl << 1) + (xMove ? 1 : 0);
		vl = (vl << MAX_MARGIN_BITS) + Math.max(marginX, MARGIN_MASK);
		vl = (vl << MAX_SIZE_BITS) + Math.max(width, SIZE_MASK);
		return vl;
	}

	public int getXMarginPix(boolean left, int widthPx, float dpToPix) {
		if (left == this.left) {
			return (int) ((marginX * CELL_SIZE_DP + DEF_MARGIN) * dpToPix);
		}
		return 0; // TODO
	}

	public void fromIntValue(int v) {
		width = v & SIZE_MASK;
		v = v >> MAX_SIZE_BITS;
		marginX = v & MARGIN_MASK;
		v = v >> MAX_MARGIN_BITS;
		xMove = v % 2 == 1;
		v = v >> 1;
		left = v % 2 == 1;
		v = v >> 1;
		height = v & SIZE_MASK;
		v = v >> MAX_SIZE_BITS;
		marginY = v & MARGIN_MASK;
		v = v >> MAX_MARGIN_BITS;
		yMove = v % 2 == 1;
		v = v >> 1;
		top = v % 2 == 1;
		v = v >> 1;
	}
}
