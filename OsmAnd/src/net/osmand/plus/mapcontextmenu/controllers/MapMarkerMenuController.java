package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class MapMarkerMenuController extends MenuController {

	private MapMarker mapMarker;

	public MapMarkerMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull MapMarker mapMarker) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		final boolean useStateList = Build.VERSION.SDK_INT >= 21;
		this.mapMarker = mapMarker;
		builder.setShowNearestWiki(true);

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					MapMarkersHelper markersHelper = activity.getMyApplication().getMapMarkersHelper();
					MapMarker marker = getMapMarker();
					if (marker.history) {
						markersHelper.restoreMarkerFromHistory(marker, 0);
					} else {
						markersHelper.moveMapMarkerToHistory(marker);
					}
					activity.getContextMenu().close();
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(mapMarker.history ? R.string.shared_string_restore : R.string.mark_passed);
		leftTitleButtonController.leftIcon = useStateList ? createStateListPassedIcon()
				: createPassedIcon(getPassedIconBgNormalColorId(), 0);

		if (!mapMarker.history) {
			rightTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						OsmandSettings.OsmandPreference<Boolean> indication
								= activity.getMyApplication().getSettings().MARKERS_DISTANCE_INDICATION_ENABLED;
						if (!indication.get()) {
							indication.set(true);
							activity.getMapLayers().getMapWidgetRegistry().updateMapMarkersMode(activity);
						}
						MapMarkersHelper markersHelper = activity.getMyApplication().getMapMarkersHelper();
						markersHelper.moveMarkerToTop(getMapMarker());
						activity.getContextMenu().close();
					}
				}
			};
			rightTitleButtonController.caption = mapActivity.getString(R.string.make_active);
			rightTitleButtonController.leftIcon = useStateList ? createStateListShowOnTopbarIcon()
					: createShowOnTopbarIcon(getDeviceTopNormalColorId(), R.color.dashboard_blue);
		}
	}

	private int getPassedIconBgNormalColorId() {
		return isLight() ? R.color.map_widget_blue : R.color.osmand_orange;
	}

	@Nullable
	private StateListDrawable createStateListPassedIcon() {
		int bgPressed = isLight() ? R.color.ctx_menu_controller_button_text_color_light_p
				: R.color.ctx_menu_controller_button_text_color_dark_p;
		int icPressed = isLight() ? R.color.ctx_menu_controller_button_text_color_light_n
				: R.color.ctx_menu_controller_button_bg_color_dark_p;
		return AndroidUtils.createPressedStateListDrawable(createPassedIcon(getPassedIconBgNormalColorId(), 0),
				createPassedIcon(bgPressed, icPressed));
	}

	@Nullable
	private LayerDrawable createPassedIcon(int bgColorRes, int icColorRes) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ShapeDrawable bg = new ShapeDrawable(new OvalShape());
			bg.getPaint().setColor(ContextCompat.getColor(mapActivity, bgColorRes));
			Drawable ic = getIcon(R.drawable.ic_action_marker_passed, icColorRes);
			return new LayerDrawable(new Drawable[]{bg, ic});
		} else {
			return null;
		}
	}

	private int getDeviceTopNormalColorId() {
		return isLight() ? R.color.on_map_icon_color : R.color.ctx_menu_info_text_dark;
	}

	private StateListDrawable createStateListShowOnTopbarIcon() {
		int bgPressed = isLight() ? R.color.ctx_menu_controller_button_text_color_light_p
				: R.color.ctx_menu_controller_button_text_color_dark_p;
		int icPressed = isLight() ? R.color.osmand_orange : R.color.active_buttons_and_links_dark;
		return AndroidUtils.createPressedStateListDrawable(createShowOnTopbarIcon(getDeviceTopNormalColorId(), R.color.dashboard_blue),
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return MapMarkerDialogHelper.getMapMarkerIcon(mapActivity.getMyApplication(), mapMarker.colorIndex);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapMarker.getPointDescription(mapActivity).getTypeName();
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !needTypeStr();
	}

	@Override
	public int getWaypointActionIconId() {
		return R.drawable.ic_action_edit_dark;
	}

	@Override
	public int getWaypointActionStringId() {
		return R.string.shared_string_edit;
	}
}