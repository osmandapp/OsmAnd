package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.util.Algorithms;

public class ParkingPositionController extends MenuController {

	private PointDescription pointDescription;
	private LatLon latLon;
	ParkingPositionPlugin plugin;
	String parkingDescription = "";

	public ParkingPositionController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription, LatLon latLon) {
		super(new MenuBuilder(app), mapActivity);
		this.pointDescription = pointDescription;
		this.latLon = latLon;
		plugin = OsmandPlugin.getPlugin(ParkingPositionPlugin.class);
		if (plugin != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(plugin.getParkingStartDesc(mapActivity));
			String leftDesc = plugin.getParkingLeftDesc(mapActivity);
			if (!Algorithms.isEmpty(leftDesc)) {
				sb.append("\n").append(leftDesc);
			}
			parkingDescription = sb.toString();
		}
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
		return !Algorithms.isEmpty(parkingDescription);
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
		return parkingDescription;
	}

	@Override
	public boolean hasTitleButton() {
		return true;
	}

	@Override
	public String getTitleButtonCaption() {
		return getMapActivity().getString(R.string.osmand_parking_delete);
	}

	@Override
	public void titleButtonPressed() {
		if (plugin != null) {
			plugin.showDeleteDialog(getMapActivity());
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void saveEntityState(Bundle bundle, String key) {
		bundle.putSerializable(key, latLon);
	}
}
