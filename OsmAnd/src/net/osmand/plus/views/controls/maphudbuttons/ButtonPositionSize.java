package net.osmand.plus.views.controls.maphudbuttons;


import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;

import org.apache.commons.logging.Log;

import java.util.List;
import java.util.Locale;

public class ButtonPositionSize {

	private static final Log LOG = PlatformUtil.getLog(ButtonPositionSize.class);

	public static final int CELL_SIZE_DP = 8;
	public static final int DEF_MARGIN_DP = 4;

	public static final int MOVE_DESCENDANTS_ANY = 0;
	public static final int MOVE_DESCENDANTS_VERTICAL = 1;
	public static final int MOVE_DESCENDANTS_HORIZONTAL = 2;

	public static final int POS_FULL_WIDTH = 0;
	public static final int POS_LEFT = 1;
	public static final int POS_RIGHT = 2;

	public static final int POS_FULL_HEIGHT = 0;
	public static final int POS_TOP = 1;
	public static final int POS_BOTTOM = 2;

	private static final int MAX_MARGIN_BITS = 10;
	private static final int MAX_SIZE_BITS = 6;
	private static final int MARGIN_MASK = (1 << MAX_MARGIN_BITS) - 1;
	private static final int SIZE_MASK = (1 << MAX_SIZE_BITS) - 1;

	public int posH = POS_LEFT, posV = POS_TOP; // left, right, bottom, top
	public int marginX = 0, marginY = 0; // in 8dp scale
	public int width = 7, height = 7; // in 8dp scale including shadow
	public boolean xMove = false, yMove = false;
	public int moveDescendants = MOVE_DESCENDANTS_ANY;
	public String id;

	private final QuadRect bounds = new QuadRect();

	public ButtonPositionSize(String id) {
		this(id, 7, true, true);
	}

	public ButtonPositionSize(String id, int sz8dp, boolean left, boolean top) {
		this.id = id;
		width = height = sz8dp;
		this.posH = left ? POS_LEFT : POS_RIGHT;
		this.posV = top ? POS_TOP : POS_BOTTOM;
	}

	public ButtonPositionSize(String id, int sz8dp, int posH, int posV) {
		this.id = id;
		width = height = sz8dp;
		this.posH = posH;
		this.posV = posV;
		validate();
	}

	public ButtonPositionSize setMoveVertical() {
		this.yMove = true;
		return this;
	}

	public ButtonPositionSize setMoveDescendantsAny() {
		this.moveDescendants = MOVE_DESCENDANTS_ANY;
		return this;
	}

	public ButtonPositionSize setMoveDescendantsVertical() {
		this.moveDescendants = MOVE_DESCENDANTS_VERTICAL;
		return this;
	}

	public ButtonPositionSize setMoveDescendantsHorizontal() {
		this.moveDescendants = MOVE_DESCENDANTS_HORIZONTAL;
		return this;
	}

	public ButtonPositionSize setMoveHorizontal() {
		this.xMove = true;
		return this;
	}

	public ButtonPositionSize setSize(int width8dp, int height8dp) {
		this.width = width8dp;
		this.height = height8dp;
		return this;
	}

	public ButtonPositionSize setPositionHorizontal(int posH) {
		this.posH = posH;
		validate();
		return this;
	}

	public ButtonPositionSize setPositionVertical(int posV) {
		this.posV = posV;
		validate();
		return this;
	}

	public ButtonPositionSize setMargin(int marginX, int marginY) {
		this.marginX = marginX;
		this.marginY = marginY;
		return this;
	}

	public boolean isLeft() {
		return posH == POS_LEFT;
	}

	public boolean isRight() {
		return posH == POS_RIGHT;
	}

	public boolean isTop() {
		return posV == POS_TOP;
	}

	public boolean isBottom() {
		return posV == POS_BOTTOM;
	}

	public boolean isFullWidth() {
		return posH == POS_FULL_WIDTH;
	}

	public boolean isFullHeight() {
		return posV == POS_FULL_HEIGHT;
	}

	public long toLongValue() {
		long vl = 0;
		vl = (vl << 2) + moveDescendants;
		vl = (vl << 2) + posV;
		vl = (vl << 1) + (yMove ? 1 : 0);
		vl = (vl << MAX_MARGIN_BITS) + Math.min(marginY, MARGIN_MASK);
		vl = (vl << MAX_SIZE_BITS) + Math.min(height, SIZE_MASK);
		vl = (vl << 2) + posH;
		vl = (vl << 1) + (xMove ? 1 : 0);
		vl = (vl << MAX_MARGIN_BITS) + Math.min(marginX, MARGIN_MASK);
		vl = (vl << MAX_SIZE_BITS) + Math.min(width, SIZE_MASK);
		return vl;
	}

