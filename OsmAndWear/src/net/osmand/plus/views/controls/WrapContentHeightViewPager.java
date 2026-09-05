package net.osmand.plus.views.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class WrapContentHeightViewPager extends ViewPager {

	private boolean swipeable = true;
	private int height;
	private int decorHeight;
	private int widthMeasuredSpec;

	private boolean animateHeight;
	private int rightHeight;
	private int leftHeight;
	private int scrollingPosition = -1;

	public interface ViewAtPositionInterface {

		View getViewAtPosition(int position);
	}

	public WrapContentHeightViewPager(Context context) {
		super(context);
		init();
	}

	public WrapContentHeightViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		addOnPageChangeListener(new OnPageChangeListener() {

			int state;

			@Override
			public void onPageScrolled(int position, float offset, int positionOffsetPixels) {}

			@Override
			public void onPageSelected(int position) {
				if (state == SCROLL_STATE_IDLE) {
					height = 0;
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				this.state = state;
			}
		});
	}

	@Override
	public void setAdapter(PagerAdapter adapter) {
		if (!(adapter instanceof ViewAtPositionInterface)) {
			throw new IllegalArgumentException("WrapContentHeightViewPager requires that PagerAdapter will implement ViewAtPositionInterface");
		}
		height = 0;
		super.setAdapter(adapter);
	}

	/**
	 * Allows to redraw the view size to wrap the content of the bigger child.
	 *
	 * @param widthMeasureSpec  with measured
	 * @param heightMeasureSpec height measured
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		widthMeasuredSpec = widthMeasureSpec;
		int mode = MeasureSpec.getMode(heightMeasureSpec);

		if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
			if (height == 0) {
				// measure vertical decor (i.e. PagerTitleStrip) based on ViewPager implementation
				decorHeight = 0;
				for (int i = 0; i < getChildCount(); i++) {
					View child = getChildAt(i);
					LayoutParams lp = (LayoutParams) child.getLayoutParams();
					if (lp != null && lp.isDecor) {
						int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
						boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
						if (consumeVertical) {
							decorHeight += child.getMeasuredHeight() ;
						}
					}
				}

				// make sure that we have an height (not sure if this is necessary because it seems that onPageScrolled is called right after
				int position = getCurrentItem();
				View child = getViewAtPosition(position);
				if (child != null) {
					height = measureViewHeight(child);
				}

			}
			int totalHeight = height + decorHeight + getPaddingBottom() + getPaddingTop();
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY);
		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public void onPageScrolled(int position, float offset, int positionOffsetPixels) {
		super.onPageScrolled(position, offset, positionOffsetPixels);
		// cache scrolled view heights
		if (scrollingPosition != position) {
			scrollingPosition = position;
			// scrolled position is always the left scrolled page
			View leftView = getViewAtPosition(position);
			View rightView = getViewAtPosition(position + 1);
			if (leftView != null && rightView != null) {
				leftHeight = measureViewHeight(leftView);
				rightHeight = measureViewHeight(rightView);
				animateHeight = true;
			} else {
				animateHeight = false;
			}
		}
		if (animateHeight) {
			int newHeight = (int) (leftHeight * (1 - offset) + rightHeight * (offset));
			if (height != newHeight) {
				height = newHeight;
				requestLayout();
				invalidate();
			}
		}
	}

	private int measureViewHeight(View view) {
		view.measure(getChildMeasureSpec(widthMeasuredSpec, getPaddingLeft() + getPaddingRight(), view.getLayoutParams().width), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		return view.getMeasuredHeight();
	}

	protected View getViewAtPosition(int position) {
		if (getAdapter() != null) {
			Object objectAtPosition = ((ViewAtPositionInterface) getAdapter()).getViewAtPosition(position);
			if (objectAtPosition != null) {
				for (int i = 0; i < getChildCount(); i++) {
					View child = getChildAt(i);
					if (child != null && getAdapter().isViewFromObject(child, objectAtPosition)) {
						return child;
					}
				}
			}
		}
		return null;
	}

	public boolean isSwipeable() {
		return swipeable;
	}

	public void setSwipeable(boolean swipeable) {
		this.swipeable = swipeable;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return swipeable && super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return swipeable && super.onTouchEvent(event);
	}
}