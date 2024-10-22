package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ButtonPositionSize {

	private static final Random RANDOM = new Random();

	public static final int CELL_SIZE_DP = 8;
	public static final int DEF_MARGIN_DP = 4;

	private static final int MAX_MARGIN_BITS = 10;
	private static final int MAX_SIZE_BITS = 6;
	private static final int MARGIN_MASK = (1 << MAX_MARGIN_BITS) - 1;
	private static final int SIZE_MASK = (1 << MAX_SIZE_BITS) - 1;

	public boolean left = true, top = true; // right, bottom false
	public int marginX = 0, marginY = 0; // in 8dp scale
	public int width = 7, height = 7; // in 8dp scale including shadow
	public boolean xMove = false, yMove = false, randomMove = false;
	public String id;

	public ButtonPositionSize(String id) {
		this(id, 7, true, true);
	}

	public ButtonPositionSize(String id, int sz8dp, boolean left, boolean top) {
		this.id = id;
		width = height = sz8dp;
		this.left = left;
		this.top = top;
	}

	public ButtonPositionSize setMoveVertical() {
		this.yMove = true;
		return this;
	}

	public ButtonPositionSize setMoveHorizontal() {
		this.xMove = true;
		return this;
	}

	public ButtonPositionSize setMoveRandom() {
		this.randomMove = true;
		return this;
	}

	public ButtonPositionSize setSize(int width8dp, int height8dp) {
		this.width = width8dp;
		this.height = height8dp;
		return this;
	}

	public long toLongValue() {
		long vl = 0;
		vl = (vl << 1) + (randomMove ? 1 : 0);
		vl = (vl << 1) + (top ? 1 : 0);
		vl = (vl << 1) + (yMove ? 1 : 0);
		vl = (vl << MAX_MARGIN_BITS) + Math.min(marginY, MARGIN_MASK);
		vl = (vl << MAX_SIZE_BITS) + Math.min(height, SIZE_MASK);
		vl = (vl << 1) + (left ? 1 : 0);
		vl = (vl << 1) + (xMove ? 1 : 0);
		vl = (vl << MAX_MARGIN_BITS) + Math.min(marginX, MARGIN_MASK);
		vl = (vl << MAX_SIZE_BITS) + Math.min(width, SIZE_MASK);
		return vl;
	}

	public void calcGridPositionFromPixel(float dpToPix, int widthPx, int heightPx,
	                                      boolean gravLeft, int x, boolean gravTop, int y) {
		float calc;
		if (x < widthPx / 2) {
			this.left = gravLeft;
			calc = x / dpToPix;
		} else {
			this.left = !gravLeft;
			calc = (widthPx - x) / dpToPix - this.width * CELL_SIZE_DP;
		}
		this.marginX = Math.max(0, Math.round((calc - DEF_MARGIN_DP) / CELL_SIZE_DP));
		if (y < heightPx / 2) {
			this.top = gravTop;
			calc = y / dpToPix;
		} else {
			this.top = !gravTop;
			calc = (heightPx - y) / dpToPix - this.height * CELL_SIZE_DP;
		}
		this.marginY = Math.max(0, Math.round((calc - DEF_MARGIN_DP) / CELL_SIZE_DP));
	}

	public int getYStartPix(float dpToPix) {
		return (int) ((marginY * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix);
	}

	public int getYEndPix(float dpToPix) {
		return (int) (((marginY + width) * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix);
	}

	public int getXStartPix(float dpToPix) {
		return (int) ((marginX * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix);
	}

	public int getXEndPix(float dpToPix) {
		return (int) (((marginX + width) * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix);
	}

	public void fromLongValue(long v) {
		width = (int) (v & SIZE_MASK);
		v = v >> MAX_SIZE_BITS;
		marginX = (int) (v & MARGIN_MASK);
		v = v >> MAX_MARGIN_BITS;
		xMove = v % 2 == 1;
		v = v >> 1;
		left = v % 2 == 1;
		v = v >> 1;
		height = (int) (v & SIZE_MASK);
		v = v >> MAX_SIZE_BITS;
		marginY = (int) (v & MARGIN_MASK);
		v = v >> MAX_MARGIN_BITS;
		yMove = v % 2 == 1;
		v = v >> 1;
		top = v % 2 == 1;
		v = v >> 1;
		randomMove = v % 2 == 1;
		v = v >> 1;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format(Locale.US, "Pos %10s x=(%s->%d%s), y=(%s->%d%s), random=%s, w=%2d, h=%2d", id,
				left ? "left " : "right", marginX, xMove ? "+" : " ",
				top ? "top " : "bott", marginY, yMove ? "+" : " ",
				randomMove ? "true" : "false", width, height);
	}

	public boolean overlap(ButtonPositionSize b, boolean xMoveAvailable, boolean yMoveAvailable) {
		if (this.top == b.top && xMoveAvailable) {
			return this.marginY < b.marginY + b.height && this.marginY + this.height > b.marginY;
		}
		if (this.left == b.left && yMoveAvailable) {
			return this.marginX < b.marginX + b.width && this.marginX + this.width > b.marginX;
		}
		if (this.left == b.left && this.top == b.top) {
			boolean intersect = this.marginX < b.marginX + b.width && this.marginX + this.width > b.marginX;
			intersect &= this.marginY < b.marginY + b.height && this.marginY + this.height > b.marginY;
			return intersect;
		}
		return false;
	}

	public static void computeNonOverlap(int space, int width, int height, List<ButtonPositionSize> buttons) {
		int MAX_ITERATIONS = 1000, iter = 0;
		for (int i = 1; i < buttons.size(); i++) {
			boolean overlap = true;
			ButtonPositionSize btn = buttons.get(i);
			while (overlap && iter++ < MAX_ITERATIONS) {
				overlap = false;
				for (int j = 0; j < i; j++) {
					ButtonPositionSize b2 = buttons.get(j);
					overlap = computeNonOverlap(space, width, height, btn, b2);
					if (overlap) {
						break;
					}
				}
			}
		}
	}

	public static boolean computeNonOverlap(int space, int width, int height, ButtonPositionSize btn, ButtonPositionSize b2) {
		boolean xMoveUnavailable = b2.width + btn.width >= width;
		boolean yMoveUnavailable = b2.height + btn.height >= height;

		if (b2.overlap(btn, xMoveUnavailable, yMoveUnavailable)) {
			boolean xMove = (btn.xMove || btn.randomMove && RANDOM.nextBoolean() || yMoveUnavailable) && !xMoveUnavailable;
			boolean yMove = (btn.yMove || btn.randomMove && RANDOM.nextBoolean() || xMoveUnavailable) && !yMoveUnavailable;

			if (xMove) {
				btn.marginX = space + b2.marginX + b2.width;
			}
			if (yMove) {
				btn.marginY = space + b2.marginY + b2.height;
			}
			return true;
		}
		return false;
	}

	public static List<ButtonPositionSize> defaultLayoutExample() {
		List<ButtonPositionSize> lst = new ArrayList<>();
		lst.add(new ButtonPositionSize(ZOOM_OUT_HUD_ID, 7, false, false).setMoveVertical());
		lst.add(new ButtonPositionSize(ZOOM_IN_HUD_ID, 7, false, false).setMoveVertical());
		lst.add(new ButtonPositionSize(BACK_TO_LOC_HUD_ID, 7, false, false).setMoveHorizontal());

		lst.add(new ButtonPositionSize(MENU_HUD_ID, 7, true, false).setMoveHorizontal());
		lst.add(new ButtonPositionSize(ROUTE_PLANNING_HUD_ID, 7, true, false).setMoveHorizontal());
//		lst.add(new ButtonPositionSize("ruler", 10, true, false).setMoveHorizontal());

		lst.add(new ButtonPositionSize(LAYERS_HUD_ID, 6, true, true).setMoveHorizontal());
		lst.add(new ButtonPositionSize(QUICK_SEARCH_HUD_ID, 6, true, true).setMoveHorizontal());
		lst.add(new ButtonPositionSize(COMPASS_HUD_ID, 6, true, true).setMoveVertical());
		return lst;
	}
}
