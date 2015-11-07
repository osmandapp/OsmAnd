package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.util.Algorithms;

public class ParkingPositionMenuController extends MenuController {

	private ParkingPositionPlugin plugin;
	private String parkingDescription = "";

	public ParkingPositionMenuController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
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
		return getPointDescription().getName();
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
