package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.*;

public class ChangeRouteModeCommand extends MeasurementModeCommand {

	private Map<WptPt, String> pointsProfiles;
	private List<WptPt> points;
	int pointIdx;
	ApplicationMode oldMode;
	ApplicationMode newMode;
	CalculationMode oldCalculationMode;
	CalculationMode newCalculationMode;
	Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData;

	public ChangeRouteModeCommand(MeasurementToolLayer measurementLayer, ApplicationMode newMode,
	                              CalculationMode newCalculationMode, CalculationMode oldCalculationMode) {
		super(measurementLayer);
		this.newMode = newMode;
		this.newCalculationMode = newCalculationMode;
		this.oldCalculationMode = oldCalculationMode;
		MeasurementEditingContext editingCtx = getEditingCtx();
		List<WptPt> pts = getEditingCtx().getPoints();
		points = new ArrayList<>(pts);
		pointsProfiles = new LinkedHashMap<>();
		for (WptPt point : points) {
			pointsProfiles.put(point, point.getProfileType());
		}
		roadSegmentData = new ConcurrentHashMap<>(editingCtx.getRoadSegmentData());
		pointIdx = editingCtx.getSelectedPointPosition();
		if (pointIdx < 0) {
			pointIdx = pointsProfiles.size() - 1;
		}
	}

	@Override
	public boolean execute() {
		executeCommand();
		return true;
	}

	@Override
	public void undo() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.getPoints().clear();
		List<WptPt> pts = new ArrayList<>();
		for (WptPt point : points) {
			point.setProfileType(pointsProfiles.get(point));
			pts.add(point);
		}
		editingCtx.addPoints(pts);
		editingCtx.clearSnappedToRoadPoints();
		editingCtx.setRoadSegmentData(roadSegmentData);
		editingCtx.setCalculationMode(oldCalculationMode);
		if (oldCalculationMode.isAll()) {
			editingCtx.setAppMode(oldMode);
		}
		editingCtx.setNeedUpdateCacheForSnap(true);
	}

	@Override
	public void redo() {
		executeCommand();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.CHANGE_ROUTE_MODE;
	}

	private void executeCommand() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		switch (newCalculationMode) {
			case ADD_NEXT_SEGMENT:
				oldMode = editingCtx.getAppMode();
				if (pointIdx > 0) {
					setProfileType(pointIdx);
				}
				editingCtx.setAppMode(newMode);
				break;
			case WHOLE_TRACK:
				for (int i = 0; i < pointsProfiles.size() - 1; i++) {
					setProfileType(i);
					editingCtx.getRoadSegmentData().remove(new Pair<>(points.get(i), points.get(i + 1)));
				}
				editingCtx.clearSnappedToRoadPoints();
				oldMode = editingCtx.getAppMode();
				editingCtx.setAppMode(newMode);
				break;
			case NEXT_SEGMENT:
				setProfileType(pointIdx);
				editingCtx.getRoadSegmentData().remove(new Pair<>(points.get(pointIdx), points.get(pointIdx + 1)));
				break;
			case ALL_NEXT_SEGMENTS:
				for (int i = pointIdx; i < pointsProfiles.size() - 1; i++) {
					setProfileType(i);
					editingCtx.getRoadSegmentData().remove(new Pair<>(points.get(i), points.get(i + 1)));
				}
				break;
			case PREVIOUS_SEGMENT:
				setProfileType(pointIdx - 1);
				editingCtx.getRoadSegmentData().remove(new Pair<>(points.get(pointIdx - 1), points.get(pointIdx)));
				break;
			case ALL_PREVIOUS_SEGMENTS:
				for (int i = 0; i < pointIdx - 1; i++) {
					setProfileType(i);
					editingCtx.getRoadSegmentData().remove(new Pair<>(points.get(i), points.get(i + 1)));
				}
				break;
		}
		editingCtx.setCalculationMode(newCalculationMode);
		editingCtx.setNeedUpdateCacheForSnap(true);
	}

	private void setProfileType(int pointIdx) {
		if (newMode != null) {
			points.get(pointIdx).setProfileType(newMode.getStringKey());
		} else {
			points.get(pointIdx).removeProfileType();
		}
	}
}
