package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class MapMarkerMenuController extends MenuController {

	private MapMarker mapMarker;

	public MapMarkerMenuController(MapActivity mapActivity, PointDescription pointDescription, MapMarker mapMarker) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.mapMarker = mapMarker;
		builder.setShowNearestWiki(true);
		createMarkerButtons(this, mapActivity, mapMarker, getShowOnTopBarIcon());
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof MapMarker) {
			this.mapMarker = (MapMarker) object;
		}
	}

	@Override
	protected Object getObject() {
		return mapMarker;
	}

	public MapMarker getMapMarker() {
		return mapMarker;
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

	@Override
	public int getWaypointActionIconId() {
		return R.drawable.map_action_edit_dark;
	}

	@Override
	public int getWaypointActionStringId() {
		return R.string.rename_marker;
	}

	public static void createMarkerButtons(MenuController menuController, final MapActivity mapActivity, final MapMarker mapMarker, Drawable leftIcon) {
		final MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();

		TitleButtonController leftTitleButtonController = menuController.new TitleButtonController() {
			@Override
			public void buttonPressed() {
				markersHelper.moveMapMarkerToHistory(mapMarker);
				mapActivity.getContextMenu().close();
			}
		};
		leftTitleButtonController.needColorizeIcon = false;
		leftTitleButtonController.caption = mapActivity.getString(R.string.mark_passed);
		leftTitleButtonController.leftIconId = menuController.isLight() ? R.drawable.passed_icon_light : R.drawable.passed_icon_dark;
		menuController.setLeftTitleButtonController(leftTitleButtonController);

		TitleButtonController leftSubtitleButtonController = menuController.new TitleButtonController() {
			@Override
			public void buttonPressed() {
				markersHelper.moveMarkerToTop(mapMarker);
				mapActivity.getContextMenu().close();
			}
		};
		leftSubtitleButtonController.caption = mapActivity.getString(R.string.show_on_top_bar);
		leftSubtitleButtonController.leftIcon = leftIcon;
		menuController.setLeftSubtitleButtonController(leftSubtitleButtonController);
	}
}