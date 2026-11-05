package net.osmand.plus.measurementtool.command;

import androidx.annotation.NonNull;

import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.List;

public class RemovePointCommand extends MeasurementModeCommand {

	private final int position;
	private WptPt point;
	private String prevPointProfile;

	public RemovePointCommand(@NonNull MeasurementToolLayer measurementLayer, int position) {
		super(measurementLayer);
		this.position = position;
	}

	@Override
	public boolean execute() {
		List<WptPt> points = getEditingCtx().getPoints();
		if (position > 0 && position <= points.size()) {
			prevPointProfile = points.get(position - 1).getProfileType();
		}
		point = getEditingCtx().removePoint(position, true);
		refreshMap();
		return true;
	}

	@Override
	public void undo() {
		List<WptPt> points = getEditingCtx().getPoints();
		if (position > 0 && position <= points.size()) {
			WptPt prevPt = points.get(position - 1);
			if (prevPointProfile != null) {
				prevPt.setProfileType(prevPointProfile);
			} else {
				prevPt.removeProfileType();
			}
		}
		getEditingCtx().addPoint(position, point);
		refreshMap();
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	public void redo() {
		getEditingCtx().removePoint(position, true);
		refreshMap();
	}

	@NonNull
	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.REMOVE_POINT;
	}
}
