package net.osmand.plus.measurementtool.command;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.DEFAULT_APP_MODE;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReversePointsCommand extends MeasurementModeCommand {

	private List<WptPt> oldPoints;
	private List<WptPt> newPoints;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> oldRoadSegmentData;
	private final ApplicationMode oldMode;

	public ReversePointsCommand(@NonNull MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
		this.oldMode = getEditingCtx().getAppMode();
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		oldPoints = new ArrayList<>(editingCtx.getPoints());
		oldRoadSegmentData = editingCtx.getRoadSegmentData();
		newPoints = new ArrayList<>(oldPoints.size());
		for (int i = oldPoints.size() - 1; i >= 0; i--) {
			WptPt point = oldPoints.get(i);
			WptPt prevPoint = i > 0 ? oldPoints.get(i - 1) : null;
			WptPt newPoint = new WptPt(point);
			newPoint.copyExtensions(point);
			if (prevPoint != null) {
				String profileType = prevPoint.getProfileType();
				if (profileType != null) {
					newPoint.setProfileType(profileType);
				} else {
					newPoint.removeProfileType();
				}
			}
			newPoints.add(newPoint);
		}
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
		editingCtx.updateSegmentsForSnap();
	}

	@Override
	public void undo() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(oldPoints);
		editingCtx.setAppMode(oldMode);
		editingCtx.setRoadSegmentData(oldRoadSegmentData);
		editingCtx.updateSegmentsForSnap();

	}

	@Override
	public void redo() {
		executeCommand();
	}

	@NonNull
	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.REVERSE_POINTS;
	}
}
