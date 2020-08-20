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
		MeasurementEditingContext editingCtx = getEditingCtx();
		oldMode = editingCtx.getAppMode();
		oldCalculationMode = editingCtx.getCalculationMode();
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		points = new LinkedList<>(editingCtx.getPoints());
		pointIdx = points.size() - 1;
		executeCommand();
		return true;
	}

	@Override
	public void undo() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(points);
		editingCtx.setAppMode(oldMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
		editingCtx.setCalculationMode(oldCalculationMode);
		editingCtx.updateCacheForSnap();
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
		if (pointIdx > 0 && newCalculationMode != CalculationMode.WHOLE_TRACK) {
			if (newMode != null) {
				points.get(pointIdx).setProfileType(newMode.getStringKey());
			} else {
				points.get(pointIdx).removeProfileType();
			}
		}
		editingCtx.setCalculationMode(newCalculationMode);
		editingCtx.setAppMode(newMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
		editingCtx.updateCacheForSnap();
	}
}
