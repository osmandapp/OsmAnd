package net.osmand.plus;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LockableViewPager extends ViewPager {
	private boolean swipeLocked;

	public LockableViewPager(Context context) {
		super(context);
	}

	public LockableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean getSwipeLocked() {
		return swipeLocked;
	}

	public void setSwipeLocked(boolean swipeLocked) {
		this.swipeLocked = swipeLocked;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return !swipeLocked && super.onTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return !swipeLocked && super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean canScrollHorizontally(int direction) {
		return !swipeLocked && super.canScrollHorizontally(direction);
	}
}
