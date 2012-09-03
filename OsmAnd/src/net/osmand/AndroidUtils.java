package net.osmand;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;

public class AndroidUtils {

	/**
	 * @param context
	 * @return true if Hardware keyboard is available
	 */
	public static boolean isHardwareKeyboardAvailable(Context context) {
		return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}
	
	/**
	 * @param context
	 */
	public static void softKeyboardDelayed(final View view) {
		view.post(new Runnable() {
			@Override
			public void run() {
				if (!isHardwareKeyboardAvailable(view.getContext())) {
					InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
					}
				}
			}
		});
	}

	/**
	 * @param closeButton expand area of which element
	 * @param left multiplier of width to add to left, 1 does nothing
	 * @param top multiplier of height to add to top, 1 does nothing 
	 * @param right multiplier of width to add to right, 1 does nothing
	 * @param bottom multiplier of height to add to bottom, 1 does nothing
	 */
	public static void expandClickableArea(final View closeButton,
			final int left, final int top, final int right, final int bottom) {
		closeButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				v.post(new Runnable() {
					@Override
					public void run() {
						closeButton.performClick();
					}
				});
				return true;
			}
		});
		// increase touch area for the button
		final View parent = (View) closeButton.getParent();
		parent.post(new Runnable() {
			// Post in the parent's message queue to make sure the parent
			// lays out its children before we call getHitRect()
			@Override
			public void run() {
				Rect r = new Rect();
				closeButton.getHitRect(r);
				r.left -= r.width() * left;
				r.top -= r.height() * top;
				r.right += r.width() * right;
				r.bottom += r.height() * bottom;
				parent.setTouchDelegate(new TouchDelegate(r, closeButton));
			}
		});
	}
}
