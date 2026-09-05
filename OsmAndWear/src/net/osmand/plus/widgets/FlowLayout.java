package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class FlowLayout extends ViewGroup {

	private int line_height;
	private boolean horizontalAutoSpacing;
	private boolean alignToCenter = false;
	private final int defaultHorizontalSpacing;
	private final int defaultVerticalSpacing;

	public FlowLayout(Context context) {
		this(context, null);
	}

	public FlowLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.FlowLayout,
				defStyleAttr,
				0
		);
		defaultHorizontalSpacing = (int) a.getDimension(R.styleable.FlowLayout_horizontalItemsSpacing, 1);
		defaultVerticalSpacing = (int) a.getDimension(R.styleable.FlowLayout_verticalItemsSpacing, 1);
	}

	// If true, available horizontal space is added to items horizontalSpacing to fit the screen width.
	public void setHorizontalAutoSpacing(boolean horizontalAutoSpacing) {
		this.horizontalAutoSpacing = horizontalAutoSpacing;
	}

	public void setAlignToCenter(boolean alignToCenter) {
		this.alignToCenter = alignToCenter;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if ((MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED))
			throw new AssertionError();

		int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
		int count = getChildCount();
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
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), childHeightMeasureSpec);
				int childWidth = child.getMeasuredWidth();
				line_height = Math.max(line_height, child.getMeasuredHeight() + getItemVerticalSpacing(child));
				if (horizontalPosition + childWidth > width) {
					horizontalPosition = getPaddingLeft();
					verticalPosition += line_height;
				}
				horizontalPosition += childWidth + getItemHorizontalSpacing(child);
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
		return new LayoutParams(1, 1); // default of 1px spacing
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		int width = r - l;
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(getContext());
		int horizontalPosition;
		if (alignToCenter) {
			horizontalPosition = getCenteredHorizontalStartPosition(width, 0, isLayoutRtl);
		} else {
			horizontalPosition = isLayoutRtl ? width - getPaddingRight() : getPaddingLeft();
		}
		int verticalPosition = getPaddingTop();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				int childWidth = child.getMeasuredWidth();
				int childHeight = child.getMeasuredHeight();
				int freeSizeSpacing = getFreeSizeSpacing(width, childWidth, getItemHorizontalSpacing(child));
				if (isLayoutRtl) {
					if (horizontalPosition - childWidth < getPaddingLeft()) {
						horizontalPosition = alignToCenter
								? getCenteredHorizontalStartPosition(width, i, isLayoutRtl)
								: width - getPaddingRight();
						verticalPosition += line_height;
					}
					child.layout(horizontalPosition - childWidth, verticalPosition, horizontalPosition, verticalPosition + childHeight);
					horizontalPosition -= freeSizeSpacing;
				} else {
					if (horizontalPosition + childWidth > width) {
						horizontalPosition = alignToCenter
								? getCenteredHorizontalStartPosition(width, i, isLayoutRtl)
								: getPaddingLeft();
						verticalPosition += line_height;
					}
					child.layout(horizontalPosition, verticalPosition, horizontalPosition + childWidth, verticalPosition + childHeight);
					horizontalPosition += freeSizeSpacing;
				}
			}
		}
	}

	private int getCenteredHorizontalStartPosition(int width, int startChildIndex, boolean isLayoutRtl) {
		int startPosition = isLayoutRtl ? width - getPaddingRight() : getPaddingLeft();
		int filledSpace = 0;
		int horizontalCenter = (width / 2);
		for (int i = startChildIndex; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				filledSpace += child.getMeasuredWidth();
				if (i != startChildIndex) {
					filledSpace += getItemHorizontalSpacing(child);
				}
				if (filledSpace > width - (getPaddingLeft() + getPaddingRight())) {
					return startPosition;
				}

				startPosition = isLayoutRtl ? horizontalCenter + (filledSpace / 2) : horizontalCenter - (filledSpace / 2);
			}
		}
		return startPosition;
	}

	private int getFreeSizeSpacing(int width, int childWidth, int horizontalSpacing) {
		int childWithSpacing = childWidth + horizontalSpacing;
		int itemsCount = width / childWithSpacing;
		if (width % childWithSpacing >= childWidth) {
			itemsCount++;
		}
		int freeSizeSpacing;
		if (itemsCount > 1 && horizontalAutoSpacing) {
			freeSizeSpacing = (width - childWidth) / (itemsCount - 1);
		} else if (!horizontalAutoSpacing) {
			freeSizeSpacing = childWithSpacing;
		} else {
			freeSizeSpacing = (width % childWidth / itemsCount);
		}
		return freeSizeSpacing;
	}

	private int getItemHorizontalSpacing(View view) {
		if (view.getLayoutParams() instanceof LayoutParams) {
			return ((LayoutParams) view.getLayoutParams()).horizontalSpacing;
		}
		return defaultHorizontalSpacing;
	}

	private int getItemVerticalSpacing(View view) {
		if (view.getLayoutParams() instanceof LayoutParams) {
			return ((LayoutParams) view.getLayoutParams()).verticalSpacing;
		}
		return defaultVerticalSpacing;
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {

		final int horizontalSpacing;
		final int verticalSpacing;

		/**
		 * @param horizontalSpacing Pixels between items, horizontally
		 * @param verticalSpacing   Pixels between items, vertically
		 */
		public LayoutParams(int horizontalSpacing, int verticalSpacing) {
			super(0, 0);
			this.horizontalSpacing = horizontalSpacing;
			this.verticalSpacing = verticalSpacing;
		}
	}
}