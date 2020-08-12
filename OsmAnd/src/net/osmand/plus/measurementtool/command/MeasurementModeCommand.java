package net.osmand.plus.measurementtool.command;

import net.osmand.plus.measurementtool.MeasurementToolLayer;

public abstract class MeasurementModeCommand implements Command {

	MeasurementToolLayer measurementLayer;

	void setMeasurementLayer(MeasurementToolLayer layer) {
		this.measurementLayer = layer;
	}

	abstract MeasurementCommandType getType();

	public enum MeasurementCommandType {
		ADD_POINT,
		CLEAR_POINTS,
		MOVE_POINT,
		REMOVE_POINT,
		REORDER_POINT,
		SNAP_TO_ROAD,
		CHANGE_ROUTE_MODE
	}
}
