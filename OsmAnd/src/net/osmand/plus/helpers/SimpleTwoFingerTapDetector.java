package net.osmand.plus.helpers;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by Barsik on 24.06.2014.
 */
public abstract class SimpleTwoFingerTapDetector {
	private static final int TIMEOUT = ViewConfiguration.getTapTimeout() + 100;
	private long mFirstDownTime = 0;
	private byte mTwoFingerTapCount = 0;
	private MotionEvent firstEvent = null;

	private void reset(long time) {
		mFirstDownTime = time;
		mTwoFingerTapCount = 0;
	}

	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				if (mFirstDownTime == 0 || event.getEventTime() - mFirstDownTime > TIMEOUT){
					reset(event.getDownTime());
				}
				break;
			case MotionEvent.ACTION_POINTER_UP:
				if (event.getPointerCount() == 2) {
					mTwoFingerTapCount++;
					firstEvent = MotionEvent.obtain(event);
				}
				else{
					mFirstDownTime = 0;
					firstEvent = null;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTwoFingerTapCount == 1 && event.getEventTime() - mFirstDownTime < TIMEOUT) {
					onTwoFingerTap(firstEvent, event);
					mFirstDownTime = 0;
					firstEvent = null;
					return true;
				}
		}

		return false;
	}

	public abstract void onTwoFingerTap(MotionEvent firstevent, MotionEvent secondevent);
}
