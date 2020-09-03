package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementEditingContext.RoadSegmentData;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.DEFAULT_APP_MODE;

public class ReversePointsCommand extends MeasurementModeCommand {

	private List<WptPt> oldPoints;
	private List<WptPt> newPoints;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> oldRoadSegmentData;
	private ApplicationMode oldMode;

	public ReversePointsCommand(MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
		this.oldMode = getEditingCtx().getAppMode();
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		oldPoints = new ArrayList<>(editingCtx.getPoints());
		oldRoadSegmentData = editingCtx.getRoadSegmentData();
		newPoints = new ArrayList<>(oldPoints);
		Collections.reverse(newPoints);
		executeCommand();
		return true;
	}

	private void executeCommand() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.clearSnappedToRoadPoints();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(newPoints);
		if (!newPoints.isEmpty()) {
			WptPt lastPoint = newPoints.get(newPoints.size() - 1);
			editingCtx.setAppMode(ApplicationMode.valueOfStringKey(lastPoint.getProfileType(), DEFAULT_APP_MODE));
		}
		editingCtx.updateCacheForSnap();
	}

	@Override
	public void undo() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(oldPoints);
		editingCtx.setAppMode(oldMode);
		editingCtx.setRoadSegmentData(oldRoadSegmentData);
		editingCtx.updateCacheForSnap();

	}

	@Override
	public void redo() {
		executeCommand();
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.REVERSE_POINTS;
	}
}
