package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

public class TimeSlider extends Slider {

	public TimeSlider(@NonNull Context context) {
		super(context);
	}

	public TimeSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public TimeSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
	}
}
