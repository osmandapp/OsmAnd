package net.osmand.plus.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.QuadRect;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackDisplayHelper {

	private final OsmandApplication app;

	private File file;
	private GPXFile gpxFile;
	private GpxDataItem gpxDataItem;

	private long modifiedTime = -1;
	private List<GpxDisplayGroup> displayGroups;
	private List<GpxDisplayGroup> originalGroups = new ArrayList<>();

	public TrackDisplayHelper(OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public GPXFile getGpx() {
		return gpxFile;
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

	public void setGpxDataItem(GpxDataItem gpxDataItem) {
		this.gpxDataItem = gpxDataItem;
	}

	public QuadRect getRect() {
		if (getGpx() != null) {
			return getGpx().getRect();
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
		if (gpxFile == null) {
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
		GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
		displayGroups = selectedGpxHelper.collectDisplayGroups(gpxFile);
		originalGroups.clear();
		for (GpxDisplayGroup g : displayGroups) {
			originalGroups.add(g.cloneInstance());
		}
		if (file != null) {
			SelectedGpxFile sf = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
			if (sf != null && file != null && sf.getDisplayGroups(app) != null) {
				displayGroups = sf.getDisplayGroups(app);
			}
		}
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
			list.addAll(g.getModifiableList());
		}
		return list;
	}
}
