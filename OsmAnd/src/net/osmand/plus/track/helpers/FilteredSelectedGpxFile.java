package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GPXUtilities.calculateTrackBounds;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.QVectorPointI;
import net.osmand.data.QuadRect;
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

	public void updateGpxFile(@NonNull GPXFile gpxFile) {
		this.gpxFile = gpxFile;
		if (gpxFile.tracks.size() > 0) {
			color = gpxFile.tracks.get(0).getColor(0);
		}
		modifiedTime = gpxFile.modifiedTime;
		processedPointsToDisplay = gpxFile.proccessPoints();
		leftPointsCount = calculatePointsCount(gpxFile);
		totalPointsCount = calculatePointsCount(sourceSelectedGpxFile.getGpxFile());
	}

	public void setTrackAnalysis(@NonNull GPXTrackAnalysis trackAnalysis) {
		this.trackAnalysis = trackAnalysis;
	}

	public void setDisplayGroups(@Nullable List<GpxDisplayGroup> displayGroups) {
		this.displayGroups = displayGroups;
		this.splitProcessed = true;
	}

	@Override
	public void processPoints(OsmandApplication app) {
		processedPointsToDisplay = gpxFile.proccessPoints();
		if (app.getOsmandMap() != null &&
			app.getOsmandMap().getMapView() != null &&
			app.getOsmandMap().getMapView().hasMapRenderer()) {
			path31 = trackPointsToPath31(processedPointsToDisplay);
		} else {
			path31 = null;
		}
		path31FromGeneralTrack = false;
		bBox = calculateTrackBounds(processedPointsToDisplay);
		bBoxFromGeneralTrack = false;
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

	@Override
	protected boolean processSplit(@NonNull OsmandApplication app) {
		return GpxDisplayHelper.processSplit(app, this);
	}

	@NonNull
	@Override
	public List<TrkSegment> getPointsToDisplay() {
		return joinSegments && gpxFile != null && gpxFile.getGeneralTrack() != null
				? gpxFile.getGeneralTrack().segments
				: processedPointsToDisplay;
	}

	@NonNull
	@Override
	public QuadRect getBBoxToDisplay() {
		if (joinSegments && gpxFile != null) {
			if (!gpxFile.hasGeneralTrack()) {
				if (gpxFile.getGeneralTrack() != null) {
					bBox = calculateTrackBounds(gpxFile.getGeneralTrack().segments);
					bBoxFromGeneralTrack = true;
				}
			} else {
				if (!bBoxFromGeneralTrack || bBox == null) {
					bBox = calculateTrackBounds(gpxFile.getGeneralTrack().segments);
					bBoxFromGeneralTrack = true;
				}
			}
		}
		if (bBox == null) {
			bBox = calculateTrackBounds(processedPointsToDisplay);
		}
		return bBox;
	}

	@NonNull
	@Override
	public QVectorPointI getPath31ToDisplay() {
		if (joinSegments && gpxFile != null) {
			if (!gpxFile.hasGeneralTrack()) {
				if (gpxFile.getGeneralTrack() != null) {
					path31 = trackPointsToPath31(gpxFile.getGeneralTrack().segments);
					path31FromGeneralTrack = true;
				}
			} else {
				if (!path31FromGeneralTrack || path31 == null) {
					path31 = trackPointsToPath31(gpxFile.getGeneralTrack().segments);
					path31FromGeneralTrack = true;
				}
			}
		}
		if (path31 == null) {
			path31 = trackPointsToPath31(processedPointsToDisplay);
		}
		return path31;
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