	public void calcGridPositionFromPixel(float dpToPix, int widthPx, int heightPx,
	                                      boolean gravLeft, int x, boolean gravTop, int y) {
		float calc;
		if (x < widthPx / 2) {
			this.posH = gravLeft ? POS_LEFT : POS_RIGHT;
			calc = x / dpToPix;
		} else {
			this.posH = gravLeft ? POS_RIGHT : POS_LEFT;
			calc = (widthPx - x) / dpToPix - this.width * CELL_SIZE_DP;
		}
		this.marginX = Math.max(0, Math.round((calc - DEF_MARGIN_DP) / CELL_SIZE_DP));
		if (y < heightPx / 2) {
			this.posV = gravTop ? POS_TOP : POS_RIGHT;
			calc = y / dpToPix;
		} else {
			this.posV = gravTop ? POS_RIGHT : POS_TOP;
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

	public int getWidthPix(float dpToPix) {
		return (int) ((width * CELL_SIZE_DP) * dpToPix);
	}

	public ButtonPositionSize fromLongValue(long v) {
		width = (int) (v & SIZE_MASK);
		v = v >> MAX_SIZE_BITS;
		marginX = (int) (v & MARGIN_MASK);
		v = v >> MAX_MARGIN_BITS;
		xMove = v % 2 == 1;
		v = v >> 1;
		posH = (int) (v % 4);
		v = v >> 2;
		height = (int) (v & SIZE_MASK);
		v = v >> MAX_SIZE_BITS;
		marginY = (int) (v & MARGIN_MASK);
		v = v >> MAX_MARGIN_BITS;
		yMove = v % 2 == 1;
		v = v >> 1;
		posV = (int) (v % 4);
		v = v >> 2;
		moveDescendants = (int) (v % 4);
		v = v >> 2;
		validate();
		return this;
	}

	private void validate() {
		if (posH == POS_FULL_WIDTH && posV == POS_FULL_HEIGHT) {
			System.err.println("Error parsing " + this + " as full width + full height");
			posH = POS_RIGHT;
		}
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "Pos %10s x=(%s->%d%s), y=(%s->%d%s), w=%2d, h=%2d", id,
				posH == POS_FULL_WIDTH ? "full_w" : posH == POS_LEFT ? "left " : "right", marginX, xMove ? "+" : " ",
				posV == POS_FULL_HEIGHT ? "full_h" : posV == POS_TOP ? "top " : "bott", marginY, yMove ? "+" : " ", width, height);
	}

	public boolean overlap(ButtonPositionSize position) {
		return QuadRect.intersects(this.bounds, position.bounds);
	}

	public static boolean computeNonOverlap(int space, List<ButtonPositionSize> buttons, int totalWidth, int totalHeight) {
		int MAX_ITERATIONS = 1000, iter = 0;
		for (ButtonPositionSize button : buttons)
			button.updateBounds(totalWidth, totalHeight);

		for (int fixedPos = buttons.size() - 1; fixedPos >= 0; ) {
			if (iter++ > MAX_ITERATIONS) {
				LOG.error("Relayout is broken");
				return false;
			}
			boolean overlap = false;
			ButtonPositionSize button = buttons.get(fixedPos);
			for (int i = fixedPos + 1; i < buttons.size(); i++) {
				ButtonPositionSize check = buttons.get(i);
				if (button.overlap(check)) {
					overlap = true;
					if (moveButton(space, check, button, totalWidth, totalHeight)) {
						check.updateBounds(totalWidth, totalHeight);
					}
					fixedPos = i;
					break;
				}
			}
			if (!overlap) {
				fixedPos--;
			}
		}
		return true;
	}

	private void updateBounds(int totalWidth, int totalHeight) {
		double left, right, top, bottom;

		if (posH == POS_FULL_WIDTH) {
			left = 0;
			right = totalWidth;
		} else if (posH == POS_LEFT) {
			left = marginX;
			right = left + width;
		} else {
			right = totalWidth - marginX;
			left = right - width;
		}

		if (posV == POS_FULL_HEIGHT) {
			top = 0;
			bottom = totalHeight;
		} else if (posV == POS_TOP) {
			top = marginY;
			bottom = top + height;
		} else {
			bottom = totalHeight - marginY;
			top = bottom - height;
		}
		bounds.left = left;
		bounds.right = right;
		bounds.top = top;
		bounds.bottom = bottom;
	}

	private static boolean moveButton(int space, ButtonPositionSize toMove,
			ButtonPositionSize overlap,	int totalWidth, int totalHeight) {
		boolean xMove = false;
		boolean yMove = false;
		if (overlap.moveDescendants == MOVE_DESCENDANTS_ANY) {
			if (overlap.posH == POS_FULL_WIDTH) {
				yMove = true;
			} else if (overlap.posV == POS_FULL_HEIGHT) {
				xMove = true;
			} else {
				if (toMove.xMove) {
					xMove = toMove.xMove;
				}
				if (toMove.yMove) {
					yMove = toMove.yMove;
				}
			}
		} else if (overlap.moveDescendants == MOVE_DESCENDANTS_VERTICAL) {
			yMove = true;
		} else if (overlap.moveDescendants == MOVE_DESCENDANTS_HORIZONTAL) {
			xMove = true;
		}

		int newX = xMove ? space + overlap.marginX + overlap.width : toMove.marginX;
		int newY = yMove ? space + overlap.marginY + overlap.height : toMove.marginY;

		if (newX + toMove.width > totalWidth) {
			newX = Math.max(0, totalWidth - toMove.width);
			if (!yMove) {
				int desired = space + overlap.marginY + overlap.height;
				newY = Math.max(0, Math.min(desired, totalHeight - toMove.height));
			}
		}
		if (newY + toMove.height > totalHeight) {
			newY = Math.max(0, totalHeight - toMove.height);
			if (!xMove) {
				int desired = space + overlap.marginX + overlap.width;
				newX = Math.max(0, Math.min(desired, totalWidth - toMove.width));
			}
		}
		boolean moved = (newX != toMove.marginX) || (newY != toMove.marginY);
		toMove.marginX = newX;
		toMove.marginY = newY;

		return moved;
	}
}
