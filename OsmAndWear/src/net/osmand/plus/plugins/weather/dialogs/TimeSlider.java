package net.osmand.plus.plugins.weather.dialogs;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.plus.plugins.weather.dialogs.WeatherForecastFragment.getDefaultCalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter.TimeFormatter;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TimeSlider extends Slider {

	private static final Log log = PlatformUtil.getLog(TimeSlider.class);

	private final TimeFormatter timeShortFormatter = new TimeFormatter(Locale.getDefault(), "HH", "h a");
	private final SimpleDateFormat simpleHoursFormat = new SimpleDateFormat("K", Locale.getDefault());

	private final Paint textPaint;

	private final Calendar calendar = getDefaultCalendar();
	private final boolean twelveHoursFormat = !DateFormat.is24HourFormat(getContext());

	private final int contentPadding;
	private final boolean isLayoutRtl;

	private int halfHeight;

	public TimeSlider(@NonNull Context context) {
		this(context, null);
	}

	public TimeSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimeSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(DEFAULT);
		textPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.default_desc_text_size));
		textPaint.setColor(AndroidUtils.getColorFromAttr(context, android.R.attr.textColorSecondary));
		textPaint.setLetterSpacing(AndroidUtils.getFloatValueFromRes(context, R.dimen.description_letter_spacing));

		isLayoutRtl = AndroidUtils.isLayoutRtl(context);
		contentPadding = getResources().getDimensionPixelSize(R.dimen.content_padding);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		halfHeight = getMeasuredHeight() / 2 + getTrackHeight() / 2;
		int height = halfHeight + contentPadding + AndroidUtils.getTextHeight(textPaint);
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
		setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
				getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		drawLegend(canvas);
	}

	private void drawLegend(@NonNull Canvas canvas) {
		int hours = isLayoutRtl ? 24 : 0;
		float y = halfHeight + contentPadding;
		for (float x : getLegendCoordinates()) {
			String text = getFormattedHours(calendar, hours, twelveHoursFormat);

			Rect rect = new Rect();
			textPaint.getTextBounds(text, 0, text.length(), rect);
			float yOffset = rect.height() / 2f - ((textPaint.descent() + textPaint.ascent()) / 2);

			canvas.drawText(text, x, y + yOffset, textPaint);

			hours = isLayoutRtl ? hours - 3 : hours + 3;
		}
	}

	private String getFormattedHours(@NonNull Calendar calendar, int hours, boolean twelveHoursFormat) {
		calendar.set(Calendar.HOUR_OF_DAY, hours);
		if (twelveHoursFormat && hours != 12) {
			return simpleHoursFormat.format(calendar.getTime());
		} else {
			return timeShortFormatter.format(calendar.getTime(), twelveHoursFormat);
		}
	}

	private float[] getLegendCoordinates() {
		int count = (int) ((getValueTo() - getValueFrom()) / 3) + 1;

		float interval = getTrackWidth() / (float) (count - 1);
		float[] coordinates = new float[count];
		for (int i = 0; i < count; i++) {
			coordinates[i] = getTrackSidePadding() + i * interval;
		}
		return coordinates;
	}

	public void hideLabel(){
		setActiveThumbIndex(-1);
	}

	public void showLabel(){
		setActiveThumbIndex(0);
	}
}