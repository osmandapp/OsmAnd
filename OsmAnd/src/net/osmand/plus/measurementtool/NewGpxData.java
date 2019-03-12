package net.osmand.plus.measurementtool;

import net.osmand.data.QuadRect;
import net.osmand.GPXUtilities;

public class NewGpxData {

	public enum ActionType {
		ADD_SEGMENT,
		ADD_ROUTE_POINTS,
		EDIT_SEGMENT,
		OVERWRITE_SEGMENT
	}

	private GPXUtilities.GPXFile gpxFile;
	private GPXUtilities.TrkSegment trkSegment;
	private QuadRect rect;
	private ActionType actionType;

	public NewGpxData(GPXUtilities.GPXFile gpxFile, QuadRect rect, ActionType actionType, GPXUtilities.TrkSegment trkSegment) {
		this.gpxFile = gpxFile;
		this.rect = rect;
		this.actionType = actionType;
		this.trkSegment = trkSegment;
	}

	public GPXUtilities.GPXFile getGpxFile() {
		return gpxFile;
	}

	public QuadRect getRect() {
		return rect;
	}

	public ActionType getActionType() {
		return actionType;
	}

	public GPXUtilities.TrkSegment getTrkSegment() {
		return trkSegment;
	}
}
