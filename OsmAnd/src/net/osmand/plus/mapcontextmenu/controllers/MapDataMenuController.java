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

	public MapDataMenuController(OsmandApplication app, MapActivity mapActivity, final BinaryMapDataObject dataObject) {
		super(new MenuBuilder(app), mapActivity);
		OsmandRegions osmandRegions = app.getRegions();
		String fullName = osmandRegions.getFullName(dataObject);
		final WorldRegion region = osmandRegions.getRegionData(fullName);
		this.region = region;
		titleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				getMapActivity().getContextMenu().close();

				final Intent intent = new Intent(getMapActivity(), getMapActivity().getMyApplication()
						.getAppCustomization().getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.FILTER_KEY, region.getLocaleName());
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				getMapActivity().startActivity(intent);
			}
		};
		titleButtonController.caption = getMapActivity().getString(R.string.shared_string_download_map);
		titleButtonController.leftIconId = R.drawable.ic_action_import;
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
		return region.getLocaleName();
	}

	@Override
	public String getTypeStr() {
		if (region.getSuperregion() != null) {
			return region.getSuperregion().getLocaleName() + "\n";
		} else {
			return getMapActivity().getString(R.string.shared_string_map) + "\n";
		}
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
