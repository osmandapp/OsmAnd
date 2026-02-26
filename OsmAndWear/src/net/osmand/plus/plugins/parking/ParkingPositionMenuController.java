package net.osmand.plus.plugins.parking;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.views.PointImageUtils;

public class ParkingPositionMenuController extends MenuController {

	private final ParkingPositionPlugin plugin;
	private String parkingStartDescription = "";
	private String parkingLeftDescription = "";
	private String parkingTitle = "";
	private final FavouritePoint fav;

	public ParkingPositionMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription,
	                                     FavouritePoint fav) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.fav = fav;
		plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
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
		leftTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;
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
		return plugin.getParkingType() ? R.color.text_color_negative : R.color.icon_color_default_light;
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return PointImageUtils.getFromPoint(mapActivity.getMyApplication(),
					ContextCompat.getColor(mapActivity, R.color.parking_icon_background), false, fav);
		} else {
			return null;
		}
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
