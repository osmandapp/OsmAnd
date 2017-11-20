package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;

import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.WptPtMenuBuilder;
import net.osmand.util.Algorithms;

public class WptPtMenuController extends MenuController {

	private WptPt wpt;

	public WptPtMenuController(MapActivity mapActivity, PointDescription pointDescription, WptPt wpt) {
		super(new WptPtMenuBuilder(mapActivity, wpt), pointDescription, mapActivity);
		this.wpt = wpt;

		final MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		final MapMarker mapMarker = markersHelper.getMapMarker(wpt);

		if (mapMarker != null) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					markersHelper.moveMapMarkerToHistory(mapMarker);
					getMapActivity().getContextMenu().close();
				}
			};
			leftTitleButtonController.needColorizeIcon = false;
			leftTitleButtonController.caption = getMapActivity().getString(R.string.mark_passed);
			leftTitleButtonController.leftIconId = isLight() ? R.drawable.passed_icon_light : R.drawable.passed_icon_dark;

			leftSubtitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					markersHelper.moveMarkerToTop(mapMarker);
					getMapActivity().getContextMenu().close();
				}
			};
			leftSubtitleButtonController.caption = getMapActivity().getString(R.string.show_on_top_bar);
			leftSubtitleButtonController.leftIcon = createShowOnTopbarIcon();
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof WptPt) {
			this.wpt = (WptPt) object;
		}
	}

	@Override
	protected Object getObject() {
		return wpt;
	}

/*
	@Override
	public boolean handleSingleTapOnMap() {
		Fragment fragment = getMapActivity().getSupportFragmentManager().findFragmentByTag(FavoritePointEditor.TAG);
		if (fragment != null) {
			((FavoritePointEditorFragment)fragment).dismiss();
			return true;
		}
		return false;
	}
*/

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity().getMyApplication(),
				wpt.getColor(ContextCompat.getColor(getMapActivity(), R.color.gpx_color_point)), false);
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (Algorithms.isEmpty(getTypeStr())) {
			return null;
		} else {
			return getIcon(R.drawable.map_small_group);
		}
	}

	@Override
	public String getTypeStr() {
		return wpt.category != null ? wpt.category : "";
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.gpx_wpt);
	}

	private Drawable createShowOnTopbarIcon() {
		IconsCache ic = getMapActivity().getMyApplication().getIconsCache();
		Drawable background = ic.getIcon(R.drawable.ic_action_device_top,
				isLight() ? R.color.on_map_icon_color : R.color.ctx_menu_info_text_dark);
		Drawable topbar = ic.getIcon(R.drawable.ic_action_device_topbar, R.color.dashboard_blue);
		return new LayerDrawable(new Drawable[]{background, topbar});
	}
}
