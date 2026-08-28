package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
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

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectedGpxFile {
	private static final Log LOG = PlatformUtil.getLog(SelectedGpxFile.class);
	private static final ExecutorService FULL_ANALYSIS_EXECUTOR = Executors.newSingleThreadExecutor();

	public boolean notShowNavigationDialog;
	public boolean selectedByUser = true;

	protected GpxFile gpxFile;
	protected GpxTrackAnalysis trackAnalysis;
	protected long analysisParametersVersion;
	private final Object analysisLock = new Object();
	private int analysisGeneration;
	private int runningAnalysisGeneration = -1;
	private boolean fullAnalysisRunning;

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
		FilteredSelectedGpxFile filtered = filteredSelectedGpxFile;
		if (filtered != null) {
			app.getGpsFilterHelper().cancelFiltering(filtered);
		}
		synchronized (analysisLock) {
			this.gpxFile = gpxFile;
			trackAnalysis = null;
			modifiedTime = gpxFile.getModifiedTime();
			pointsModifiedTime = gpxFile.getPointsModifiedTime();
			analysisParametersVersion = 0;
			analysisGeneration++;
			fullAnalysisRunning = false;
			runningAnalysisGeneration = -1;
			analysisLock.notifyAll();
		}
		if (!Algorithms.isEmpty(gpxFile.getTracks())) {
			this.color = gpxFile.getTracks().get(0).getColor(0);
		}
		processPoints(app);
		if (filtered != null) {
			app.getGpsFilterHelper().filterGpxFile(filtered, false);
		}
	}

	public boolean isLoaded() {
		return gpxFile.getModifiedTime() != -1;
	}

	public GpxTrackAnalysis getTrackAnalysis(@NonNull OsmandApplication app) {
		return getFullTrackAnalysis(app);
	}

	public GpxTrackAnalysis getFullTrackAnalysis(@NonNull OsmandApplication app) {
		boolean interrupted = false;
		try {
			while (true) {
				AnalysisRequest request;
				synchronized (analysisLock) {
					while (true) {
						if (isFullTrackAnalysisAvailableLocked(app)) {
							return trackAnalysis;
						}
						if (!fullAnalysisRunning) {
							request = startFullAnalysisLocked(app);
							break;
						}
						try {
							analysisLock.wait();
						} catch (InterruptedException e) {
							interrupted = true;
						}
					}
				}

				try {
					GpxTrackAnalysis analysis = calculateTrackAnalysis(request, true);
					GpxTrackAnalysis published = finishFullAnalysis(app, request, analysis);
					if (published != null) {
						return published;
					}
				} catch (RuntimeException | Error error) {
					finishFullAnalysis(app, request, null);
					throw error;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public GpxTrackAnalysis getTrackSummaryAnalysis(@NonNull OsmandApplication app) {
		while (true) {
			AnalysisRequest request;
			synchronized (analysisLock) {
				if (!isTrackAnalysisOutdatedLocked(app) && trackAnalysis != null) {
					return trackAnalysis;
				}
				request = createAnalysisRequestLocked(app);
			}

			GpxTrackAnalysis summary = calculateTrackAnalysis(request, false);
			synchronized (analysisLock) {
				if (isAnalysisRequestCurrentLocked(request, app)) {
					if (trackAnalysis == null || !trackAnalysis.getCollectPointData()
							|| isTrackAnalysisOutdatedLocked(app)) {
						trackAnalysis = summary;
						modifiedTime = request.fileModifiedTime;
						analysisParametersVersion = request.parametersVersion;
						pointsModifiedTime = request.pointsModifiedTime;
					}
					return trackAnalysis;
				}
			}
		}
	}

	public GpxTrackAnalysis getTrackAnalysisToDisplay(OsmandApplication app) {
		return filteredSelectedGpxFile != null && filteredSelectedGpxFile.isFilterReady()
				? filteredSelectedGpxFile.getFullTrackAnalysis(app)
				: getFullTrackAnalysis(app);
	}

	public GpxTrackAnalysis getTrackSummaryAnalysisToDisplay(OsmandApplication app) {
		return filteredSelectedGpxFile != null && filteredSelectedGpxFile.isFilterReady()
				? filteredSelectedGpxFile.getTrackSummaryAnalysis(app)
				: getTrackSummaryAnalysis(app);
	}

	@Nullable
	public GpxTrackAnalysis getAvailableFullTrackAnalysisToDisplay(@NonNull OsmandApplication app) {
		FilteredSelectedGpxFile filtered = filteredSelectedGpxFile;
		return filtered != null && filtered.isFilterReady()
				? filtered.getAvailableFullTrackAnalysis(app)
				: getAvailableFullTrackAnalysis(app);
	}

	public void requestFullTrackAnalysisToDisplay(@NonNull OsmandApplication app) {
		FilteredSelectedGpxFile filtered = filteredSelectedGpxFile;
		if (filtered != null && filtered.isFilterReady()) {
			filtered.requestFullTrackAnalysis(app);
		} else {
			requestFullTrackAnalysis(app);
		}
	}

	@Nullable
	public GpxTrackAnalysis getAvailableFullTrackAnalysis(@NonNull OsmandApplication app) {
		synchronized (analysisLock) {
			return isFullTrackAnalysisAvailableLocked(app) ? trackAnalysis : null;
		}
	}

	public void requestFullTrackAnalysis(@NonNull OsmandApplication app) {
		AnalysisRequest request;
		synchronized (analysisLock) {
			if (isFullTrackAnalysisAvailableLocked(app) || fullAnalysisRunning) {
				return;
			}
			request = startFullAnalysisLocked(app);
		}
		FULL_ANALYSIS_EXECUTOR.execute(() -> {
			try {
				if (!isAnalysisRequestCurrent(request, app)) {
					finishFullAnalysis(app, request, null);
					return;
				}
				GpxTrackAnalysis analysis = calculateTrackAnalysis(request, true);
				if (finishFullAnalysis(app, request, analysis) != null) {
					app.runInUIThread(() -> app.getOsmandMap().refreshMap());
				}
			} catch (RuntimeException error) {
				finishFullAnalysis(app, request, null);
				LOG.error("Failed to calculate selected GPX analysis", error);
			} catch (Error error) {
				finishFullAnalysis(app, request, null);
				throw error;
			}
		});
	}

	public void cancelPendingFullAnalysis() {
		synchronized (analysisLock) {
			analysisGeneration++;
			fullAnalysisRunning = false;
			runningAnalysisGeneration = -1;
			analysisLock.notifyAll();
		}
	}

	public void setTrackAnalysis(@NonNull GpxTrackAnalysis trackAnalysis) {
		synchronized (analysisLock) {
			this.trackAnalysis = trackAnalysis;
			fullAnalysisRunning = false;
			runningAnalysisGeneration = -1;
			analysisLock.notifyAll();
		}
	}

	public void setTrackSummaryAnalysis(@NonNull GpxTrackAnalysis trackAnalysis,
	                                    long modifiedTime,
	                                    long analysisParametersVersion) {
		synchronized (analysisLock) {
			this.trackAnalysis = trackAnalysis;
			this.modifiedTime = modifiedTime;
			this.analysisParametersVersion = analysisParametersVersion;
		}
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

	private boolean isTrackAnalysisOutdatedLocked(@NonNull OsmandApplication app) {
		return modifiedTime != gpxFile.getModifiedTime()
				|| pointsModifiedTime != gpxFile.getPointsModifiedTime()
				|| analysisParametersVersion != getAnalysisParametersVersion(app);
	}

	private boolean isFullTrackAnalysisAvailableLocked(@NonNull OsmandApplication app) {
		return trackAnalysis != null && trackAnalysis.getCollectPointData()
				&& !isTrackAnalysisOutdatedLocked(app);
	}

	@NonNull
	private AnalysisRequest startFullAnalysisLocked(@NonNull OsmandApplication app) {
		AnalysisRequest request = createAnalysisRequestLocked(app);
		fullAnalysisRunning = true;
		runningAnalysisGeneration = request.generation;
		return request;
	}

	@NonNull
	private AnalysisRequest createAnalysisRequestLocked(@NonNull OsmandApplication app) {
		GpxFile file = gpxFile;
		long fileTimestamp = Algorithms.isEmpty(file.getPath())
				? System.currentTimeMillis()
				: new File(file.getPath()).lastModified();
		return new AnalysisRequest(file, analysisGeneration, file.getModifiedTime(),
				file.getPointsModifiedTime(), getAnalysisParametersVersion(app), fileTimestamp);
	}

	private boolean isAnalysisRequestCurrentLocked(@NonNull AnalysisRequest request,
	                                               @NonNull OsmandApplication app) {
		return analysisGeneration == request.generation && gpxFile == request.gpxFile
				&& gpxFile.getModifiedTime() == request.fileModifiedTime
				&& gpxFile.getPointsModifiedTime() == request.pointsModifiedTime
				&& getAnalysisParametersVersion(app) == request.parametersVersion;
	}

	private boolean isAnalysisRequestCurrent(@NonNull AnalysisRequest request,
	                                         @NonNull OsmandApplication app) {
		synchronized (analysisLock) {
			return isAnalysisRequestCurrentLocked(request, app);
		}
	}

	@NonNull
	private GpxTrackAnalysis calculateTrackAnalysis(@NonNull AnalysisRequest request,
	                                                boolean collectPointData) {
		long analysisStart = System.currentTimeMillis();
		GpxTrackAnalysis analysis = request.gpxFile.getAnalysis(request.fileTimestamp, null, null,
				PluginsHelper.getTrackPointsAnalyser(), collectPointData);
		if (collectPointData && !showCurrentTrack) {
			String path = request.gpxFile.getPath();
			LOG.info("Calculated full selected GPX analysis name="
					+ (Algorithms.isEmpty(path) ? "inMemoryTrack" : new File(path).getName())
					+ ", points=" + analysis.getPoints()
					+ " in " + (System.currentTimeMillis() - analysisStart) + " ms");
		}
		return analysis;
	}

	@Nullable
	private GpxTrackAnalysis finishFullAnalysis(@NonNull OsmandApplication app,
	                                            @NonNull AnalysisRequest request,
	                                            @Nullable GpxTrackAnalysis analysis) {
		synchronized (analysisLock) {
			boolean ownsRunningRequest = fullAnalysisRunning
					&& runningAnalysisGeneration == request.generation;
			if (!ownsRunningRequest) {
				return null;
			}
			fullAnalysisRunning = false;
			runningAnalysisGeneration = -1;
			if (analysis != null && isAnalysisRequestCurrentLocked(request, app)) {
				trackAnalysis = analysis;
				modifiedTime = request.fileModifiedTime;
				pointsModifiedTime = request.pointsModifiedTime;
				analysisParametersVersion = request.parametersVersion;
			}
			analysisLock.notifyAll();
			return analysis != null && trackAnalysis == analysis ? analysis : null;
		}
	}

	private static class AnalysisRequest {
		@NonNull
		private final GpxFile gpxFile;
		private final int generation;
		private final long fileModifiedTime;
		private final long pointsModifiedTime;
		private final long parametersVersion;
		private final long fileTimestamp;

		private AnalysisRequest(@NonNull GpxFile gpxFile, int generation,
		                        long fileModifiedTime, long pointsModifiedTime,
		                        long parametersVersion, long fileTimestamp) {
			this.gpxFile = gpxFile;
			this.generation = generation;
			this.fileModifiedTime = fileModifiedTime;
			this.pointsModifiedTime = pointsModifiedTime;
			this.parametersVersion = parametersVersion;
			this.fileTimestamp = fileTimestamp;
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
		pointsModifiedTime = gpxFile.getPointsModifiedTime();
		splitGroups = null;
		splitProcessed = showCurrentTrack;
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
		} else if (joinSegments && gpxFile != null && gpxFile.getGeneralTrack() != null) {
			return gpxFile.getGeneralTrack().getSegments();
		} else {
			return processedPointsToDisplay;
		}
	}

	public long getPointsToDisplayCount() {
		long total = 0;
		for (TrkSegment segment : getPointsToDisplay()) {
			total += segment.getPoints().size();
		}
		return total;
	}

	public final void addEmptySegmentToDisplay() {
		processedPointsToDisplay.add(new TrkSegment());
	}

	public final void appendTrackPointToDisplay(@NonNull OsmandApplication app,
	                                            @NonNull WptPt point, boolean firstPoint) {
		TrkSegment lastSegment;
		if (processedPointsToDisplay.isEmpty()) {
			lastSegment = new TrkSegment();
			processedPointsToDisplay.add(lastSegment);
		} else {
			lastSegment = processedPointsToDisplay.get(processedPointsToDisplay.size() - 1);
		}
		lastSegment.getPoints().add(point);

		// Add current point to the general segment
		TrkSegment generalSegment = gpxFile != null ? gpxFile.getGeneralSegment() : null;
		if (generalSegment != null) {
			WptPt wptPt = new WptPt(point);
			List<WptPt> points = generalSegment.getPoints();
			if (firstPoint) {
				// Mark current point as start for segment
				wptPt.setFirstPoint(true);
				if (!points.isEmpty()) {
					// Mark previous point as last for segment
					WptPt previousPoint = points.get(points.size() - 1);
					previousPoint.setLastPoint(true);
				}
			}
			points.add(wptPt);
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
		return filteredSelectedGpxFile != null && filteredSelectedGpxFile.isFilterReady()
				? filteredSelectedGpxFile.getBoundsToDisplay()
				: bounds;
	}

	@NonNull
	public final AreaI getAreaToDisplay() {
		if (filteredSelectedGpxFile != null && filteredSelectedGpxFile.isFilterReady()) {
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
		return filteredSelectedGpxFile != null && filteredSelectedGpxFile.isFilterReady()
				? filteredSelectedGpxFile.getGpxFile() : gpxFile;
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
		if (filteredSelectedGpxFile != null) {
			return filteredSelectedGpxFile.isFilterReady()
					? filteredSelectedGpxFile.getSplitGroups(app) : null;
		}
		if (pointsModifiedTime != gpxFile.getPointsModifiedTime()) {
			processPoints(app);
		}
		if (!splitProcessed) {
			updateSplit(app);
		}
		return splitGroups;
	}

	public void setSplitGroups(List<GpxDisplayGroup> displayGroups, OsmandApplication app) {
		setSplitGroups(displayGroups, app, false);
	}

	public void setSplitGroups(List<GpxDisplayGroup> displayGroups, OsmandApplication app, boolean forceUpdate) {
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.setSplitGroups(displayGroups, app);
		} else {
			if (pointsModifiedTime != gpxFile.getPointsModifiedTime() || forceUpdate) {
				processPoints(app);
			}
			this.splitProcessed = true;
			this.splitGroups = displayGroups;
		}
	}

	protected final boolean hasMapRenderer(@NonNull OsmandApplication app) {
		OsmandMap osmandMap = app.getOsmandMap();
		return osmandMap != null && osmandMap.getMapView().hasMapRenderer();
	}

	@NonNull
	public FilteredSelectedGpxFile createFilteredSelectedGpxFile(@NonNull OsmandApplication app, @Nullable GpxDataItem item) {
		filteredSelectedGpxFile = new FilteredSelectedGpxFile(app, this, item);
		if (item != null) {
			app.getGpsFilterHelper().filterGpxFile(filteredSelectedGpxFile, false);
		}
		return filteredSelectedGpxFile;
	}

	@Nullable
	public FilteredSelectedGpxFile getFilteredSelectedGpxFile() {
		return filteredSelectedGpxFile;
	}

	public boolean hasFilters() {
		return filteredSelectedGpxFile != null && filteredSelectedGpxFile.isFilterReady();
	}

	public boolean isFiltering() {
		return filteredSelectedGpxFile != null && !filteredSelectedGpxFile.isFilterReady();
	}
}
