package net.osmand.plus.measurementtool.command;

import net.osmand.data.LatLon;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementEditingContext.AdditionMode;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.List;

public class AddPointCommand extends MeasurementModeCommand {

	private int position;
	private WptPt point;
	private String prevPointProfile;
	private boolean center;
	private boolean addPointBefore;

	public AddPointCommand(MeasurementToolLayer measurementLayer, boolean center) {
		super(measurementLayer);
		init(null, center);
	}

	public AddPointCommand(MeasurementToolLayer measurementLayer, LatLon latLon) {
		super(measurementLayer);
		init(latLon, false);
	}

	private void init(LatLon latLon, boolean center) {
		MeasurementEditingContext ctx = getEditingCtx();
		if (latLon != null) {
			point = new WptPt();
			point.lat = latLon.getLatitude();
			point.lon = latLon.getLongitude();
		}
		this.center = center;
		position = ctx.getPointsCount();
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext ctx = getEditingCtx();
		addPointBefore = ctx.isInAddPointBeforeMode();
		List<WptPt> points = ctx.getPoints();
		if (points.size() > 0) {
			WptPt prevPt = points.get(points.size() - 1);
			prevPointProfile = prevPt.getProfileType();
		}
		if (point != null) {
			ctx.addPoint(point, addPointBefore ? AdditionMode.ADD_BEFORE : AdditionMode.ADD_AFTER);
			measurementLayer.moveMapToPoint(position);
		} else if (center) {
			point = measurementLayer.addCenterPoint(addPointBefore);
		} else {
			point = measurementLayer.addPoint(addPointBefore);
		}
		refreshMap();
		return point != null;
	}

	@Override
	public void undo() {
		MeasurementEditingContext ctx = getEditingCtx();
		if (position > 0) {
			WptPt prevPt = ctx.getPoints().get(position - 1);
			if (prevPointProfile != null) {
				prevPt.setProfileType(prevPointProfile);
			} else {
				prevPt.removeProfileType();
			}
		}
		ctx.removePoint(position, true);
		refreshMap();
	}

	@Override
	public void redo() {
		getEditingCtx().addPoint(position, point, addPointBefore ? AdditionMode.ADD_BEFORE : AdditionMode.ADD_AFTER);
		refreshMap();
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.ADD_POINT;
	}
}
