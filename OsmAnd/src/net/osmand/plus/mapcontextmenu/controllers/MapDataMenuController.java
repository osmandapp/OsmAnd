package net.osmand.plus.mapcontextmenu.controllers;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class MapDataMenuController extends MenuController {
	private WorldRegion region;
	private String regionName;

	public MapDataMenuController(OsmandApplication app, MapActivity mapActivity, final BinaryMapDataObject dataObject) {
		super(new MenuBuilder(app), mapActivity);
		OsmandRegions osmandRegions = app.getRegions();
		String fullName = osmandRegions.getFullName(dataObject);
		final WorldRegion region = osmandRegions.getRegionData(fullName);
		this.region = region;
		if (region != null) {
			regionName = region.getLocaleName();
		} else {
			regionName = dataObject.getName();
		}

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				getMapActivity().getContextMenu().close();

				final Intent intent = new Intent(getMapActivity(), getMapActivity().getMyApplication()
						.getAppCustomization().getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.FILTER_KEY, regionName);
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				getMapActivity().startActivity(intent);
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_download);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_import;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				// todo delete
				//getMapActivity().getContextMenu().close();
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		rightTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;

		topRightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				// todo other maps
			}
		};
		topRightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_others);

	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(R.drawable.ic_map, R.color.osmand_orange_dark, R.color.osmand_orange);
	}

	@Override
	public String getNameStr() {
		return regionName;
	}

	@Override
	public String getTypeStr() {
		String res;
		if (region != null && region.getSuperregion() != null) {
			res = region.getSuperregion().getLocaleName();
		} else {
			res = getMapActivity().getString(R.string.shared_string_map);
		}
		if (getMenuType() == MenuType.STANDARD) {
			res += "\n";
		}
		return res;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
	}

	@Override
	public boolean fabVisible() {
		return false;
	}

	@Override
	public boolean buttonsVisible() {
		return false;
	}
}
