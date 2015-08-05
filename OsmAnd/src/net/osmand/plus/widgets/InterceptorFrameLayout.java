package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import net.osmand.plus.widgets.tools.SwipeDismissTouchListener;

/**
 * Created by GaidamakUA on 8/5/15.
 */
public class InterceptorFrameLayout extends FrameLayout {
	private static final String TAG = "InterceptorFrameLayout";
	private int mTouchSlop;
	private boolean mIsScrolling;
	private float mDownX;
	private boolean mShown;
	private SwipeDismissTouchListener listener;

	public InterceptorFrameLayout(Context context) {
		this(context, null);
	}

	public InterceptorFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	public InterceptorFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public InterceptorFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	public void setListener(SwipeDismissTouchListener listener) {
		this.listener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		Log.v(TAG, "onInterceptTouchEvent(" + "ev=" + ev + ")");
		final int action = MotionEventCompat.getActionMasked(ev);

		// Always handle the case of the touch gesture being complete.
		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			// Release the scroll.
			mIsScrolling = false;
			return false; // Do not intercept touch event, let the child handle it
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mShown = false;
				mDownX = ev.getRawX();
				listener.onTouch(this, ev);
				return false;
			case MotionEvent.ACTION_MOVE:
				if (mIsScrolling) {
					return true;
				}

				final int xDiff = calculateDistanceX(ev);
				if (xDiff > mTouchSlop) {
					mIsScrolling = true;
					return true;
				}
				break;
		}

		return false;
	}

	private int calculateDistanceX(MotionEvent ev) {
		return (int) (ev.getRawX() - mDownX);
	}
}
