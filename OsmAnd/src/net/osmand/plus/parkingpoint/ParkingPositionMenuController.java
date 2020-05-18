package net.osmand.plus.parkingpoint;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class ParkingPositionMenuController extends MenuController {

	private ParkingPositionPlugin plugin;
	private String parkingStartDescription = "";
	private String parkingLeftDescription = "";
	private String parkingTitle = "";

	public ParkingPositionMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		plugin = OsmandPlugin.getPlugin(ParkingPositionPlugin.class);
		if (plugin != null) {
			buildParkingDescription(mapActivity);
		}
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (plugin != null && activity != null) {
					plugin.showDeleteDialog(activity);
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_delete);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
	}

	private void buildParkingDescription(MapActivity mapActivity) {
		parkingStartDescription = plugin.getParkingStartDesc(mapActivity);
		parkingLeftDescription = plugin.getParkingLeftDesc(mapActivity);
		parkingTitle = plugin.getParkingTitle(mapActivity);
	}

	@Override
	protected void setObject(Object object) {
		MapActivity mapActivity = getMapActivity();
		if (plugin != null && mapActivity != null) {
			buildParkingDescription(mapActivity);
		}
	}

	@Override
	protected Object getObject() {
		return getLatLon();
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
	public int getAdditionalInfoIconRes() {
		return R.drawable.ic_action_opening_hour_16;
	}

	@Override
	public CharSequence getAdditionalInfoStr() {
		return parkingLeftDescription;
	}

	@Override
	public int getAdditionalInfoColorId() {
		return plugin.getParkingType() ? R.color.ctx_menu_amenity_closed_text_color : R.color.icon_color_default_light;
	}

	@NonNull
	@Override
	public String getNameStr() {
		return parkingTitle;
	}

	@Override
	public boolean navigateInPedestrianMode() {
		return true;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {

		return getIcon( plugin == null || plugin.getParkingTime() <= 0 ?
				R.drawable.mx_parking : R.drawable.mx_special_parking_time_limited, R.color.map_widget_blue);
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return parkingStartDescription;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
