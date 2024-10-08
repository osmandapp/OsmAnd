package net.osmand.plus.track.helpers;

import static net.osmand.shared.gpx.GpxParameter.MAX_FILTER_ALTITUDE;
import static net.osmand.shared.gpx.GpxParameter.MAX_FILTER_HDOP;
import static net.osmand.shared.gpx.GpxParameter.MAX_FILTER_SPEED;
import static net.osmand.shared.gpx.GpxParameter.MIN_FILTER_ALTITUDE;
import static net.osmand.shared.gpx.GpxParameter.MIN_FILTER_SPEED;
import static net.osmand.shared.gpx.GpxParameter.SMOOTHING_THRESHOLD;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GpsFilterHelper.AltitudeFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.HdopFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SmoothingFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SpeedFilter;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.io.KFile;

import java.util.List;

public class FilteredSelectedGpxFile extends SelectedGpxFile {

	@NonNull
	private final SelectedGpxFile sourceSelectedGpxFile;

	private int totalPointsCount;
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
	                               @NonNull SelectedGpxFile sourceSelectedGpxFile,
	                               @Nullable GpxDataItem dataItem) {
		this.sourceSelectedGpxFile = sourceSelectedGpxFile;
		this.joinSegments = sourceSelectedGpxFile.joinSegments;

		setGpxFile(sourceSelectedGpxFile.gpxFile.clone(), app);
		if (joinSegments) {
			gpxFile.addGeneralTrack();
		}

		smoothingFilter = new SmoothingFilter(app, sourceSelectedGpxFile);
		speedFilter = new SpeedFilter(app, sourceSelectedGpxFile);
		altitudeFilter = new AltitudeFilter(app, sourceSelectedGpxFile);
		hdopFilter = new HdopFilter(app, sourceSelectedGpxFile);
		if (dataItem != null) {
			smoothingFilter.updateValue(dataItem.getParameter(SMOOTHING_THRESHOLD));
			speedFilter.updateValues(dataItem.getParameter(MIN_FILTER_SPEED), dataItem.getParameter(MAX_FILTER_SPEED));
			altitudeFilter.updateValues(dataItem.getParameter(MIN_FILTER_ALTITUDE), dataItem.getParameter(MAX_FILTER_ALTITUDE));
			hdopFilter.updateValue(dataItem.getParameter(MAX_FILTER_HDOP));
		}
	}

	@Override
	public void setGpxFile(@NonNull GpxFile gpxFile, @NonNull OsmandApplication app) {
		super.setGpxFile(gpxFile, app);
		leftPointsCount = calculatePointsCount(gpxFile);
		totalPointsCount = calculatePointsCount(getSourceSelectedGpxFile().getGpxFile());
	}

	@Override
	protected void update(@NonNull OsmandApplication app) {
		GpxTrackAnalysis sourceAnalysis = sourceSelectedGpxFile.trackAnalysis;
		smoothingFilter.updateAnalysis(sourceAnalysis);
		speedFilter.updateAnalysis(sourceAnalysis);
		altitudeFilter.updateAnalysis(sourceAnalysis);
		hdopFilter.updateAnalysis(sourceAnalysis);
		app.getGpsFilterHelper().filterGpxFile(this, false);
	}

	public void updateGpxFile(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile) {
		this.gpxFile = gpxFile;
		if (gpxFile.getTracks().size() > 0) {
			color = gpxFile.getTracks().get(0).getColor(0);
		}
		modifiedTime = gpxFile.getModifiedTime();
		processPoints(app);

		leftPointsCount = calculatePointsCount(gpxFile);
		totalPointsCount = calculatePointsCount(sourceSelectedGpxFile.getGpxFile());
	}

	@Override
	public void processPoints(@NonNull OsmandApplication app) {
		processedPointsToDisplay = gpxFile.processPoints();
		updateBounds();
		updateArea(hasMapRenderer(app));
	}

	private int calculatePointsCount(@NonNull GpxFile gpxFile) {
		int count = 0;
		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		for (TrkSegment segment : segments) {
			count += segment.getPoints().size();
		}
		return count;
	}

	public void resetFilters(@NonNull OsmandApplication app) {
		smoothingFilter.reset();
		speedFilter.reset();
		altitudeFilter.reset();
		hdopFilter.reset();

		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		GpxDataItem item = gpxDbHelper.getItem(new KFile(gpxFile.getPath()));
		if (item != null) {
			item.setParameter(SMOOTHING_THRESHOLD, Double.NaN);
			item.setParameter(MIN_FILTER_SPEED, Double.NaN);
			item.setParameter(MAX_FILTER_SPEED, Double.NaN);
			item.setParameter(MIN_FILTER_ALTITUDE, Double.NaN);
			item.setParameter(MAX_FILTER_ALTITUDE, Double.NaN);
			item.setParameter(MAX_FILTER_HDOP, Double.NaN);

			gpxDbHelper.updateDataItem(item);
		}
		app.getGpsFilterHelper().filterGpxFile(this, true);
	}

	@NonNull
	@Override
	public List<TrkSegment> getPointsToDisplay() {
		return joinSegments && gpxFile != null && gpxFile.getGeneralTrack() != null
				? gpxFile.getGeneralTrack().getSegments()
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

	public static boolean isGpsFiltersConfigValid(@NonNull GpxDataItem item) {
		double smoothingThreshold = item.getParameter(SMOOTHING_THRESHOLD);
		double minFilterSpeed = item.getParameter(MIN_FILTER_SPEED);
		double maxFilterSpeed = item.getParameter(MAX_FILTER_SPEED);
		double minFilterAltitude = item.getParameter(MIN_FILTER_ALTITUDE);
		double maxFilterAltitude = item.getParameter(MAX_FILTER_ALTITUDE);
		double maxFilterHdop = item.getParameter(MAX_FILTER_HDOP);

		double sum = smoothingThreshold + minFilterSpeed + maxFilterSpeed + minFilterAltitude + maxFilterAltitude + maxFilterHdop;
		return !Double.isNaN(sum) && sum != 0;
	}
}