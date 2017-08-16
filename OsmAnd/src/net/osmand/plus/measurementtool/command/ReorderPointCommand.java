package net.osmand.plus.measurementtool.command;

import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.Collections;

public class ReorderPointCommand extends MeasurementModeCommand {

	private final int from;
	private final int to;

	public ReorderPointCommand(MeasurementToolLayer measurementLayer, int from, int to) {
		this.measurementLayer = measurementLayer;
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean execute() {
		return true;
	}

	@Override
	public void undo() {
		swap();
	}

	@Override
	public void redo() {
		swap();
	}

	private void swap() {
		Collections.swap(measurementLayer.getMeasurementPoints(), from, to);
		measurementLayer.refreshMap();
	}
}
