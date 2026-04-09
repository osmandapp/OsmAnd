package net.osmand.plus.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.RangeSlider;

import net.osmand.plus.utils.AndroidUtils;

public class RangeSliderEx extends RangeSlider {

	public RangeSliderEx(@NonNull Context context) {
		super(context);
	}

	public RangeSliderEx(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public RangeSliderEx(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		try {
			super.onDraw(canvas);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(getDebugInfo(), e);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		try {
			return super.onTouchEvent(event);
		} catch (IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException(getDebugInfo() + " | Original Error: " + e.getMessage());
		}
	}

	@NonNull
	private String getDebugInfo() {
		String path = AndroidUtils.getViewPath(this, 2);
		String state = "Failed to read values";
		try {
			state = String.format(java.util.Locale.US, "values=%s, from=%s, to=%s, step=%s",
					getValues(), getValueFrom(), getValueTo(), getStepSize());
		} catch (Exception ignored) {
		}
		return String.format("RangeSliderEx Crash | Path: [%s] | State: [%s]", path, state);
	}
}