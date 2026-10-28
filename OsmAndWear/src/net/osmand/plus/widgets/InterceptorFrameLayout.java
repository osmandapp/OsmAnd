package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import net.osmand.plus.widgets.tools.SwipeDismissTouchListener;

public class InterceptorFrameLayout extends FrameLayout {
	private final int mTouchSlop;
	private boolean mIsScrolling;
	private float mDownX;
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
		int action = ev.getActionMasked();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mIsScrolling = false;
			return false;
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mDownX = ev.getRawX();
				if(listener != null) {
					listener.onTouch(this, ev);
				}
				return false;
			case MotionEvent.ACTION_MOVE:
				if (mIsScrolling) {
					return true;
				}

				int xDiff = calculateDistanceX(ev);
				if (Math.abs(xDiff) > mTouchSlop) {
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
