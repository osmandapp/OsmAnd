package net.osmand.plus.measurementtool.command;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.DEFAULT_APP_MODE;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeRouteModeCommand extends MeasurementModeCommand {

	private List<WptPt> oldPoints;
	private List<WptPt> newPoints;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> oldRoadSegmentData;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> newRoadSegmentData;
	private final ApplicationMode oldMode;
	private final ApplicationMode newMode;
	private final ChangeRouteType changeRouteType;
	private final int pointIndex;

	public enum ChangeRouteType {
		LAST_SEGMENT,
		WHOLE_ROUTE,
		NEXT_SEGMENT,
		ALL_NEXT_SEGMENTS,
		PREV_SEGMENT,
		ALL_PREV_SEGMENTS
	}

	public ChangeRouteModeCommand(@NonNull MeasurementToolLayer measurementLayer, ApplicationMode newMode,
								  ChangeRouteType changeRouteType, int pointIndex) {
		super(measurementLayer);
		this.newMode = newMode;
		this.changeRouteType = changeRouteType;
		this.pointIndex = pointIndex;
		this.oldMode = getEditingCtx().getAppMode();
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		oldPoints = new ArrayList<>(editingCtx.getPoints());
		oldRoadSegmentData = editingCtx.getRoadSegmentData();
		newPoints = new ArrayList<>(oldPoints.size());
		newRoadSegmentData = new HashMap<>(oldRoadSegmentData);
		if (oldPoints.size() > 0) {
			for (WptPt pt : oldPoints) {
				WptPt point = new WptPt(pt);
				point.copyExtensions(pt);
				newPoints.add(point);
			}
			switch (changeRouteType) {
				case LAST_SEGMENT: {
					updateProfileType(newPoints.get(newPoints.size() - 1));
					editingCtx.setLastCalculationMode(CalculationMode.NEXT_SEGMENT);
					newRoadSegmentData = null;
					break;
				}
				case WHOLE_ROUTE: {
					for (WptPt pt : newPoints) {
						updateProfileType(pt);
					}
					editingCtx.setLastCalculationMode(CalculationMode.WHOLE_TRACK);
					newRoadSegmentData.clear();
					break;
				}
				case NEXT_SEGMENT: {
					if (pointIndex >= 0 && pointIndex < newPoints.size()) {
						updateProfileType(newPoints.get(pointIndex));
					}
					newRoadSegmentData.remove(getPairAt(pointIndex));
					break;
				}
				case ALL_NEXT_SEGMENTS: {
					for (int i = pointIndex; i >= 0 && i < newPoints.size(); i++) {
						updateProfileType(newPoints.get(i));
						newRoadSegmentData.remove(getPairAt(i));
					}
					break;
				}
				case PREV_SEGMENT: {
					if (pointIndex > 0 && pointIndex < newPoints.size()) {
						updateProfileType(newPoints.get(pointIndex - 1));
						newRoadSegmentData.remove(getPairAt(pointIndex - 1));
					}
					break;
				}
				case ALL_PREV_SEGMENTS: {
					for (int i = 0; i < pointIndex && i < newPoints.size(); i++) {
						updateProfileType(newPoints.get(i));
						newRoadSegmentData.remove(getPairAt(i));
					}
					break;
				}
			}
		}
		executeCommand();
		return true;
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
		return MeasurementCommandType.CHANGE_ROUTE_MODE;
	}

	private Pair<WptPt, WptPt> getPairAt(int pointIndex) {
		WptPt first = pointIndex >= 0 && pointIndex < newPoints.size() ? newPoints.get(pointIndex) : null;
		WptPt second = pointIndex >= 0 && pointIndex < newPoints.size() - 1 ? newPoints.get(pointIndex + 1) : null;
		return new Pair<>(first, second);
	}

	private void executeCommand() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(newPoints);
		if (newPoints.isEmpty()) {
			editingCtx.setAppMode(newMode);
		} else {
			WptPt lastPoint = newPoints.get(newPoints.size() - 1);
			editingCtx.setAppMode(ApplicationMode.valueOfStringKey(lastPoint.getProfileType(), DEFAULT_APP_MODE));
		}
		if (newRoadSegmentData != null) {
			editingCtx.setRoadSegmentData(newRoadSegmentData);
		}
		editingCtx.updateSegmentsForSnap();
	}

	private void updateProfileType(WptPt pt) {
		if (!pt.isGap()) {
			if (newMode != null && newMode != DEFAULT_APP_MODE) {
				pt.setProfileType(newMode.getStringKey());
			} else {
				pt.removeProfileType();
			}
		}
	}
}
