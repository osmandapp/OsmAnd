package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.DEFAULT_APP_MODE;

public class ChangeRouteModeCommand extends MeasurementModeCommand {

	private List<WptPt> oldPoints;
	private List<WptPt> newPoints;
	private ApplicationMode oldMode;
	private ApplicationMode newMode;
	private CalculationMode oldCalculationMode;
	private CalculationMode newCalculationMode;

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
		oldPoints = new ArrayList<>(editingCtx.getPoints());
		newPoints = new ArrayList<>(oldPoints.size());
		if (oldPoints.size() > 0) {
			for (WptPt pt : oldPoints) {
				WptPt point = new WptPt(pt);
				point.copyExtensions(pt);
				newPoints.add(point);
			}
			switch (newCalculationMode) {
				case NEXT_SEGMENT:
					updateProfileType(newPoints.get(newPoints.size() - 1));
					break;
				case WHOLE_TRACK:
					for (WptPt pt : newPoints) {
						updateProfileType(pt);
					}
					break;
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
		editingCtx.setCalculationMode(oldCalculationMode);
		editingCtx.setAppMode(oldMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
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
		editingCtx.getPoints().clear();
		editingCtx.addPoints(newPoints);
		editingCtx.setCalculationMode(newCalculationMode);
		editingCtx.setAppMode(newMode);
		if (newCalculationMode == CalculationMode.WHOLE_TRACK) {
			editingCtx.clearSnappedToRoadPoints();
		}
		editingCtx.updateCacheForSnap();
	}

	private void updateProfileType(WptPt pt) {
		if (newMode != null && newMode != DEFAULT_APP_MODE) {
			pt.setProfileType(newMode.getStringKey());
		} else {
			pt.removeProfileType();
		}
	}
}
