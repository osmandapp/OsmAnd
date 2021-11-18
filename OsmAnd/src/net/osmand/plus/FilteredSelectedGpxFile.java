package net.osmand.plus;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.helpers.GpsFilterHelper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class FilteredSelectedGpxFile extends SelectedGpxFile {

	@NonNull
	private final SelectedGpxFile sourceSelectedGpxFile;

	private final int totalPointsCount;
	private int leftPointsCount;

	@NonNull
	private final SmoothingFilter smoothingFilter;
	@NonNull
	private final SpeedFilter speedFilter;
	@NonNull
	private final AltitudeFilter altitudeFilter;
	@NonNull
	private final HdopFilter hdopFilter;

	public FilteredSelectedGpxFile(@NonNull OsmandApplication app,
	                               @NonNull SelectedGpxFile sourceSelectedGpxFile) {
		this.sourceSelectedGpxFile = sourceSelectedGpxFile;
		this.joinSegments = sourceSelectedGpxFile.joinSegments;
		this.hiddenGroups = sourceSelectedGpxFile.getHiddenGroups();

		setGpxFile(GpsFilterHelper.copyGpxFile(app, sourceSelectedGpxFile.gpxFile), app);
		if (joinSegments) {
			gpxFile.addGeneralTrack();
		}
		totalPointsCount = leftPointsCount;

		smoothingFilter = new SmoothingFilter(app, sourceSelectedGpxFile);
		speedFilter = new SpeedFilter(app, sourceSelectedGpxFile);
		altitudeFilter = new AltitudeFilter(app, sourceSelectedGpxFile);
		hdopFilter = new HdopFilter(app, sourceSelectedGpxFile);
	}

	@Override
	public void setGpxFile(@NonNull GPXFile gpxFile, @NonNull OsmandApplication app) {
		super.setGpxFile(gpxFile, app);
		leftPointsCount = calculateLeftPointsCount();
	}

	public void updateGpxFile(@NonNull GPXFile gpxFile) {
		this.gpxFile = gpxFile;
		if (gpxFile.tracks.size() > 0) {
			color = gpxFile.tracks.get(0).getColor(0);
		}
		modifiedTime = gpxFile.modifiedTime;
		processedPointsToDisplay = gpxFile.proccessPoints();
		leftPointsCount = calculateLeftPointsCount();
	}

	public void setTrackAnalysis(@NonNull GPXTrackAnalysis trackAnalysis) {
		this.trackAnalysis = trackAnalysis;
	}

	public void setDisplayGroups(@NonNull List<GpxDisplayGroup> displayGroups) {
		this.displayGroups = displayGroups;
		this.splitProcessed = true;
	}

	private int calculateLeftPointsCount() {
		int count = 0;
		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		for (TrkSegment segment : segments) {
			count += segment.points.size();
		}
		return count;
	}

	public void resetFilters(@NonNull OsmandApplication app) {
		smoothingFilter.reset();
		speedFilter.reset();
		altitudeFilter.reset();
		hdopFilter.reset();
		app.getGpsFilterHelper().filterGpxFile(this);
	}

	@Override
	protected boolean processSplit(@Nullable OsmandApplication app) {
		return GpxSelectionHelper.processSplit(app, this);
	}

	@Override
	public List<TrkSegment> getPointsToDisplay() {
		return joinSegments && gpxFile != null && gpxFile.getGeneralTrack() != null
				? gpxFile.getGeneralTrack().segments
				: processedPointsToDisplay;
	}

	@NonNull
	public SelectedGpxFile getSourceSelectedGpxFile() {
		return sourceSelectedGpxFile;
	}

	public int getTotalPointsCount() {
		return totalPointsCount;
	}

	public int getLeftPointsCount() {
		return leftPointsCount;
	}

	@NonNull
	public SmoothingFilter getSmoothingFilter() {
		return smoothingFilter;
	}

	@NonNull
	public SpeedFilter getSpeedFilter() {
		return speedFilter;
	}

	@NonNull
	public AltitudeFilter getAltitudeFilter() {
		return altitudeFilter;
	}

	@NonNull
	public HdopFilter getHdopFilter() {
		return hdopFilter;
	}

	public static abstract class GpsFilter {

		protected static final int SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;

		protected final GPXTrackAnalysis analysis;

		protected double selectedMinValue;
		protected double selectedMaxValue;

		protected boolean nightMode;

		protected final ForegroundColorSpan blackTextSpan;
		protected final ForegroundColorSpan greyTextSpan;
		protected final StyleSpan boldSpan;

		public GpsFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			this.analysis = selectedGpxFile.getTrackAnalysis(app);

			this.selectedMaxValue = getMaxValue();
			if (isRangeSupported()) {
				this.selectedMinValue = getMinValue();
			}

			nightMode = app.getDaynightHelper().isNightModeForMapControls();

			blackTextSpan = new ForegroundColorSpan(ColorUtilities.getPrimaryTextColor(app, nightMode));
			greyTextSpan = new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode));
			boldSpan = new StyleSpan(Typeface.BOLD);
		}

		protected void checkSelectedValues() {
			if (isRangeSupported()) {
				if (selectedMinValue > selectedMaxValue) {
					double temp = selectedMinValue;
					selectedMinValue = selectedMaxValue;
					selectedMaxValue = temp;
				}
				if (selectedMinValue < getMinValue()) {
					selectedMinValue = getMinValue();
				}
			}
			if (selectedMaxValue > getMaxValue()) {
				selectedMaxValue = getMaxValue();
			}
		}

		public abstract boolean isNeeded();

		public abstract boolean isRangeSupported();

		public abstract boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint);

		public abstract double getMinValue();

		public abstract double getMaxValue();

		public void updateValue(double maxValue) {
			// Not implemented
		}

		public void updateValues(double minValue, double maxValue) {
			// Not implemented
		}

		public void reset() {
			if (isRangeSupported()) {
				updateValues(getMinValue(), getMaxValue());
			} else {
				updateValue(getMaxValue());
			}
		}

		public final double getSelectedMinValue() {
			return isRangeSupported() ? selectedMinValue : getMinValue();
		}

		public final double getSelectedMaxValue() {
			return selectedMaxValue;
		}

		@NonNull
		public CharSequence getFormattedStyledValue(@NonNull OsmandApplication app, double value) {
			String formattedValue = getFormattedValue(value, app);
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
		public abstract String getFormattedValue(double value, @NonNull OsmandApplication app);

		@NonNull
		public abstract CharSequence getFilterTitle(@NonNull OsmandApplication app);

		@NonNull
		protected CharSequence styleFilterTitle(@NonNull String title, int boldEndIndex) {
			SpannableString spannableTitle = new SpannableString(title);
			spannableTitle.setSpan(blackTextSpan, 0,boldEndIndex, SPAN_FLAGS);
			spannableTitle.setSpan(boldSpan, 0, boldEndIndex, SPAN_FLAGS);
			spannableTitle.setSpan(greyTextSpan, boldEndIndex, title.length(), SPAN_FLAGS);
			return spannableTitle;
		}

		@NonNull
		public abstract String getLeftText(@NonNull OsmandApplication app);

		@NonNull
		public abstract String getRightText(@NonNull OsmandApplication app);

		@StringRes
		public abstract int getDescriptionId();
	}

	public static class SmoothingFilter extends GpsFilter {

		public SmoothingFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
			selectedMaxValue = getMinValue();
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
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			return !isNeeded() || getSelectedMaxValue() == 0 || distanceToLastSurvivedPoint > getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return 0;
		}

		@Override
		public double getMaxValue() {
			return 100;
		}

		@Override
		public void updateValue(double maxValue) {
			selectedMaxValue = ((int) maxValue);
		}

		@Override
		public void reset() {
			updateValue(getMinValue());
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return OsmAndFormatter.getFormattedDistance((float) value, app);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String smoothing = app.getString(R.string.gps_filter_smoothing);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, smoothing,
					getFormattedValue(getSelectedMaxValue(), app));
			return styleFilterTitle(title, smoothing.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.distance_between_points);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return getFormattedValue(getSelectedMaxValue(), app);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_smoothing_desc;
		}
	}

	public static class SpeedFilter extends GpsFilter {

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
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			float speed = analysis.speedData.get(pointIndex).speed;
			return !isNeeded() || getSelectedMinValue() <= speed && speed <= getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return 0d;
		}

		@Override
		public double getMaxValue() {
			return Math.ceil(analysis.maxSpeed);
		}

		@Override
		public void updateValues(double minValue, double maxValue) {
			selectedMinValue = minValue;
			selectedMaxValue = maxValue;
			checkSelectedValues();
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return OsmAndFormatter.getFormattedSpeed((float) value, app, true);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String speed = app.getString(R.string.map_widget_speed);
			String value;
			if (!isNeeded()) {
				value = app.getString(R.string.gpx_logging_no_data);
			} else {
				String min = OsmAndFormatter.getFormattedSpeed((float) getSelectedMinValue(), app, false);
				String max = OsmAndFormatter.getFormattedSpeed((float) getSelectedMaxValue(), app, false);
				String range = app.getString(R.string.ltr_or_rtl_combine_via_dash, min, max);
				String unit = app.getSettings().SPEED_SYSTEM.get().toShortString(app);
				value = app.getString(R.string.ltr_or_rtl_combine_via_space, range, unit);
			}
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, speed, value);

			return styleFilterTitle(title, speed.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_min);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_max);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_speed_altitude_desc;
		}
	}

	public static class AltitudeFilter extends GpsFilter {

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
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			float altitude = analysis.elevationData.get(pointIndex).elevation;
			return !isNeeded() || getSelectedMinValue() <= altitude && altitude <= getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return ((int) Math.floor(analysis.minElevation));
		}

		@Override
		public double getMaxValue() {
			return ((int) Math.ceil(analysis.maxElevation));
		}

		@Override
		public void updateValues(double minValue, double maxValue) {
			selectedMinValue = ((int) minValue);
			selectedMaxValue = ((int) maxValue);
			checkSelectedValues();
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String altitude = app.getString(R.string.altitude);
			String value;
			if (!isNeeded()) {
				value = app.getString(R.string.gpx_logging_no_data);
			} else {
				String minAltitude = getFormattedValue(getSelectedMinValue(), app);
				String maxAltitude = getFormattedValue(getSelectedMaxValue(), app);
				value = app.getString(R.string.ltr_or_rtl_combine_via_dash, minAltitude, maxAltitude);
			}
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, altitude, value);

			return styleFilterTitle(title, altitude.length());
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_min);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_max);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_speed_altitude_desc;
		}
	}

	public static class HdopFilter extends GpsFilter {

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
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			return !isNeeded() || point.hdop <= getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return 0d;
		}

		@Override
		public double getMaxValue() {
			return analysis.maxHdop;
		}

		@Override
		public void updateValue(double maxValue) {
			selectedMaxValue = maxValue;
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return String.format(app.getLocaleHelper().getPreferredLocale(), "%.1f", value);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String gpsPrecision = app.getString(R.string.gps_filter_precision);
			String value = isNeeded()
					? getFormattedValue(getSelectedMaxValue(), app)
					: app.getString(R.string.gpx_logging_no_data);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, gpsPrecision, value);
			return styleFilterTitle(title, gpsPrecision.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.max_hdop);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return getFormattedValue(getSelectedMaxValue(), app);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_hdop_desc;
		}
	}
}