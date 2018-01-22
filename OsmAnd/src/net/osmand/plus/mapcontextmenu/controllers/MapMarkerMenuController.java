package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
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
		final OsmandApplication app = mapActivity.getMyApplication();
		final boolean useStateList = Build.VERSION.SDK_INT >= 21;
		this.mapMarker = mapMarker;
		builder.setShowNearestWiki(true);

		final MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				markersHelper.moveMapMarkerToHistory(getMapMarker());
				getMapActivity().getContextMenu().close();
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.mark_passed);
		leftTitleButtonController.leftIcon = useStateList ? createStateListPassedIcon()
				: createPassedIcon(getPassedIconBgNormalColorId(), 0);

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				OsmandSettings.OsmandPreference<Boolean> indication = app.getSettings().MARKERS_DISTANCE_INDICATION_ENABLED;
				if (!indication.get()) {
					indication.set(true);
					getMapActivity().getMapLayers().getMapWidgetRegistry().updateMapMarkersMode(getMapActivity());
				}
				markersHelper.moveMarkerToTop(getMapMarker());
				getMapActivity().getContextMenu().close();
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.make_active);
		rightTitleButtonController.leftIcon = useStateList ? createStateListShowOnTopbarIcon()
				: createShowOnTopbarIcon(getDeviceTopNormalColorId(), R.color.dashboard_blue);
	}

	private int getPassedIconBgNormalColorId() {
		return isLight() ? R.color.map_widget_blue : R.color.osmand_orange;
	}

	private StateListDrawable createStateListPassedIcon() {
		int bgPressed = isLight() ? R.color.ctx_menu_controller_button_text_color_light_p
				: R.color.ctx_menu_controller_button_text_color_dark_p;
		int icPressed = isLight() ? R.color.ctx_menu_controller_button_text_color_light_n
				: R.color.ctx_menu_controller_button_bg_color_dark_p;
		return AndroidUtils.createStateListDrawable(createPassedIcon(getPassedIconBgNormalColorId(), 0),
				createPassedIcon(bgPressed, icPressed));
	}

	private LayerDrawable createPassedIcon(int bgColorRes, int icColorRes) {
		ShapeDrawable bg = new ShapeDrawable(new OvalShape());
		bg.getPaint().setColor(ContextCompat.getColor(getMapActivity(), bgColorRes));
		Drawable ic = getIcon(R.drawable.ic_action_marker_passed, icColorRes);
		return new LayerDrawable(new Drawable[]{bg, ic});
	}

	private int getDeviceTopNormalColorId() {
		return isLight() ? R.color.on_map_icon_color : R.color.ctx_menu_info_text_dark;
	}

	private StateListDrawable createStateListShowOnTopbarIcon() {
		int bgPressed = isLight() ? R.color.ctx_menu_controller_button_text_color_light_p
				: R.color.ctx_menu_controller_button_text_color_dark_p;
		int icPressed = isLight() ? R.color.osmand_orange : R.color.route_info_go_btn_bg_dark_p;
		return AndroidUtils.createStateListDrawable(createShowOnTopbarIcon(getDeviceTopNormalColorId(), R.color.dashboard_blue),
				createShowOnTopbarIcon(bgPressed, icPressed));
	}

	private LayerDrawable createShowOnTopbarIcon(int bgColorRes, int icColorRes) {
		Drawable background = getIcon(R.drawable.ic_action_device_top, bgColorRes);
		Drawable topbar = getIcon(R.drawable.ic_action_device_topbar, icColorRes);
		return new LayerDrawable(new Drawable[]{background, topbar});
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
	public Drawable getRightIcon() {
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
}