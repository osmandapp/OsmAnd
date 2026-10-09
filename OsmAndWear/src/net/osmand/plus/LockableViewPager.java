package net.osmand.plus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class LockableViewPager extends ViewPager {
	private static final int POS_Y_UNLOCKED = -1;
	private static final int POS_Y_LOCKED = Integer.MAX_VALUE;
	private int swipeLockedPosY = POS_Y_UNLOCKED;

	public LockableViewPager(Context context) {
		super(context);
	}

	public LockableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean getSwipeLocked() {
		return swipeLockedPosY != POS_Y_UNLOCKED;
	}

	public void setSwipeLocked(boolean swipeLocked) {
		this.swipeLockedPosY = swipeLocked ? POS_Y_LOCKED : POS_Y_UNLOCKED;
	}

	public void setSwipeLockedPosY(int posY) {
		this.swipeLockedPosY = posY;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return (swipeLockedPosY == POS_Y_UNLOCKED || swipeLockedPosY < event.getY()) && super.onTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return (swipeLockedPosY == POS_Y_UNLOCKED || swipeLockedPosY < event.getY()) && super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean canScrollHorizontally(int direction) {
		return swipeLockedPosY != POS_Y_LOCKED && super.canScrollHorizontally(direction);
	}
}
