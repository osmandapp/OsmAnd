package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.LinkedList;
import java.util.List;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.*;

public class ChangeRouteModeCommand extends MeasurementModeCommand {

	private List<WptPt> points;
	int pointIdx;
	ApplicationMode oldMode;
	ApplicationMode newMode;
	CalculationMode oldCalculationMode;
	CalculationMode newCalculationMode;


	public ChangeRouteModeCommand(MeasurementToolLayer measurementLayer, ApplicationMode newMode,
	                              CalculationMode newCalculationMode) {
		super(measurementLayer);
		this.newMode = newMode;
		this.newCalculationMode = newCalculationMode;
		MeasurementEditingContext editingCtx = measurementLayer.getEditingCtx();
		oldMode = editingCtx.getSnapToRoadAppMode();
		oldCalculationMode = editingCtx.getCalculationMode();
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext editingCtx = measurementLayer.getEditingCtx();
		points = new LinkedList<>(editingCtx.getPoints());
		pointIdx = points.size() - 1;
		if (pointIdx > 0 && newCalculationMode != CalculationMode.WHOLE_TRACK) {
			if (newMode != null) {
				points.get(pointIdx).setProfileType(newMode.getStringKey());
			} else {
				points.get(pointIdx).removeProfileType();
			}
		}
		editingCtx.setCalculationMode(newCalculationMode);
		editingCtx.setInSnapToRoadMode(true);
		editingCtx.setSnapToRoadAppMode(newMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
		editingCtx.setNeedUpdateCacheForSnap(true);
		return true;
	}

	@Override
	public void undo() {
		MeasurementEditingContext editingCtx = measurementLayer.getEditingCtx();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(points);
		editingCtx.setSnapToRoadAppMode(oldMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
		editingCtx.setCalculationMode(oldCalculationMode);
		editingCtx.setInSnapToRoadMode(true);
		editingCtx.setNeedUpdateCacheForSnap(true);
	}

	@Override
	public void redo() {
		MeasurementEditingContext editingCtx = measurementLayer.getEditingCtx();
		if (pointIdx > 0) {
			if (newMode != null) {
				points.get(pointIdx).setProfileType(newMode.getStringKey());
			} else {
				points.get(pointIdx).removeProfileType();
			}
		}
		editingCtx.setInSnapToRoadMode(true);
		editingCtx.setCalculationMode(newCalculationMode);
		editingCtx.setSnapToRoadAppMode(newMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
		editingCtx.setNeedUpdateCacheForSnap(true);
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.CHANGE_ROUTE_MODE;
	}
}
