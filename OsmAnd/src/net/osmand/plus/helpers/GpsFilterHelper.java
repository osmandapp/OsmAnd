package net.osmand.plus.helpers;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class GpsFilterHelper {

	private final OsmandApplication app;
	private final SelectedGpxFile selectedGpxFile;

	public final SmoothingFilter smoothingFilter;
	public final SpeedFilter speedFilter;
	public final AltitudeFilter altitudeFilter;
	public final HdopFilter hdopFilter;

	private int leftPoints;
	private int totalPoints;

	public GpsFilterHelper(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;

		this.smoothingFilter = new SmoothingFilter(app, selectedGpxFile);
		this.speedFilter = new SpeedFilter(app, selectedGpxFile);
		this.altitudeFilter = new AltitudeFilter(app, selectedGpxFile);
		this.hdopFilter = new HdopFilter(app, selectedGpxFile);
	}

	public int getLeftPoints() {
		return leftPoints;
	}

	public int getTotalPoints() {
		return totalPoints;
	}

	public abstract class GpsFilter<T extends Number> {

		protected static final int SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;

		protected final SelectedGpxFile selectedGpxFile;
		protected final GPXTrackAnalysis analysis;
		protected final Map<String, String> extensions;

		protected T selectedMinValue;
		protected T selectedMaxValue;

		protected boolean nightMode;

		protected final ForegroundColorSpan blackTextSpan;
		protected final ForegroundColorSpan greyTextSpan;
		protected final StyleSpan boldSpan;

		public GpsFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			this.selectedGpxFile = selectedGpxFile;
			this.analysis = selectedGpxFile.getTrackAnalysis(app);
			this.extensions = selectedGpxFile.getGpxFile().getExtensionsToRead();

			selectedMaxValue = parseConfigValueFromGpx(getMaxValueTag(), getMaxValue());
			if (isRangeSupported()) {
				selectedMinValue = parseConfigValueFromGpx(getMinValueTag(), getMinValue());
			}
			checkSelectedValues();

			nightMode = app.getDaynightHelper().isNightModeForMapControls();

			blackTextSpan = new ForegroundColorSpan(ColorUtilities.getPrimaryTextColor(app, nightMode));
			greyTextSpan = new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode));
			boldSpan = new StyleSpan(Typeface.BOLD);
		}

		protected T parseConfigValueFromGpx(String tag, T defaultValue) {
			if (Algorithms.isEmpty(tag)) {
				return defaultValue;
			}
			String stringValue = extensions.get(tag);
			if (stringValue == null) {
				return defaultValue;
			}
			try {
				if (defaultValue instanceof Integer) {
					return ((T) Integer.valueOf(stringValue));
				} else if (defaultValue instanceof Double) {
					return ((T) Double.valueOf(stringValue));
				}
				return defaultValue;
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}

		protected void checkSelectedValues() {
			if (isRangeSupported()) {
				if (selectedMinValue.floatValue() > selectedMaxValue.floatValue()) {
					T temp = selectedMinValue;
					selectedMinValue = selectedMaxValue;
					selectedMaxValue = temp;
				}
				if (selectedMinValue.floatValue() < getMinValue().floatValue()) {
					selectedMinValue = getMinValue();
				}
			}
			if (selectedMaxValue.floatValue() > getMaxValue().floatValue()) {
				selectedMaxValue = getMaxValue();
			}
		}

		public abstract boolean isNeeded();

		public abstract boolean isRangeSupported();

		public abstract boolean accept(@NonNull WptPt point);

		public abstract T getMinValue();

		public abstract T getMaxValue();

		@Nullable
		public String getMinValueTag() {
			return null;
		}

		public abstract String getMaxValueTag();

		public void updateValue(float maxValue) {
			// Not implemented
		}

		public void updateValues(float minValue, float maxValue) {
			// Not implemented
		}

		public final T getSelectedMinValue() {
			return isRangeSupported() ? selectedMinValue : getMinValue();
		}

		public final T getSelectedMaxValue() {
			return selectedMaxValue;
		}

		@NonNull
		public CharSequence getFormattedStyledValue(@NonNull T value) {
			String formattedValue = getFormattedValue(value);
			int spaceIndex = formattedValue.indexOf(" ");

			int endIndex = spaceIndex == -1 ? formattedValue.length() : spaceIndex;
			SpannableString spannableString = new SpannableString(formattedValue);
			spannableString.setSpan(blackTextSpan, 0, endIndex, SPAN_FLAGS);

			if (spaceIndex != -1) {
				spannableString.setSpan(greyTextSpan, spaceIndex + 1, formattedValue.length(), SPAN_FLAGS);
			}

			return spannableString;
		}

		@NonNull
		public abstract String getFormattedValue(@NonNull T value);

		@NonNull
		public abstract CharSequence getFilterTitle();

		@NonNull
		protected CharSequence styleFilterTitle(@NonNull String title, int boldEndIndex) {
			SpannableString spannableTitle = new SpannableString(title);
			spannableTitle.setSpan(blackTextSpan, 0,boldEndIndex, SPAN_FLAGS);
			spannableTitle.setSpan(boldSpan, 0, boldEndIndex, SPAN_FLAGS);
			spannableTitle.setSpan(greyTextSpan, boldEndIndex, title.length(), SPAN_FLAGS);
			return spannableTitle;
		}

		@NonNull
		public abstract String getLeftText();

		@NonNull
		public abstract String getRightText();

		@StringRes
		public abstract int getDescriptionId();
	}

	public class SmoothingFilter extends GpsFilter<Integer> {

		private static final String TAG_SMOOTHING_FILTER = "filter_min_distance_between_points";

		public SmoothingFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
			selectedMaxValue = parseConfigValueFromGpx(getMaxValueTag(), getMinValue());
		}

		@Override
		public boolean isNeeded() {
			return true;
		}

		@Override
		public boolean isRangeSupported() {
			return false;
		}

		@Override
		public boolean accept(@NonNull WptPt point) {
			return true; // todo gps
		}

		@Override
		public Integer getMinValue() {
			return 0;
		}

		@Override
		public Integer getMaxValue() {
			return 100;
		}

		@Override
		public void updateValue(float maxValue) {
			selectedMaxValue = ((int) maxValue);
		}

		@Override
		public String getMaxValueTag() {
			return TAG_SMOOTHING_FILTER;
		}

		@NonNull
		@Override
		public String getFormattedValue(@NonNull Integer value) {
			return OsmAndFormatter.getFormattedDistance(value, app);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle() {
			String smoothing = app.getString(R.string.gps_filter_smoothing);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, smoothing,
					getFormattedValue(getSelectedMaxValue()));
			return styleFilterTitle(title, smoothing.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText() {
			return app.getString(R.string.distance_between_points);
		}

		@NonNull
		@Override
		public String getRightText() {
			return getFormattedValue(getSelectedMaxValue());
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_smoothing_desc;
		}
	}

	public class SpeedFilter extends GpsFilter<Integer> {

		private static final String TAG_MIN_SPEED = "filter_min_speed";
		private static final String TAG_MAX_SPEED = "filter_max_speed";

		public SpeedFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
		}

		@Override
		public boolean isNeeded() {
			return analysis.isSpeedSpecified();
		}

		@Override
		public boolean isRangeSupported() {
			return true;
		}

		@Override
		public boolean accept(@NonNull WptPt point) {
			return false;
			// todo gps
		}

		@Override
		public Integer getMinValue() {
			return 0;
		}

		@Override
		public Integer getMaxValue() {
			return transformSpeed(analysis.maxSpeed);
		}

		@Override
		public void updateValues(float minValue, float maxValue) {
			selectedMinValue = ((int) minValue);
			selectedMaxValue = ((int) maxValue);
			checkSelectedValues();
		}

		private int transformSpeed(float metersPerSecond) {
			String speedInUnits = OsmAndFormatter.getFormattedSpeed(metersPerSecond, app, false);
			return (int) Math.ceil(Double.parseDouble(speedInUnits));
		}

		@Override
		public String getMinValueTag() {
			return TAG_MIN_SPEED;
		}

		@Override
		public String getMaxValueTag() {
			return TAG_MAX_SPEED;
		}

		@NonNull
		@Override
		public String getFormattedValue(@NonNull Integer value) {
			return OsmAndFormatter.getFormattedSpeed(value, null, false);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle() {
			String speed = app.getString(R.string.map_widget_speed);
			String value;
			if (!isNeeded()) {
				value = app.getString(R.string.gpx_logging_no_data);
			} else {
				String min = getFormattedValue(getSelectedMinValue());
				String max = getFormattedValue(getSelectedMaxValue());
				String range = app.getString(R.string.ltr_or_rtl_combine_via_dash, min, max);
				String unit = app.getSettings().SPEED_SYSTEM.get().toShortString(app);
				value = app.getString(R.string.ltr_or_rtl_combine_via_space, range, unit);
			}
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, speed, value);

			return styleFilterTitle(title, speed.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText() {
			return app.getString(R.string.shared_string_min);
		}

		@NonNull
		@Override
		public String getRightText() {
			return app.getString(R.string.shared_string_max);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_speed_desc;
		}
	}

	public class AltitudeFilter extends GpsFilter<Integer> {

		private static final String TAG_MIN_ALTITUDE = "filter_min_altitude";
		private static final String TAG_MAX_ALTITUDE = "filter_max_altitude";

		public AltitudeFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
		}

		@Override
		public boolean isNeeded() {
			return analysis.isElevationSpecified();
		}

		@Override
		public boolean isRangeSupported() {
			return true;
		}

		@Override
		public boolean accept(@NonNull WptPt point) {
			return false; // todo gps
		}

		@Override
		public Integer getMinValue() {
			return ((int) Math.floor(analysis.minElevation));
		}

		@Override
		public Integer getMaxValue() {
			return ((int) Math.ceil(analysis.maxElevation));
		}

		@Override
		public void updateValues(float minValue, float maxValue) {
			selectedMinValue = ((int) minValue);
			selectedMaxValue = ((int) maxValue);
			checkSelectedValues();
		}

		@Override
		public String getMinValueTag() {
			return TAG_MIN_ALTITUDE;
		}

		@Override
		public String getMaxValueTag() {
			return TAG_MAX_ALTITUDE;
		}

		@NonNull
		@Override
		public String getFormattedValue(@NonNull Integer value) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle() {
			String altitude = app.getString(R.string.altitude);
			String value;
			if (!isNeeded()) {
				value = app.getString(R.string.gpx_logging_no_data);
			} else {
				String minAltitude = getFormattedValue(getSelectedMinValue());
				String maxAltitude = getFormattedValue(getSelectedMaxValue());
				value = app.getString(R.string.ltr_or_rtl_combine_via_dash, minAltitude, maxAltitude);
			}
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, altitude, value);

			return styleFilterTitle(title, altitude.length());
		}

		@NonNull
		@Override
		public String getLeftText() {
			return app.getString(R.string.shared_string_min);
		}

		@NonNull
		@Override
		public String getRightText() {
			return app.getString(R.string.shared_string_max);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_altitude_desc;
		}
	}

	public class HdopFilter extends GpsFilter<Double> {

		private static final String TAG_MAX_HDOP = "filter_max_hdop";

		public HdopFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
		}

		@Override
		public boolean isNeeded() {
			return analysis.isHdopSpecified();
		}

		@Override
		public boolean isRangeSupported() {
			return false;
		}

		@Override
		public boolean accept(@NonNull WptPt point) {
			return false; // todo gps
		}

		@Override
		public Double getMinValue() {
			return 0d;
		}

		@Override
		public Double getMaxValue() {
			return analysis.maxHdop;
		}

		@Override
		public void updateValue(float maxValue) {
			selectedMaxValue = ((double) maxValue);
		}

		@Override
		public String getMaxValueTag() {
			return TAG_MAX_HDOP;
		}

		@NonNull
		@Override
		public String getFormattedValue(@NonNull Double value) {
			return String.format(app.getLocaleHelper().getPreferredLocale(), "%.1f", value);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle() {
			String gpsPrecision = app.getString(R.string.gps_filter_precision);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, gpsPrecision,
					getFormattedValue(getSelectedMaxValue().doubleValue()));
			return styleFilterTitle(title, gpsPrecision.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText() {
			return app.getString(R.string.max_hdop);
		}

		@NonNull
		@Override
		public String getRightText() {
			return getFormattedValue(getSelectedMaxValue());
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_hdop_desc;
		}
	}
}