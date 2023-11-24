package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.data.QuadRect;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackDisplayHelper {

	private final OsmandApplication app;

	private File file;
	private GPXFile gpxFile;
	private GPXFile filteredGpxFile;
	private GpxDataItem gpxDataItem;

	private long modifiedTime = -1;
	private List<GpxDisplayGroup> displayGroups;
	private final List<GpxDisplayGroup> originalGroups = new ArrayList<>();

	public TrackDisplayHelper(OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public GPXFile getGpx() {
		return gpxFile;
	}

	@Nullable
	public GPXFile getGpxFileToDisplay() {
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

	public void setGpx(@NonNull GPXFile result) {
		this.gpxFile = result;
		if (file == null) {
			this.gpxFile = app.getSavingTrackHelper().getCurrentGpx();
		}
	}

	public void setFilteredGpxFile(@Nullable GPXFile filteredGpxFile) {
		this.filteredGpxFile = filteredGpxFile;
	}

	public void setGpxDataItem(GpxDataItem gpxDataItem) {
		this.gpxDataItem = gpxDataItem;
	}

	public QuadRect getRect() {
		if (filteredGpxFile != null) {
			return filteredGpxFile.getRect();
		}
		if (gpxFile != null) {
			return gpxFile.getRect();
		} else {
			return new QuadRect(0, 0, 0, 0);
		}
	}

	public boolean setJoinSegments(boolean joinSegments) {
		if (gpxDataItem != null) {
			boolean updated = app.getGpxDbHelper().updateJoinSegments(gpxDataItem, joinSegments);

			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
			if (updated && selectedGpxFile != null) {
				selectedGpxFile.setJoinSegments(joinSegments);
			}
			return updated;
		}
		return false;
	}

	public boolean isJoinSegments() {
		return gpxDataItem != null && gpxDataItem.isJoinSegments();
	}

	public List<GpxDisplayGroup> getGpxFile(boolean useDisplayGroups) {
		if (filteredGpxFile == null && gpxFile == null) {
			return new ArrayList<>();
		}
		if (gpxFile.modifiedTime != modifiedTime) {
			updateDisplayGroups();
		}
		if (useDisplayGroups) {
			return displayGroups;
		} else {
			return originalGroups;
		}
	}

	public void updateDisplayGroups() {
		modifiedTime = gpxFile.modifiedTime;
		GPXFile gpx = filteredGpxFile != null ? filteredGpxFile : gpxFile;
		displayGroups = app.getGpxDisplayHelper().collectDisplayGroups(gpx, true);
		originalGroups.clear();
		for (GpxDisplayGroup group : displayGroups) {
			originalGroups.add(new GpxDisplayGroup(group));
		}
		if (file != null) {
			SelectedGpxFile sf = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
			if (sf != null && file != null && sf.getDisplayGroups(app) != null) {
				displayGroups = sf.getDisplayGroups(app);
			}
		}
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

	private boolean hasFilterType(GpxDisplayItemType filterType, GpxDisplayItemType[] filterTypes) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private List<GpxDisplayGroup> filterGroups(boolean useDisplayGroups, GpxDisplayItemType[] filterTypes) {
		List<GpxDisplayGroup> groups = new ArrayList<>();
		for (GpxDisplayGroup group : getGpxFile(useDisplayGroups)) {
			if (hasFilterType(group.getType(), filterTypes)) {
				groups.add(group);
			}
		}
		return groups;
	}

	public static List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for (GpxDisplayGroup g : groups) {
			list.addAll(g.getDisplayItems());
		}
		return list;
	}
}
