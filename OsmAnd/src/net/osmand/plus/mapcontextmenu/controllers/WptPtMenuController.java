package net.osmand.plus.mapcontextmenu.controllers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import net.osmand.GPXUtilities;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.WptPtMenuBuilder;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.util.Algorithms;

import java.io.File;

public class WptPtMenuController extends MenuController {

	private WptPt wpt;
	private MapMarker mapMarker;

	public WptPtMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull final WptPt wpt) {
		super(new WptPtMenuBuilder(mapActivity, wpt), pointDescription, mapActivity);
		this.wpt = wpt;

		final MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		mapMarker = markersHelper.getMapMarker(wpt);
		if (mapMarker == null) {
			mapMarker = markersHelper.getMapMarker(new LatLon(wpt.lat, wpt.lon));
		} else {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(mapActivity, mapMarker.getPointDescription(mapActivity), mapMarker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		}
		//todo extract / simplify
		final OsmandApplication app = mapActivity.getMyApplication();
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
		GPXUtilities.GPXFile gpxFile = selectedGpxFile != null ? selectedGpxFile.getGpxFile() : null;
		GPXUtilities.Metadata metadata = gpxFile != null ? gpxFile.metadata : null;
		final TravelArticle article = metadata != null ? getTravelArticle(metadata) : null;
		if (article != null) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					WikivoyageArticleDialogFragment.showInstance(app, getMapActivity().getSupportFragmentManager(), article.getTripId(), article.getLang());
				}
			};
			leftTitleButtonController.caption = mapActivity.getString(R.string.context_menu_read_article);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_read_text;
		}
	}

	//todo extract somewhere, maybe to TravelDbHelper
	private TravelArticle getTravelArticle(@NonNull GPXUtilities.Metadata metadata) {
		String title = metadata.getArticleTitle();
		String lang = metadata.getArticleLang();
		if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(lang)) {
			return getMapActivity().getMyApplication().getTravelDbHelper().getArticle(title, lang);
		}
		return null;
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
			return FavoriteImageDrawable.getOrCreate(mapActivity.getMyApplication(),
					wpt.getColor(ContextCompat.getColor(mapActivity, R.color.gpx_color_point)), false);
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
				String gpxName = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
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
}
