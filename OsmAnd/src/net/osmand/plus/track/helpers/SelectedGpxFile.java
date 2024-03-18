package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TrackArea;
import net.osmand.data.QuadRect;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.views.OsmandMap;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SelectedGpxFile {

	public boolean notShowNavigationDialog;
	public boolean selectedByUser = true;

	protected GPXFile gpxFile;
	protected GPXTrackAnalysis trackAnalysis;

	protected List<TrkSegment> processedPointsToDisplay = new ArrayList<>();
	protected List<GpxDisplayGroup> displayGroups;

	@NonNull
	protected QuadRect bounds = new QuadRect();
	@Nullable
	protected TrackArea area = null;

	protected int color;
	protected long modifiedTime = -1;
	protected long pointsModifiedTime = -1;

	private boolean routePoints;
	protected boolean joinSegments;
	private boolean showCurrentTrack;
	protected boolean splitProcessed;

	private FilteredSelectedGpxFile filteredSelectedGpxFile;

	public void setGpxFile(@NonNull GPXFile gpxFile, @NonNull OsmandApplication app) {
		this.gpxFile = gpxFile;
		if (!Algorithms.isEmpty(gpxFile.tracks)) {
			this.color = gpxFile.tracks.get(0).getColor(0);
		}
		processPoints(app);
		if (filteredSelectedGpxFile != null) {
			app.getGpsFilterHelper().filterGpxFile(filteredSelectedGpxFile, false);
		}
	}

	public boolean isLoaded() {
		return gpxFile.modifiedTime != -1;
	}

	public GPXTrackAnalysis getTrackAnalysis(OsmandApplication app) {
		if (modifiedTime != gpxFile.modifiedTime) {
			update(app);
		}
		return trackAnalysis;
	}

	public GPXTrackAnalysis getTrackAnalysisToDisplay(OsmandApplication app) {
		return filteredSelectedGpxFile != null
				? filteredSelectedGpxFile.getTrackAnalysis(app)
				: getTrackAnalysis(app);
	}

	public void setTrackAnalysis(@NonNull GPXTrackAnalysis trackAnalysis) {
		this.trackAnalysis = trackAnalysis;
	}

	public void setDisplayGroups(@Nullable List<GpxDisplayGroup> displayGroups) {
		this.displayGroups = displayGroups;
		this.splitProcessed = true;
	}

	protected void update(@NonNull OsmandApplication app) {
		modifiedTime = gpxFile.modifiedTime;
		pointsModifiedTime = gpxFile.pointsModifiedTime;

		long fileTimestamp = Algorithms.isEmpty(gpxFile.path)
				? System.currentTimeMillis()
				: new File(gpxFile.path).lastModified();
		trackAnalysis = gpxFile.getAnalysis(fileTimestamp, null, null, PluginsHelper.getTrackPointsAnalyser());

		updateSplit(app);

		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.update(app);
		}
	}

	private void updateSplit(@NonNull OsmandApplication app) {
		displayGroups = null;
		if (showCurrentTrack) {
			splitProcessed = true;
		} else {
			app.getGpxDisplayHelper().processSplitAsync(this, result -> {
				splitProcessed = result;
				return true;
			});
		}
	}

	public void processPoints(@NonNull OsmandApplication app) {
		update(app);

		processedPointsToDisplay = gpxFile.proccessPoints();
		routePoints = false;
		if (processedPointsToDisplay.isEmpty()) {
			processedPointsToDisplay = gpxFile.processRoutePoints();
			routePoints = !processedPointsToDisplay.isEmpty();
		}

		updateBounds();
		updateArea(hasMapRenderer(app));

		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.processPoints(app);
		}
	}

	public boolean isRoutePoints() {
		return routePoints;
	}

	@NonNull
	public List<TrkSegment> getPointsToDisplay() {
		if (filteredSelectedGpxFile != null) {
			return filteredSelectedGpxFile.getPointsToDisplay();
		} else if (joinSegments) {
			return gpxFile != null && gpxFile.getGeneralTrack() != null
					? gpxFile.getGeneralTrack().segments
					: Collections.emptyList();
		} else {
			return processedPointsToDisplay;
		}
	}

	public final void addEmptySegmentToDisplay() {
		processedPointsToDisplay.add(new TrkSegment());
	}

	public final void appendTrackPointToDisplay(@NonNull WptPt point, @NonNull OsmandApplication app) {
		TrkSegment lastSegment;
		if (processedPointsToDisplay.size() == 0) {
			lastSegment = new TrkSegment();
			processedPointsToDisplay.add(lastSegment);
		} else {
			lastSegment = processedPointsToDisplay.get(processedPointsToDisplay.size() - 1);
		}

		lastSegment.points.add(point);

		boolean hasCalculatedBounds = !bounds.hasInitialState();
		if (hasCalculatedBounds) {
			// Update already calculated bounds without iterating all points
			GPXUtilities.updateBounds(bounds, Collections.singletonList(point), 0);
		} else {
			updateBounds();
		}

		// Update path31 without iterating all points
		if (hasMapRenderer(app)) {
			if (area == null) {
				area = new TrackArea();
			}
			int x31 = MapUtils.get31TileNumberX(point.lon);
			int y31 = MapUtils.get31TileNumberY(point.lat);
			area.add(new PointI(x31, y31));
		}
	}

	public final void clearSegmentsToDisplay() {
		processedPointsToDisplay.clear();
		bounds = new QuadRect();
		area = null;
	}

	@NonNull
	public final QuadRect getBoundsToDisplay() {
		return filteredSelectedGpxFile != null
				? filteredSelectedGpxFile.getBoundsToDisplay()
				: bounds;
	}

	@NonNull
	public final AreaI getAreaToDisplay() {
		if (filteredSelectedGpxFile != null) {
			return filteredSelectedGpxFile.getAreaToDisplay();
		}

		if (area == null) {
			updateArea(true);
		}
		return area.normalized();
	}

	protected final void updateBounds() {
		bounds = GPXUtilities.calculateTrackBounds(processedPointsToDisplay);
	}

	protected final void updateArea(boolean hasMapRenderer) {
		if (!hasMapRenderer) {
			area = null;
			return;
		}

		area = new TrackArea();
		for (TrkSegment segment : processedPointsToDisplay) {
			for (WptPt point : segment.points) {
				int x31 = MapUtils.get31TileNumberX(point.lon);
				int y31 = MapUtils.get31TileNumberY(point.lat);
				area.add(new PointI(x31, y31));
			}
		}
	}

	public int getHiddenGroupsCount() {
		int counter = 0;
		for (PointsGroup group : new ArrayList<>(gpxFile.getPointsGroups().values())) {
			if (group.isHidden()) {
				counter++;
			}
		}
		return counter;
	}

	public boolean isGroupHidden(@Nullable String name) {
		PointsGroup pointsGroup = gpxFile.getPointsGroups().get(name != null ? name : "");
		return pointsGroup != null && pointsGroup.isHidden();
	}

	@NonNull
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public GPXFile getGpxFileToDisplay() {
		return filteredSelectedGpxFile != null ? filteredSelectedGpxFile.getGpxFile() : gpxFile;
	}

	public GPXFile getModifiableGpxFile() {
		// call process points after
		return gpxFile;
	}

	public boolean isShowCurrentTrack() {
		return showCurrentTrack;
	}

	public void setShowCurrentTrack(boolean showCurrentTrack) {
		this.showCurrentTrack = showCurrentTrack;
	}

	public boolean isJoinSegments() {
		return joinSegments;
	}

	public void setJoinSegments(boolean joinSegments) {
		this.joinSegments = joinSegments;
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.setJoinSegments(joinSegments);
		}
	}

	public int getColor() {
		return color;
	}

	public long getModifiedTime() {
		return modifiedTime;
	}

	public long getPointsModifiedTime() {
		return pointsModifiedTime;
	}

	public void resetSplitProcessed() {
		splitProcessed = false;
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.splitProcessed = false;
		}
	}

	public List<GpxDisplayGroup> getDisplayGroups(@NonNull OsmandApplication app) {
		if (modifiedTime != gpxFile.modifiedTime) {
			update(app);
		}
		if (!splitProcessed) {
			updateSplit(app);
		}
		return filteredSelectedGpxFile != null ? filteredSelectedGpxFile.getDisplayGroups(app) : displayGroups;
	}

	public void setDisplayGroups(List<GpxDisplayGroup> displayGroups, OsmandApplication app) {
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.setDisplayGroups(displayGroups, app);
		} else {
			this.splitProcessed = true;
			this.displayGroups = displayGroups;

			if (modifiedTime != gpxFile.modifiedTime) {
				update(app);
			}
		}
	}

	protected final boolean hasMapRenderer(@NonNull OsmandApplication app) {
		OsmandMap osmandMap = app.getOsmandMap();
		return osmandMap != null && osmandMap.getMapView().hasMapRenderer();
	}

	@NonNull
	public FilteredSelectedGpxFile createFilteredSelectedGpxFile(@NonNull OsmandApplication app,
	                                                             @Nullable GpxDataItem gpxDataItem) {
		filteredSelectedGpxFile = new FilteredSelectedGpxFile(app, this, gpxDataItem);
		return filteredSelectedGpxFile;
	}

	@Nullable
	public FilteredSelectedGpxFile getFilteredSelectedGpxFile() {
		return filteredSelectedGpxFile;
	}
}
