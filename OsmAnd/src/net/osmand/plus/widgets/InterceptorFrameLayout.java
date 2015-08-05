package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by GaidamakUA on 8/5/15.
 */
public class InterceptorFrameLayout extends FrameLayout {
	public InterceptorFrameLayout(Context context) {
		super(context);
	}

	public InterceptorFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public InterceptorFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public InterceptorFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
}
