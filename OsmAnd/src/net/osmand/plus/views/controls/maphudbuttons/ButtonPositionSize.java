package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;

import java.util.ArrayList;
import java.util.List;

public class ButtonPositionSize {

	public static final int CELL_SIZE_DP = 8;
	public static final int DEF_MARGIN_DP = 4;

	private static final int MAX_MARGIN_BITS = 10;
	private static final int MAX_SIZE_BITS = 6;
	private static final int MARGIN_MASK = (1 << MAX_MARGIN_BITS) - 1;
	private static final int SIZE_MASK = (1 << MAX_SIZE_BITS) - 1;

	public boolean left = true, top = true; // right, bottom false
	public int marginX = 0, marginY = 0; // in 8dp scale
	public int width = 7, height = 7; // in 8dp scale including shadow
	public boolean xMove = false, yMove = false;
	public String id;

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

	public long toLongValue() {
		long vl = 0;
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

	public void calcGridFromBottomRight(float dpToPix, int widthPx, int heightPx, int xRight, int yBottom) {
		// TODO test
		float calc;
		if (xRight < widthPx / 2) {
			this.left = true;
			calc = xRight / dpToPix - this.width;
		} else {
			this.left = false;
			calc = (widthPx - xRight) / dpToPix;
		}
		this.marginX = Math.max(0, Math.round((calc - DEF_MARGIN_DP) / CELL_SIZE_DP));
		if (yBottom < heightPx / 2) {
			this.top = true;
			calc = yBottom / dpToPix - this.height;
		} else {
			this.top = false;
			calc = (heightPx - yBottom) / dpToPix;
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
	}

	@Override
	public String toString() {
		return String.format("Pos %10s x=(%s->%d%s), y=(%s->%d%s), w=%2d, h=%2d", id, left ? "left " : "right", marginX,
				xMove ? "+" : " ", top ? "top " : "bott", marginY, yMove ? "+" : " ", width, height);
	}

	public boolean overlap(ButtonPositionSize b) {
		if (this.left == b.left && this.top == b.top) {
			boolean intersect = this.marginX < b.marginX + b.width && this.marginX + this.width > b.marginX;
			intersect &= this.marginY < b.marginY + b.height && this.marginY + this.height > b.marginY;
			return intersect;
		}
		return false;
	}

	public static void computeNonOverlap(int space, List<ButtonPositionSize> buttons) {
		int MAX_ITERATIONS = 1000, iter = 0;
		for (int i = 1; i < buttons.size(); i++) {
			boolean overlap = true;
			ButtonPositionSize btn = buttons.get(i);
			while (overlap && iter++ < MAX_ITERATIONS) {
				overlap = false;
				for (int j = 0; j < i; j++) {
					ButtonPositionSize b2 = buttons.get(j);
					if (b2.overlap(btn)) {
						overlap = true;
						if (btn.xMove || !btn.yMove) {
							btn.marginX = space + b2.marginX + b2.width;
						}
						if (btn.yMove) {
							btn.marginY = space + b2.marginY + b2.height;
						}
						break;
					}
				}
			}
		}
	}

	public static List<ButtonPositionSize> defaultLayoutExample() {
		List<ButtonPositionSize> lst = new ArrayList<>();
		lst.add(new ButtonPositionSize(ZOOM_OUT_HUD_ID, 7, false, false).setMoveVertical());
		lst.add(new ButtonPositionSize(ZOOM_IN_HUD_ID, 7, false, false).setMoveVertical());
		lst.add(new ButtonPositionSize(BACK_TO_LOC_HUD_ID, 7, false, false).setMoveHorizontal());

		lst.add(new ButtonPositionSize(MENU_HUD_ID, 7, true, false).setMoveHorizontal());
		lst.add(new ButtonPositionSize(ROUTE_PLANNING_HUD_ID, 7, true, false).setMoveHorizontal());
		lst.add(new ButtonPositionSize("ruler", 10, true, false).setMoveHorizontal());

		lst.add(new ButtonPositionSize(LAYERS_HUD_ID, 6, true, true).setMoveHorizontal());
		lst.add(new ButtonPositionSize(QUICK_SEARCH_HUD_ID, 6, true, true).setMoveHorizontal());
		lst.add(new ButtonPositionSize(COMPASS_HUD_ID, 6, true, true).setMoveVertical());
		return lst;
	}
}
