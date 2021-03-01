package net.osmand.plus.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;

public class FlowLayout extends ViewGroup {

	private int line_height;

	public static class LayoutParams extends ViewGroup.LayoutParams {

		final int horizontal_spacing;
		final int vertical_spacing;
		final boolean isHorizontalAutoSpacing;

		/**
		 * @param horizontal_spacing Pixels between items, horizontally
		 * @param vertical_spacing   Pixels between items, vertically
		 */
		public LayoutParams(int horizontal_spacing, int vertical_spacing, boolean isHorizontalAutoSpacing) {
			super(0, 0);
			this.horizontal_spacing = horizontal_spacing;
			this.vertical_spacing = vertical_spacing;
			this.isHorizontalAutoSpacing = isHorizontalAutoSpacing;
		}
	}

	public FlowLayout(Context context) {
		super(context);
	}

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if ((MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED))
			throw new AssertionError();

		final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
		final int count = getChildCount();
		int line_height = 0;
		int horizontalPosition = getPaddingLeft();
		int verticalPosition = getPaddingTop();
		int childHeightMeasureSpec;

		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
		} else {
			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), childHeightMeasureSpec);
				final int childWidth = child.getMeasuredWidth();
				line_height = Math.max(line_height, child.getMeasuredHeight() + lp.vertical_spacing);
				if (horizontalPosition + childWidth > width) {
					horizontalPosition = getPaddingLeft();
					verticalPosition += line_height;
				}
				horizontalPosition += childWidth + lp.horizontal_spacing;
			}
		}

		this.line_height = line_height;
		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			height = verticalPosition + line_height;
		} else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
			if (verticalPosition + line_height < height) {
				height = verticalPosition + line_height;
			}
		}
		setMeasuredDimension(width, height);
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(1, 1, false); // default of 1px spacing
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		final int width = r - l;
		int freeSizeSpacing;
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(getContext());
		int horizontalPosition = isLayoutRtl ? width - getPaddingRight() : getPaddingLeft();
		int verticalPosition = getPaddingTop();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final int childWidth = child.getMeasuredWidth();
				final int childHeight = child.getMeasuredHeight();
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				int itemsCount = width / childWidth;
				if (itemsCount > 1) {
					freeSizeSpacing = width % childWidth / (itemsCount - 1);
				} else {
					freeSizeSpacing = width % childWidth / itemsCount;
				}
				if (lp.isHorizontalAutoSpacing) {
					if (isLayoutRtl) {
						if (horizontalPosition - childWidth < getPaddingLeft()) {
							horizontalPosition = width - getPaddingRight();
							verticalPosition += line_height;
						}
						child.layout(horizontalPosition - childWidth, verticalPosition, horizontalPosition, verticalPosition + childHeight);
						horizontalPosition -= childWidth + lp.horizontal_spacing + freeSizeSpacing;
					} else {
						if (horizontalPosition + childWidth > width) {
							horizontalPosition = getPaddingLeft();
							verticalPosition += line_height;
						}
						child.layout(horizontalPosition, verticalPosition, horizontalPosition + childWidth, verticalPosition + childHeight);
						horizontalPosition += childWidth + lp.horizontal_spacing + freeSizeSpacing;
					}
				} else {
					if (isLayoutRtl) {
						if (horizontalPosition - childWidth < l) {
							horizontalPosition = width - getPaddingRight();
							verticalPosition += line_height;
						}
						child.layout(horizontalPosition - childWidth, verticalPosition, horizontalPosition, verticalPosition + childHeight);
						horizontalPosition -= childWidth + lp.horizontal_spacing;
					} else {
						if (horizontalPosition + childWidth > width) {
							horizontalPosition = getPaddingLeft();
							verticalPosition += line_height;
						}
						child.layout(horizontalPosition, verticalPosition, horizontalPosition + childWidth, verticalPosition + childHeight);
						horizontalPosition += childWidth + lp.horizontal_spacing;
					}
				}
			}
		}
	}
}