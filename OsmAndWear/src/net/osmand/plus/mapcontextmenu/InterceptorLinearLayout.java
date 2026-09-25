package net.osmand.plus.mapcontextmenu;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

public class InterceptorLinearLayout extends LinearLayout {
	private final int mTouchSlop;
	private boolean mIsScrolling;
	private float mDownY;
	private OnTouchListener listener;

	public InterceptorLinearLayout(Context context) {
		this(context, null);
	}

	public InterceptorLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	public InterceptorLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public InterceptorLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	public int getTouchSlop() {
		return mTouchSlop;
	}

	public boolean isScrolling() {
		return mIsScrolling;
	}

	public void setListener(OnTouchListener listener) {
		this.listener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean handled = false;
		int action = ev.getAction();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mIsScrolling = false;
			handled = false;
		} else {
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					mIsScrolling = false;
					mDownY = ev.getRawY();
					handled = false;
				case MotionEvent.ACTION_MOVE:
					if (mIsScrolling) {
						handled = true;
					} else {
						int yDiff = calculateDistanceY(ev);
						if (Math.abs(yDiff) > mTouchSlop) {
							mIsScrolling = true;
							handled = true;
						}
					}
					break;
			}
		}

		if (listener != null) {
			listener.onTouch(this, ev);
		}
		return handled;
	}

	private int calculateDistanceY(MotionEvent ev) {
		return (int) (ev.getRawY() - mDownY);
	}
}
