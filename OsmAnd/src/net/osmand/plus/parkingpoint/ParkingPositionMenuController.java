package net.osmand.plus.parkingpoint;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class ParkingPositionMenuController extends MenuController {

	private ParkingPositionPlugin plugin;
	private String parkingDescription = "";

	public ParkingPositionMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		plugin = OsmandPlugin.getPlugin(ParkingPositionPlugin.class);
		if (plugin != null) {
			buildParkingDescription(mapActivity);
		}
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					plugin.showDeleteDialog(getMapActivity());
				}
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
	}

	private void buildParkingDescription(MapActivity mapActivity) {
		StringBuilder sb = new StringBuilder();
		sb.append(plugin.getParkingStartDesc(mapActivity));
		String leftDesc = plugin.getParkingLeftDesc(mapActivity);
		if (!Algorithms.isEmpty(leftDesc)) {
			sb.append("\n").append(leftDesc);
		}
		parkingDescription = sb.toString();
	}

	@Override
	protected void setObject(Object object) {
		if (plugin != null) {
			buildParkingDescription(getMapActivity());
		}
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
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(R.drawable.ic_action_parking_dark, R.color.map_widget_blue);
	}

	@Override
	public String getTypeStr() {
		return parkingDescription;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
