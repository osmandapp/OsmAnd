package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

import java.util.Calendar;

public class TimeSlider extends Slider {

	private Calendar currentDate;

	public TimeSlider(@NonNull Context context) {
		this(context, null);
	}

	public TimeSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimeSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setCurrentDate(@Nullable Calendar currentDate) {
		this.currentDate = currentDate;
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
	}
}
