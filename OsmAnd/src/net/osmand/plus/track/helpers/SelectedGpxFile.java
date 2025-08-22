package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TrackArea;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.views.OsmandMap;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SelectedGpxFile {

	public boolean notShowNavigationDialog;
	public boolean selectedByUser = true;

	protected GpxFile gpxFile;
	protected GpxTrackAnalysis trackAnalysis;
	protected long analysisParametersVersion;

	protected List<TrkSegment> processedPointsToDisplay = new ArrayList<>();
	protected List<GpxDisplayGroup> splitGroups;

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

	public void setGpxFile(@NonNull GpxFile gpxFile, @NonNull OsmandApplication app) {
		this.gpxFile = gpxFile;
		if (!Algorithms.isEmpty(gpxFile.getTracks())) {
			this.color = gpxFile.getTracks().get(0).getColor(0);
		}
		processPoints(app);
		if (filteredSelectedGpxFile != null) {
			app.getGpsFilterHelper().filterGpxFile(filteredSelectedGpxFile, false);
		}
	}

	public boolean isLoaded() {
		return gpxFile.getModifiedTime() != -1;
	}

	public GpxTrackAnalysis getTrackAnalysis(@NonNull OsmandApplication app) {
		if (modifiedTime != gpxFile.getModifiedTime()
				|| analysisParametersVersion != getAnalysisParametersVersion(app)) {
			update(app);
		}
		return trackAnalysis;
	}

	public GpxTrackAnalysis getTrackAnalysisToDisplay(OsmandApplication app) {
		return filteredSelectedGpxFile != null
				? filteredSelectedGpxFile.getTrackAnalysis(app)
				: getTrackAnalysis(app);
	}

	public void setTrackAnalysis(@NonNull GpxTrackAnalysis trackAnalysis) {
		this.trackAnalysis = trackAnalysis;
	}

	public void setSplitGroups(@Nullable List<GpxDisplayGroup> splitGroups) {
		this.splitGroups = splitGroups;
		this.splitProcessed = true;
	}

	private long getAnalysisParametersVersion(@NonNull OsmandApplication app) {
		String path = gpxFile.getPath();
		KFile file = !Algorithms.isEmpty(path) ? new KFile(path) : null;
		GpxDataItem dataItem = file != null ? app.getGpxDbHelper().getItem(file, false) : null;
		return dataItem != null ? dataItem.getAnalysisParametersVersion() : 0;
	}

	protected void update(@NonNull OsmandApplication app) {
		modifiedTime = gpxFile.getModifiedTime();
		analysisParametersVersion = getAnalysisParametersVersion(app);
		pointsModifiedTime = gpxFile.getPointsModifiedTime();

		long fileTimestamp = Algorithms.isEmpty(gpxFile.getPath())
				? System.currentTimeMillis()
				: new File(gpxFile.getPath()).lastModified();
		trackAnalysis = gpxFile.getAnalysis(fileTimestamp, null, null, PluginsHelper.getTrackPointsAnalyser());

		updateSplit(app);

		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.update(app);
		}
	}

	private void updateSplit(@NonNull OsmandApplication app) {
		splitGroups = null;
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

		processedPointsToDisplay = gpxFile.processPoints();
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
					? gpxFile.getGeneralTrack().getSegments()
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
		if (processedPointsToDisplay.isEmpty()) {
			lastSegment = new TrkSegment();
			processedPointsToDisplay.add(lastSegment);
		} else {
			lastSegment = processedPointsToDisplay.get(processedPointsToDisplay.size() - 1);
		}
		lastSegment.getPoints().add(point);

		TrkSegment generalSegment = gpxFile != null ? gpxFile.getGeneralSegment() : null;
		if (generalSegment != null) {
			generalSegment.getPoints().add(point);
		}

		boolean hasCalculatedBounds = !bounds.hasInitialState();
		if (hasCalculatedBounds) {
			// Update already calculated bounds without iterating all points
			KQuadRect kQuadRect = SharedUtil.kQuadRect(bounds);
			GpxUtilities.INSTANCE.updateBounds(kQuadRect, Collections.singletonList(point), 0);

			bounds.right = kQuadRect.getRight();
			bounds.left = kQuadRect.getLeft();
			bounds.top = kQuadRect.getTop();
			bounds.bottom = kQuadRect.getBottom();
		} else {
			updateBounds();
		}

		// Update path31 without iterating all points
		if (hasMapRenderer(app)) {
			if (area == null) {
				area = new TrackArea();
			}
			int x31 = MapUtils.get31TileNumberX(point.getLon());
			int y31 = MapUtils.get31TileNumberY(point.getLat());
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
		bounds = SharedUtil.jQuadRect(GpxUtilities.INSTANCE.calculateTrackBounds(processedPointsToDisplay));
	}

	protected final void updateArea(boolean hasMapRenderer) {
		if (!hasMapRenderer) {
			area = null;
			return;
		}

		area = new TrackArea();
		for (TrkSegment segment : processedPointsToDisplay) {
			for (WptPt point : segment.getPoints()) {
				int x31 = MapUtils.get31TileNumberX(point.getLongitude());
				int y31 = MapUtils.get31TileNumberY(point.getLatitude());
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
	public GpxFile getGpxFile() {
		return gpxFile;
	}

	public GpxFile getGpxFileToDisplay() {
		return filteredSelectedGpxFile != null ? filteredSelectedGpxFile.getGpxFile() : gpxFile;
	}

	public GpxFile getModifiableGpxFile() {
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

	public List<GpxDisplayGroup> getSplitGroups(@NonNull OsmandApplication app) {
		if (modifiedTime != gpxFile.getModifiedTime()) {
			update(app);
		}
		if (!splitProcessed) {
			updateSplit(app);
		}
		return filteredSelectedGpxFile != null ? filteredSelectedGpxFile.getSplitGroups(app) : splitGroups;
	}

	public void setSplitGroups(List<GpxDisplayGroup> displayGroups, OsmandApplication app) {
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.setSplitGroups(displayGroups, app);
		} else {
			this.splitProcessed = true;
			this.splitGroups = displayGroups;

			if (modifiedTime != gpxFile.getModifiedTime()) {
				update(app);
			}
		}
	}

	protected final boolean hasMapRenderer(@NonNull OsmandApplication app) {
		OsmandMap osmandMap = app.getOsmandMap();
		return osmandMap != null && osmandMap.getMapView().hasMapRenderer();
	}

	@NonNull
	public FilteredSelectedGpxFile createFilteredSelectedGpxFile(@NonNull OsmandApplication app, @Nullable GpxDataItem item) {
		filteredSelectedGpxFile = new FilteredSelectedGpxFile(app, this, item);
		return filteredSelectedGpxFile;
	}

	@Nullable
	public FilteredSelectedGpxFile getFilteredSelectedGpxFile() {
		return filteredSelectedGpxFile;
	}
}
