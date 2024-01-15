package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.GpxSplitType.DISTANCE;
import static net.osmand.plus.track.GpxSplitType.NO_SPLIT;
import static net.osmand.plus.track.GpxSplitType.TIME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class GpxDisplayGroup {

	private final GPXFile gpxFile;

	private GpxDisplayItemType type = GpxDisplayItemType.TRACK_SEGMENT;
	private List<GpxDisplayItem> displayItems = new ArrayList<>();
	private String gpxName;
	private String name;
	private String description;
	private Track track;
	private double splitDistance = -1;
	private int splitTime = -1;
	private int color;
	private boolean generalTrack;

	public GpxDisplayGroup(@NonNull GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public GpxDisplayGroup(@NonNull GpxDisplayGroup group) {
		gpxFile = group.gpxFile;
		type = group.type;
		name = group.name;
		description = group.description;
		track = group.track;
		displayItems = new ArrayList<>(group.displayItems);
	}

	@NonNull
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	@Nullable
	public Track getTrack() {
		return track;
	}

	public void setTrack(@Nullable Track track) {
		this.track = track;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGpxName() {
		return gpxName;
	}

	public void setGpxName(String gpxName) {
		this.gpxName = gpxName;
	}

	public String getName() {
		return name;
	}

	@NonNull
	public List<GpxDisplayItem> getDisplayItems() {
		return new ArrayList<>(displayItems);
	}

	public void addDisplayItems(@NonNull List<GpxDisplayItem> items) {
		displayItems = CollectionUtils.addAllToList(displayItems, items);
	}

	public void clearDisplayItems() {
		displayItems = new ArrayList<>();
	}

	public GpxDisplayItemType getType() {
		return type;
	}

	public void setType(GpxDisplayItemType type) {
		this.type = type;
	}

	public boolean isSplitDistance() {
		return splitDistance > 0;
	}

	public double getSplitDistance() {
		return splitDistance;
	}

	public boolean isSplitTime() {
		return splitTime > 0;
	}

	public int getSplitTime() {
		return splitTime;
	}

	public void updateSplit(@NonNull GpxSplitParams splitParams) {
		clearDisplayItems();

		if (splitParams.splitType == NO_SPLIT) {
			splitDistance = -1;
			splitTime = -1;
		} else if (splitParams.splitType == DISTANCE) {
			splitDistance = splitParams.splitInterval;
			splitTime = -1;
		} else if (splitParams.splitType == TIME) {
			splitDistance = -1;
			splitTime = (int) splitParams.splitInterval;
		}
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public boolean isGeneralTrack() {
		return generalTrack;
	}

	public void setGeneralTrack(boolean generalTrack) {
		this.generalTrack = generalTrack;
	}
}
