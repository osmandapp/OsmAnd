package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxDisplayHelper.processGroupTrack;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;

import java.util.ArrayList;
import java.util.List;

public class GpxDisplayGroup {

	private final GPXFile gpxFile;

	private GpxDisplayItemType type = GpxDisplayItemType.TRACK_SEGMENT;
	private List<GpxDisplayItem> list = new ArrayList<>();
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

	@NonNull
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public Track getTrack() {
		return track;
	}

	public void setTrack(Track track) {
		this.track = track;
	}

	public GpxDisplayGroup cloneInstance() {
		GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
		group.type = type;
		group.name = name;
		group.description = description;
		group.track = track;
		group.list = new ArrayList<>(list);
		return group;
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

	public List<GpxDisplayItem> getModifiableList() {
		return list;
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

	public void noSplit(OsmandApplication app) {
		list.clear();
		splitDistance = -1;
		splitTime = -1;
		processGroupTrack(app, this);
	}

	public void splitByDistance(OsmandApplication app, double meters, boolean joinSegments) {
		list.clear();
		splitDistance = meters;
		splitTime = -1;
		processGroupTrack(app, this, joinSegments);
	}

	public void splitByTime(OsmandApplication app, int seconds, boolean joinSegments) {
		list.clear();
		splitDistance = -1;
		splitTime = seconds;
		processGroupTrack(app, this, joinSegments);
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
