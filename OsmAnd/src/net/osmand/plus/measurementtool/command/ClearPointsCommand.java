package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClearPointsCommand extends MeasurementModeCommand {

	private List<WptPt> points;
	private boolean needUpdateCache;

	public ClearPointsCommand(MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
	}

	@Override
	public boolean execute() {
		List<WptPt> pts = getEditingCtx().getPoints();
		needUpdateCache = getEditingCtx().isNeedUpdateCacheForSnap();
		points = new ArrayList<>(pts);
		pts.clear();
		getEditingCtx().clearSegments();
		refreshMap();
		return true;
	}

	@Override
	public void undo() {
		getEditingCtx().addPoints(points);
		if (needUpdateCache) {
			getEditingCtx().setNeedUpdateCacheForSnap(true);
		}
		refreshMap();
	}

	@Override
	public void redo() {
		getEditingCtx().clearSegments();
		refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.CLEAR_POINTS;
	}
}
