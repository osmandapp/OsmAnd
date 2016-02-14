package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class MapMarkerMenuController extends MenuController {

	private MapMarker mapMarker;

	public MapMarkerMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, MapMarker mapMarker) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.mapMarker = mapMarker;
		final MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				markersHelper.removeMapMarker(getMapMarker().index);
				markersHelper.addMapMarkerHistory(getMapMarker());
				getMapActivity().getContextMenu().close();
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_remove);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof MapMarker) {
			this.mapMarker = (MapMarker) object;
		}
	}

	public MapMarker getMapMarker() {
		return mapMarker;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return MapMarkerDialogHelper.getMapMarkerIcon(getMapActivity().getMyApplication(), mapMarker.colorIndex);
	}

	@Override
	public String getTypeStr() {
		return mapMarker.getPointDescription(getMapActivity()).getTypeName();
	}

	@Override
	public boolean needStreetName() {
		return !needTypeStr();
	}
}