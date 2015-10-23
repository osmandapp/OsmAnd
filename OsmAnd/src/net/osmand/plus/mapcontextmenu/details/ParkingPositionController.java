package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.mapcontextmenu.MenuController;

public class ParkingPositionController extends MenuController {

	private PointDescription pointDescription;
	private LatLon latLon;

	public ParkingPositionController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription, LatLon latLon) {
		super(new ParkingPositionBuilder(app), mapActivity);
		this.pointDescription = pointDescription;
		this.latLon = latLon;
	}

	@Override
	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(R.drawable.ic_action_parking_dark, R.color.map_widget_blue, R.color.osmand_orange);
	}

	@Override
	public String getNameStr() {
		return pointDescription.getTypeName();
	}

	@Override
	public String getTypeStr() {
		return "Parked at 10:23";
	}

	@Override
	public boolean hasTitleButton() {
		return true;
	}

	@Override
	public String getTitleButtonCaption() {
		return getMapActivity().getText(R.string.osmand_parking_delete).toString();
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription) {
		if (pointDescription != null) {
			addPlainMenuItem(R.drawable.map_my_location, PointDescription.getLocationName(getMapActivity(),
					latLon.getLatitude(), latLon.getLongitude(), true).replaceAll("\n", ""));
		}
	}

	@Override
	public void saveEntityState(Bundle bundle, String key) {
	}
}
