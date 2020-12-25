package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClearPointsCommand extends MeasurementModeCommand {

	private List<WptPt> points;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData;
	private ClearCommandMode clearMode;
	private int pointPosition;

	public enum ClearCommandMode {
		ALL,
		BEFORE,
		AFTER
	}

	public ClearPointsCommand(MeasurementToolLayer measurementLayer, ClearCommandMode clearMode) {
		super(measurementLayer);
		this.clearMode = clearMode;
	}

	@Override
	public boolean execute() {
		pointPosition = getEditingCtx().getSelectedPointPosition();
		executeCommand();
		return true;
	}

	private void executeCommand() {
		MeasurementEditingContext ctx = getEditingCtx();
		List<WptPt> pts = ctx.getPoints();
		points = new ArrayList<>(pts);
		roadSegmentData = ctx.getRoadSegmentData();
		switch (clearMode) {
			case ALL:
				pts.clear();
				ctx.clearSegments();
				break;
			case BEFORE:
				ctx.trimBefore(pointPosition);
				break;
			case AFTER:
				ctx.trimAfter(pointPosition);
		}
		refreshMap();
	}

	@Override
	public void undo() {
		MeasurementEditingContext ctx = getEditingCtx();
		ctx.clearSegments();
		ctx.setRoadSegmentData(roadSegmentData);
		ctx.addPoints(points);
		refreshMap();
	}

	@Override
	public void redo() {
		executeCommand();
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.CLEAR_POINTS;
	}
}
