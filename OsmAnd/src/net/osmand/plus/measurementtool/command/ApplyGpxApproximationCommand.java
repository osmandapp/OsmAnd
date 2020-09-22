package net.osmand.plus.measurementtool.command;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;

import java.util.ArrayList;
import java.util.List;

public class ApplyGpxApproximationCommand extends MeasurementModeCommand {

	private ApplicationMode mode;
	private GpxRouteApproximation approximation;
	private List<WptPt> points;

	public ApplyGpxApproximationCommand(MeasurementToolLayer measurementLayer, GpxRouteApproximation approximation, ApplicationMode mode) {
		super(measurementLayer);
		this.approximation = approximation;
		this.mode = mode;
	}

	public List<WptPt> getPoints() {
		return points;
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.APPROXIMATE_POINTS;
	}

	@Override
	public boolean execute() {
		List<WptPt> pts = getEditingCtx().getPoints();
		points = new ArrayList<>(pts);
		applyApproximation();
		refreshMap();
		return true;
	}

	@Override
	public boolean update(@NonNull Command command) {
		if (command instanceof ApplyGpxApproximationCommand) {
			ApplyGpxApproximationCommand approxCommand = (ApplyGpxApproximationCommand) command;
			approximation = approxCommand.approximation;
			mode = approxCommand.mode;
			applyApproximation();
			refreshMap();
			return true;
		}
		return false;
	}

	@Override
	public void undo() {
		getEditingCtx().resetAppMode();
		getEditingCtx().clearSegments();
		getEditingCtx().addPoints(points);
		getEditingCtx().updateCacheForSnap();
		refreshMap();
	}

	@Override
	public void redo() {
		applyApproximation();
		refreshMap();
	}

	public void applyApproximation() {
		getEditingCtx().setAppMode(mode);
		getEditingCtx().clearSegments();
		getEditingCtx().setPoints(approximation, mode);
	}
}
