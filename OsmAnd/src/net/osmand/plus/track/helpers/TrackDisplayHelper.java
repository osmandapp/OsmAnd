package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.GpxSplitType.NO_SPLIT;
import static net.osmand.shared.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackDisplayHelper {

	private final OsmandApplication app;
	private final GpxAppearanceHelper gpxAppearanceHelper;

	private File file;
	private GpxFile gpxFile;
	private GpxFile filteredGpxFile;
	private GpxDataItem gpxDataItem;
	private SelectedGpxFile selectedGpxFile;
	protected long analysisParametersVersion;

	private long modifiedTime = -1;
	private List<GpxDisplayGroup> displayGroups;
	private final List<GpxDisplayGroup> originalGroups = new ArrayList<>();

	public TrackDisplayHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.gpxAppearanceHelper = new GpxAppearanceHelper(app);
	}

	@Nullable
	public GpxFile getGpx() {
		return gpxFile;
	}

	@Nullable
	public GpxFile getGpxFileToDisplay() {
		return filteredGpxFile != null ? filteredGpxFile : gpxFile;
	}

	@Nullable
	public GpxDataItem getGpxDataItem() {
		return gpxDataItem;
	}

	public File getFile() {
		return file;
	}

	public void setFile(@Nullable File file) {
		this.file = file;
	}

	public void setGpx(@NonNull GpxFile result) {
		this.gpxFile = result;
		if (file == null) {
			this.gpxFile = app.getSavingTrackHelper().getCurrentGpx();
		}
	}

	public void setFilteredGpxFile(@Nullable GpxFile filteredGpxFile) {
		this.filteredGpxFile = filteredGpxFile;
	}

	public void setGpxDataItem(@Nullable GpxDataItem gpxDataItem) {
		this.gpxDataItem = gpxDataItem;
	}

	public void setSelectedGpxFile(@Nullable SelectedGpxFile selectedGpxFile) {
		this.selectedGpxFile = selectedGpxFile;
	}

	@NonNull
	public QuadRect getRect() {
		if (filteredGpxFile != null) {
			return SharedUtil.jQuadRect(filteredGpxFile.getRect());
		}
		if (gpxFile != null) {
			return SharedUtil.jQuadRect(gpxFile.getRect());
		} else {
			return new QuadRect(0, 0, 0, 0);
		}
	}

	public boolean setJoinSegments(boolean joinSegments) {
		if (gpxDataItem != null) {
			boolean updated = app.getGpxDbHelper().updateDataItemParameter(gpxDataItem, JOIN_SEGMENTS, joinSegments);
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.getPath());
			if (updated && selectedGpxFile != null) {
				selectedGpxFile.setJoinSegments(joinSegments);
			}
			return updated;
		}
		return false;
	}

	public boolean isJoinSegments() {
		return gpxDataItem != null ? gpxDataItem.getParameter(JOIN_SEGMENTS) : false;
	}

	public List<GpxDisplayGroup> getGpxFile(boolean useDisplayGroups) {
		if (filteredGpxFile == null && gpxFile == null) {
			return new ArrayList<>();
		}
		if (gpxFile.getModifiedTime() != modifiedTime
				|| analysisParametersVersion != getAnalysisParametersVersion()) {
			updateDisplayGroups();
		}
		if (useDisplayGroups) {
			return displayGroups;
		} else {
			return originalGroups;
		}
	}

	private long getAnalysisParametersVersion() {
		return gpxDataItem != null ? gpxDataItem.getAnalysisParametersVersion() : 0;
	}

	public void updateDisplayGroups() {
		modifiedTime = gpxFile.getModifiedTime();
		long analysisParametersVersion = getAnalysisParametersVersion();
		boolean analysisParametersVersionChanged = this.analysisParametersVersion != analysisParametersVersion;
		this.analysisParametersVersion = analysisParametersVersion;

		GpxFile gpx = filteredGpxFile != null ? filteredGpxFile : gpxFile;
		displayGroups = app.getGpxDisplayHelper().collectDisplayGroups(selectedGpxFile, gpx, true,
				!analysisParametersVersionChanged && useCachedGroups());
		originalGroups.clear();
		for (GpxDisplayGroup group : displayGroups) {
			originalGroups.add(group.copy());
		}
	}

	private boolean useCachedGroups() {
		if (gpxDataItem != null) {
			Integer type = gpxAppearanceHelper.getParameter(gpxDataItem, SPLIT_TYPE);
			return type == null || GpxSplitType.getSplitTypeByTypeId(type) == NO_SPLIT;
		}
		return true;
	}

	@NonNull
	public List<GpxDisplayGroup> getPointsOriginalGroups() {
		GpxDisplayItemType[] filterTypes = {
				GpxDisplayItemType.TRACK_POINTS,
				GpxDisplayItemType.TRACK_ROUTE_POINTS
		};
		return getOriginalGroups(filterTypes);
	}

	@NonNull
	public List<GpxDisplayGroup> getOriginalGroups(GpxDisplayItemType[] filterTypes) {
		return filterGroups(false, filterTypes);
	}

	@NonNull
	public List<GpxDisplayGroup> getDisplayGroups(GpxDisplayItemType[] filterTypes) {
		return filterGroups(true, filterTypes);
	}

	private static boolean hasFilterType(GpxDisplayItemType filterType, GpxDisplayItemType[] filterTypes) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private List<GpxDisplayGroup> filterGroups(boolean useDisplayGroups, GpxDisplayItemType[] filterTypes) {
		return filterGroups(getGpxFile(useDisplayGroups), filterTypes);
	}

	public static List<GpxDisplayGroup> filterGroups(@NonNull List<GpxDisplayGroup> groups, @NonNull GpxDisplayItemType[] filterTypes) {
		List<GpxDisplayGroup> filteredGroup = new ArrayList<>();
		for (GpxDisplayGroup group : groups) {
			if (hasFilterType(group.getType(), filterTypes)) {
				filteredGroup.add(group);
			}
		}
		return filteredGroup;
	}

	public static List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for (GpxDisplayGroup g : groups) {
			list.addAll(g.getDisplayItems());
		}
		return list;
	}
}
