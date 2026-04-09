package net.osmand.plus.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

import net.osmand.plus.utils.AndroidUtils;

public class SliderEx extends Slider {

	public SliderEx(@NonNull Context context) {
		super(context);
	}

	public SliderEx(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public SliderEx(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
			state = String.format(java.util.Locale.US, "val=%s, from=%s, to=%s, step=%s",
					getValue(), getValueFrom(), getValueTo(), getStepSize());
		} catch (Exception ignored) {
		}
		return String.format("SliderEx Crash | Path: [%s] | State: [%s]", path, state);
	}
}