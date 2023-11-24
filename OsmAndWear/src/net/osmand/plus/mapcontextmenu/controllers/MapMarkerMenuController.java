package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.WidgetUtils;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

public class MapMarkerMenuController extends MenuController {

	private MapMarker mapMarker;

	public MapMarkerMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull MapMarker mapMarker) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
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
		int activeColorId = ColorUtilities.getActiveColorId(!isLight());
		leftTitleButtonController.caption = mapActivity.getString(mapMarker.history ? R.string.shared_string_restore : R.string.mark_passed);
		leftTitleButtonController.startIcon = createPassedIcon(activeColorId);

		if (!mapMarker.history) {
			rightTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						OsmandApplication app = activity.getMyApplication();
						MapMarkersHelper markersHelper = app.getMapMarkersHelper();
						markersHelper.moveMarkerToTop(getMapMarker());
						activity.getContextMenu().close();
					}
				}
			};
			rightTitleButtonController.caption = mapActivity.getString(R.string.make_active);
			rightTitleButtonController.startIcon = createShowOnTopbarIcon(ColorUtilities.getDefaultIconColorId(!isLight()));
		}
	}

	@Nullable
	private LayerDrawable createPassedIcon(int bgColorRes) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ShapeDrawable bg = new ShapeDrawable(new OvalShape());
			bg.getPaint().setColor(ContextCompat.getColor(mapActivity, bgColorRes));
			Drawable ic = getIcon(R.drawable.ic_action_marker_passed, 0);
			return new LayerDrawable(new Drawable[] {bg, ic});
		} else {
			return null;
		}
	}

	private LayerDrawable createShowOnTopbarIcon(int bgColorRes) {
		Drawable background = getIcon(R.drawable.ic_action_device_top, bgColorRes);
		Drawable topbar = getIcon(R.drawable.ic_action_device_topbar, R.color.active_color_primary_light);
		return new LayerDrawable(new Drawable[] {background, topbar});
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