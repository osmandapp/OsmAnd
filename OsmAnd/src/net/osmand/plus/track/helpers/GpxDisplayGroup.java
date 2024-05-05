package net.osmand.plus.track.helpers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class GpxDisplayGroup {

	private final GPXFile gpxFile;

	private List<GpxDisplayItem> displayItems = new ArrayList<>();
	private String gpxName;
	private String name;
	private String description;
	private int index = -1;
	private int color;

	GpxDisplayGroup(@NonNull GPXFile gpxFile, int index) {
		this.gpxFile = gpxFile;
		this.index = index;
	}

	@NonNull
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public abstract void applyName(@NonNull Context context, @NonNull String name);

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getGpxName() {
		return gpxName;
	}

	public void setGpxName(@NonNull String gpxName) {
		this.gpxName = gpxName;
	}

	public int getIndex() {
		return index;
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

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	@NonNull
	public abstract GpxDisplayItemType getType();

	@NonNull
	public GpxDisplayGroup copy() {
		GpxDisplayGroup copy = newInstance(gpxFile);
		copy.gpxName = gpxName;
		copy.name = name;
		copy.description = description;
		copy.color = color;
		copy.displayItems = new ArrayList<>(displayItems);
		return copy;
	}

	@NonNull
	protected abstract GpxDisplayGroup newInstance(@NonNull GPXFile gpxFile);

	@Nullable
	public static TrackDisplayGroup getTrackDisplayGroup(@Nullable GpxDisplayGroup group) {
		return group instanceof TrackDisplayGroup ? (TrackDisplayGroup) group : null;
	}
}
