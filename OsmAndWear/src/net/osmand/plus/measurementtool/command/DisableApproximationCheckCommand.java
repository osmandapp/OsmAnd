package net.osmand.plus.measurementtool.command;

import androidx.annotation.NonNull;

import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class DisableApproximationCheckCommand extends MeasurementModeCommand {

	private final boolean shouldCheckApproximation;

	public DisableApproximationCheckCommand(@NonNull MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
		shouldCheckApproximation = getEditingCtx().shouldCheckApproximation();
	}

	@Override
	public boolean execute() {
		getEditingCtx().setShouldCheckApproximation(false);
		return true;
	}

	@Override
	public void undo() {
		getEditingCtx().setShouldCheckApproximation(shouldCheckApproximation);
	}

	@Override
	public void redo() {
		getEditingCtx().setShouldCheckApproximation(false);
	}

	@NonNull
	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.DISABLE_APPROXIMATION_CHECK;
	}
}
