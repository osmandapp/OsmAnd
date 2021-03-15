package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.plus.wikivoyage.menu.WikivoyageWptPtMenuBuilder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;

public class WptPtMenuController extends MenuController {

	private WptPt wpt;
	private MapMarker mapMarker;

	public WptPtMenuController(@NonNull MenuBuilder menuBuilder, @NonNull MapActivity mapActivity,
							   @NonNull PointDescription pointDescription, @NonNull final WptPt wpt) {
		super(menuBuilder, pointDescription, mapActivity);
		this.wpt = wpt;
		MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		mapMarker = markersHelper.getMapMarker(wpt);
		if (mapMarker == null) {
			mapMarker = markersHelper.getMapMarker(new LatLon(wpt.lat, wpt.lon));
		}

		TitleButtonController openTrackButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					GpxSelectionHelper selectionHelper = mapActivity.getMyApplication().getSelectedGpxHelper();
					SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedGPXFile(wpt);
					if (selectedGpxFile != null) {
						String path = selectedGpxFile.getGpxFile().path;
						TrackMenuFragment.showInstance(mapActivity, path, selectedGpxFile.isShowCurrentTrack(), new LatLon(wpt.lon, wpt.lat), null, null);
					}
				}
			}
		};
		openTrackButtonController.startIconId = R.drawable.ic_action_polygom_dark;
		openTrackButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);

		if (mapMarker != null) {
			PointDescription description = mapMarker.getPointDescription(mapActivity);
			MapMarkerMenuController markerMenuController = new MapMarkerMenuController(mapActivity, description, mapMarker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();

			additionalButtonsControllers = new ArrayList<>();
			additionalButtonsControllers.add(Pair.<TitleButtonController, TitleButtonController>create(openTrackButtonController, null));
		} else {
			leftTitleButtonController = openTrackButtonController;
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
	public Drawable getRightIcon() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return PointImageDrawable.getFromWpt(mapActivity.getMyApplication(),
					wpt.getColor(ContextCompat.getColor(mapActivity, R.color.gpx_color_point)), false, wpt);
		} else {
			return null;
		}
	}

	@Override
	public Drawable getSubtypeIcon() {
		if (Algorithms.isEmpty(getSubtypeStr())) {
			return null;
		} else {
			return getIcon(R.drawable.ic_action_group_name_16, isLight() ? R.color.icon_color_default_light : R.color.ctx_menu_bottom_view_icon_dark);
		}
	}

	@Override
	public boolean isWaypointButtonEnabled() {
		return mapMarker == null;
	}

	@NonNull
	@Override
	public String getTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			GpxSelectionHelper helper = mapActivity.getMyApplication().getSelectedGpxHelper();
			SelectedGpxFile selectedGpxFile = helper.getSelectedGPXFile(wpt);
			StringBuilder sb = new StringBuilder();
			sb.append(mapActivity.getString(R.string.shared_string_waypoint));
			sb.append(", ");
			if (selectedGpxFile != null) {
				File file = new File(selectedGpxFile.getGpxFile().path);
				String gpxName = file.getName().replace(IndexConstants.GPX_FILE_EXT, "").replace("/", " ").replace("_", " ");
				sb.append(gpxName);
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	@NonNull
	@Override
	public String getSubtypeStr() {
		return wpt.category != null ? wpt.category : "";
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_waypoint);
		} else {
			return "";
		}
	}

	public static WptPtMenuController getInstance(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull final WptPt wpt) {
		return new WptPtMenuController(new WikivoyageWptPtMenuBuilder(mapActivity, wpt), mapActivity, pointDescription, wpt);
	}
}