package net.osmand.plus.measurementtool.command;

import net.osmand.plus.measurementtool.MeasurementToolLayer;

abstract class MeasurementModeCommand implements Command {

	MeasurementToolLayer measurementLayer;

	void setMeasurementLayer(MeasurementToolLayer layer) {
		this.measurementLayer = layer;
	}
}
