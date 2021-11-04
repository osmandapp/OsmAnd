package net.osmand.plus.helpers;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class GpsFilterHelper {

	private final OsmandApplication app;

	private SelectedGpxFile sourceSelectedGpxFile;
	private final SelectedGpxFile filteredSelectedGpxFile = new SelectedGpxFile();

	private final Set<GpsFilterListener> listeners = new HashSet<>();

	private SmoothingFilter smoothingFilter;
	private SpeedFilter speedFilter;
	private AltitudeFilter altitudeFilter;
	private HdopFilter hdopFilter;

	private int leftPoints;
	private int totalPoints;

	private String lastSavedFilePath;

	public GpsFilterHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.filteredSelectedGpxFile.filtered = true;
	}

	public void setSelectedGpxFile(@NonNull SelectedGpxFile selectedGpxFile) {
		sourceSelectedGpxFile = selectedGpxFile;
		filteredSelectedGpxFile.setJoinSegments(sourceSelectedGpxFile.isJoinSegments());

		lastSavedFilePath = null;

		smoothingFilter = new SmoothingFilter(app, selectedGpxFile);
		speedFilter = new SpeedFilter(app, selectedGpxFile);
		altitudeFilter = new AltitudeFilter(app, selectedGpxFile);
		hdopFilter = new HdopFilter(app, selectedGpxFile);

		filterGpxFile();
	}

	@Nullable
	public SelectedGpxFile getSourceSelectedGpxFile() {
		return sourceSelectedGpxFile;
	}

	@NonNull
	public SelectedGpxFile getFilteredSelectedGpxFile() {
		return filteredSelectedGpxFile;
	}

	public boolean isEnabled() {
		return sourceSelectedGpxFile != null;
	}

	public boolean isSourceOfFilteredGpxFile(@NonNull SelectedGpxFile selectedGpxFile) {
		return selectedGpxFile.equals(sourceSelectedGpxFile);
	}

	public void onSavedFile(String path) {
		lastSavedFilePath = path;
	}

	public String getLastSavedFilePath() {
		return lastSavedFilePath;
	}

	public void disableFilter() {
		GPXFile sourceGpxFile = sourceSelectedGpxFile.getGpxFile();
		if (Algorithms.isEmpty(sourceGpxFile.path)) {
			app.getSelectedGpxHelper().selectGpxFile(sourceGpxFile, false, false);
		}
		sourceSelectedGpxFile = null;

		filteredSelectedGpxFile.getModifiablePointsToDisplay().clear();
		GPXFile filteredGpxFile = filteredSelectedGpxFile.getGpxFile();
		if (filteredGpxFile != null) {
			filteredGpxFile.tracks.clear();
		}

		listeners.clear();
	}

	public List<SelectedGpxFile> replaceWithFilteredTrack(@NonNull List<SelectedGpxFile> selectedGpxFiles) {
		if (filteredSelectedGpxFile.getGpxFile() == null) {
			return selectedGpxFiles;
		} else if (selectedGpxFiles.size() == 0) {
			return Collections.singletonList(filteredSelectedGpxFile);
		}

		List<SelectedGpxFile> tempList = new ArrayList<>(selectedGpxFiles.size());
		for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
			if (isSourceOfFilteredGpxFile(selectedGpxFile)) {
				tempList.add(filteredSelectedGpxFile);
			} else {
				tempList.add(selectedGpxFile);
			}
		}
		return tempList;
	}

	public void addListener(@NonNull GpsFilterListener listener) {
		listeners.add(listener);
	}

	public void resetFilters() {
		smoothingFilter.reset();
		speedFilter.reset();
		altitudeFilter.reset();
		hdopFilter.reset();
		filterGpxFile();

		for (GpsFilterListener listener : listeners) {
			listener.onFiltersReset();
		}
	}

	public void filterGpxFile() {
		GPXFile filteredGpxFile = copyGpxFile(app, sourceSelectedGpxFile.getGpxFile());
		filteredGpxFile.tracks.clear();

		leftPoints = 0;
		totalPoints = 0;

		GPXFile sourceGpxFile = sourceSelectedGpxFile.getGpxFile();
		for (Track track : sourceGpxFile.tracks) {

			Track filteredTrack = copyTrack(track);

			for (TrkSegment segment : track.segments) {

				if (segment.generalSegment) {
					continue;
				}

				TrkSegment filteredSegment = copySegment(segment);

				double previousPointDistance = 0;
				List<WptPt> points = segment.points;

				for (int i = 0; i < points.size(); i++) {
					WptPt point = points.get(i);

					double cumulativeDistance = point.distance - previousPointDistance;
					boolean firstOrLast = i == 0 || i + 1 == points.size();

					if (acceptPoint(point, totalPoints, cumulativeDistance, firstOrLast)) {
						filteredSegment.points.add(new WptPt(point));
						leftPoints++;
						previousPointDistance = point.distance;
					}

					totalPoints++;
				}

				if (filteredSegment.points.size() != 0) {
					filteredTrack.segments.add(filteredSegment);
				}
			}

			if (filteredTrack.segments.size() != 0) {
				filteredGpxFile.tracks.add(filteredTrack);
			}
		}

		if (filteredSelectedGpxFile.isJoinSegments()) {
			filteredGpxFile.addGeneralTrack();
		}

		filteredSelectedGpxFile.setGpxFile(filteredGpxFile, app);

		for (GpsFilterListener listener : listeners) {
			listener.onFinishFiltering();
		}
	}

	private Track copyTrack(Track source) {
		Track copy = new Track();
		copy.name = source.name;
		copy.desc = source.desc;
		return copy;
	}

	private TrkSegment copySegment(TrkSegment source) {
		TrkSegment copy = new TrkSegment();
		copy.name = source.name;
		copy.routeSegments = source.routeSegments;
		copy.routeTypes = source.routeTypes;
		return copy;
	}

	private boolean acceptPoint(WptPt point, int pointIndex, double distance, boolean firstOrLast) {
		return speedFilter.acceptPoint(point, pointIndex, distance)
				&& altitudeFilter.acceptPoint(point, pointIndex, distance)
				&& hdopFilter.acceptPoint(point, pointIndex, distance)
				&& (firstOrLast || smoothingFilter.acceptPoint(point, pointIndex, distance));
	}

	public int getLeftPoints() {
		return leftPoints;
	}

	public int getTotalPoints() {
		return totalPoints;
	}

	public SmoothingFilter getSmoothingFilter() {
		return smoothingFilter;
	}

	public SpeedFilter getSpeedFilter() {
		return speedFilter;
	}

	public AltitudeFilter getAltitudeFilter() {
		return altitudeFilter;
	}

	public HdopFilter getHdopFilter() {
		return hdopFilter;
	}

	public static abstract class GpsFilter<T extends Number> {

		protected static final int SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;

		protected final GPXTrackAnalysis analysis;

		protected T selectedMinValue;
		protected T selectedMaxValue;

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

		public abstract boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint);

		public abstract T getMinValue();

		public abstract T getMaxValue();

		public void updateValue(float maxValue) {
			// Not implemented
		}

		public void updateValues(float minValue, float maxValue) {
			// Not implemented
		}

		public void reset() {
			if (isRangeSupported()) {
				updateValues(getMinValue().floatValue(), getMaxValue().floatValue());
			} else {
				updateValue(getMaxValue().floatValue());
			}
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
		public void reset() {
			updateValue(getMinValue());
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
			float speed = transformSpeed(analysis.speedData.get(pointIndex).speed);
			return !isNeeded() || getSelectedMinValue() <= speed && speed <= getSelectedMaxValue();
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
			return R.string.gps_filter_speed_altitude_desc;
		}
	}

	public class AltitudeFilter extends GpsFilter<Integer> {

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
			return R.string.gps_filter_speed_altitude_desc;
		}
	}

	public class HdopFilter extends GpsFilter<Double> {

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

		@NonNull
		@Override
		public String getFormattedValue(@NonNull Double value) {
			return String.format(app.getLocaleHelper().getPreferredLocale(), "%.1f", value);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle() {
			String gpsPrecision = app.getString(R.string.gps_filter_precision);
			String value = isNeeded()
					? getFormattedValue(getSelectedMaxValue())
					: app.getString(R.string.gpx_logging_no_data);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, gpsPrecision, value);
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

	public static GPXFile copyGpxFile(OsmandApplication app, GPXFile source) {
		GPXFile copy = new GPXFile(Version.getFullVersion(app));
		copy.author = source.author;
		copy.metadata = source.metadata;
		copy.tracks = new ArrayList<>(source.tracks);
		copy.addPoints(source.getPoints());
		copy.routes = new ArrayList<>(source.routes);
		copy.path = source.path;
		copy.hasAltitude = source.hasAltitude;
		copy.modifiedTime = System.currentTimeMillis();
		return copy;
	}

	public interface GpsFilterListener {

		void onFinishFiltering();

		void onFiltersReset();

	}
}