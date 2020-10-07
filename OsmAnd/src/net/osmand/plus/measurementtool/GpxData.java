package net.osmand.plus.measurementtool;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.data.QuadRect;

public class GpxData {

	public enum ActionType {
		ADD_SEGMENT,
		ADD_ROUTE_POINTS,
		EDIT_SEGMENT,
		OVERWRITE_SEGMENT
	}

	private GPXFile gpxFile;
	private TrkSegment trkSegment;
	private QuadRect rect;
	private ActionType actionType;

	public GpxData(GPXFile gpxFile, QuadRect rect, ActionType actionType, TrkSegment trkSegment) {
		this.gpxFile = gpxFile;
		this.rect = rect;
		this.actionType = actionType;
		this.trkSegment = trkSegment;
	}

	public GpxData(GPXFile gpxFile, GpxData gpxData) {
		this.gpxFile = gpxFile;
		this.rect = gpxData.rect;
		this.actionType = gpxData.actionType;
		this.trkSegment = gpxData.trkSegment;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public QuadRect getRect() {
		return rect;
	}

	public ActionType getActionType() {
		return actionType;
	}

	public TrkSegment getTrkSegment() {
		return trkSegment;
	}
}
