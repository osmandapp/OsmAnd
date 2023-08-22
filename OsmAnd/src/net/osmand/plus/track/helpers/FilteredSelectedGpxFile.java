package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpsFilterHelper.AltitudeFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.HdopFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SmoothingFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SpeedFilter;

import java.io.File;
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
	                               @Nullable GpxDataItem gpxDataItem) {
		this.sourceSelectedGpxFile = sourceSelectedGpxFile;
		this.joinSegments = sourceSelectedGpxFile.joinSegments;
		this.hiddenGroups = sourceSelectedGpxFile.getHiddenGroups();

		setGpxFile(GpsFilterHelper.copyGpxFile(app, sourceSelectedGpxFile.gpxFile), app);
		if (joinSegments) {
			gpxFile.addGeneralTrack();
		}

		smoothingFilter = new SmoothingFilter(app, sourceSelectedGpxFile);
		speedFilter = new SpeedFilter(app, sourceSelectedGpxFile);
		altitudeFilter = new AltitudeFilter(app, sourceSelectedGpxFile);
		hdopFilter = new HdopFilter(app, sourceSelectedGpxFile);
		if (gpxDataItem != null) {
			smoothingFilter.updateValue(gpxDataItem.getSmoothingThreshold());
			speedFilter.updateValues(gpxDataItem.getMinFilterSpeed(), gpxDataItem.getMaxFilterSpeed());
			altitudeFilter.updateValues(gpxDataItem.getMinFilterAltitude(), gpxDataItem.getMaxFilterAltitude());
			hdopFilter.updateValue(gpxDataItem.getMaxFilterHdop());
		}
	}

	@Override
	public void setGpxFile(@NonNull GPXFile gpxFile, @NonNull OsmandApplication app) {
		super.setGpxFile(gpxFile, app);
		leftPointsCount = calculatePointsCount(gpxFile);
		totalPointsCount = calculatePointsCount(getSourceSelectedGpxFile().getGpxFile());
	}

	@Override
	protected void update(@NonNull OsmandApplication app) {
		GPXTrackAnalysis sourceAnalysis = sourceSelectedGpxFile.trackAnalysis;
		smoothingFilter.updateAnalysis(sourceAnalysis);
		speedFilter.updateAnalysis(sourceAnalysis);
		altitudeFilter.updateAnalysis(sourceAnalysis);
		hdopFilter.updateAnalysis(sourceAnalysis);
		app.getGpsFilterHelper().filterGpxFile(this, false);
	}

	public void updateGpxFile(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		this.gpxFile = gpxFile;
		if (gpxFile.tracks.size() > 0) {
			color = gpxFile.tracks.get(0).getColor(0);
		}
		modifiedTime = gpxFile.modifiedTime;
		processPoints(app);

		leftPointsCount = calculatePointsCount(gpxFile);
		totalPointsCount = calculatePointsCount(sourceSelectedGpxFile.getGpxFile());
	}

	@Override
	public void processPoints(OsmandApplication app) {
		processedPointsToDisplay = gpxFile.proccessPoints();
		updateBounds();
		updateArea(hasMapRenderer(app));
	}

	private int calculatePointsCount(@NonNull GPXFile gpxFile) {
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

		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		GpxDataItem gpxDataItem = gpxDbHelper.getItem(new File(gpxFile.path));
		if (gpxDataItem != null) {
			gpxDbHelper.resetGpsFilters(gpxDataItem);
		}

		app.getGpsFilterHelper().filterGpxFile(this, true);
	}

	@NonNull
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

	public static boolean isGpsFiltersConfigValid(@NonNull GpxDataItem dataItem) {
		double sum = dataItem.getSmoothingThreshold() + dataItem.getMinFilterSpeed()
				+ dataItem.getMaxFilterSpeed() + dataItem.getMinFilterAltitude()
				+ dataItem.getMaxFilterAltitude() + dataItem.getMaxFilterHdop();
		return !Double.isNaN(sum) && sum != 0;
	}
}