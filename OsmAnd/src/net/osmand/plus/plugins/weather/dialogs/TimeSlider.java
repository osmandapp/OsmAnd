package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.utils.OsmAndFormatter.TimeFormatter;

import org.apache.commons.logging.Log;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TimeSlider extends Slider {

	private static final Log log = PlatformUtil.getLog(TimeSlider.class);

	private final TimeFormatter timeShortFormatter = new TimeFormatter(Locale.getDefault(), "HH", "h a");
	private final SimpleDateFormat simpleHoursFormat = new SimpleDateFormat("K", Locale.getDefault());

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

	public float[] getTicksCoordinates() {
		try {
			Field[] fields = getClass().getSuperclass().getSuperclass().getDeclaredFields();
			for (Field field : fields) {
				if ("ticksCoordinates".equals(field.getName())) {
					field.setAccessible(true);
					return (float[]) field.get(this);
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
}
