package net.osmand.plus.measurementtool.command;

import androidx.annotation.NonNull;

import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public abstract class MeasurementModeCommand implements Command {

	MeasurementToolLayer measurementLayer;

	public MeasurementModeCommand(@NonNull MeasurementToolLayer measurementLayer) {
		this.measurementLayer = measurementLayer;
	}

	void setMeasurementLayer(MeasurementToolLayer layer) {
		this.measurementLayer = layer;
	}

	@Override
	public boolean update(@NonNull Command command) {
		return false;
	}

	@NonNull
	public abstract MeasurementCommandType getType();

	MeasurementEditingContext getEditingCtx() {
		return measurementLayer.getEditingCtx();
	}

	void refreshMap() {
		measurementLayer.refreshMap();
	}

	public enum MeasurementCommandType {
		ADD_POINT,
		CLEAR_POINTS,
		MOVE_POINT,
		REMOVE_POINT,
		REORDER_POINT,
		SNAP_TO_ROAD,
		CHANGE_ROUTE_MODE,
		APPROXIMATE_POINTS,
		REVERSE_POINTS,
		SPLIT_POINTS,
		JOIN_POINTS,
		DISABLE_APPROXIMATION_CHECK
	}
}
