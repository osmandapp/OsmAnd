package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class RemovePointCommand extends MeasurementModeCommand {

	private final int position;
	private WptPt point;
	private String prevPointProfile;

	public RemovePointCommand(MeasurementToolLayer measurementLayer, int position) {
		super(measurementLayer);
		this.position = position;
	}

	@Override
	public boolean execute() {
		if (position > 0) {
			prevPointProfile = getEditingCtx().getPoints().get(position - 1).getProfileType();
		}
		point = getEditingCtx().removePoint(position, true);
		refreshMap();
		return true;
	}

	@Override
	public void undo() {
		if (position > 0) {
			WptPt prevPt = getEditingCtx().getPoints().get(position - 1);
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

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.REMOVE_POINT;
	}
}
