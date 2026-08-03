package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNoteMenuController;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.wikivoyage.menu.WikivoyageWptPtMenuBuilder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;

public class WptPtMenuController extends MenuController {

	private WptPt wpt;
	private MapMarker mapMarker;
	private AudioVideoNoteMenuController audioVideoNoteController;

	public WptPtMenuController(@NonNull MenuBuilder menuBuilder, @NonNull MapActivity mapActivity,
	                           @NonNull PointDescription pointDescription, @NonNull WptPt wpt) {
		super(menuBuilder, pointDescription, mapActivity);
		this.wpt = wpt;

		OsmandApplication app = mapActivity.getMyApplication();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();

		mapMarker = markersHelper.getMapMarker(wpt);
		if (mapMarker == null) {
			mapMarker = markersHelper.getMapMarker(new LatLon(wpt.getLat(), wpt.getLon()));
		}
		if (mapMarker != null && mapMarker.history && !app.getSettings().KEEP_PASSED_MARKERS_ON_MAP.get()) {
			mapMarker = null;
		}
		TitleButtonController openTrackButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					GpxSelectionHelper selectionHelper = mapActivity.getMyApplication().getSelectedGpxHelper();
					SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedGPXFile(wpt);
					if (selectedGpxFile != null) {
						SelectedGpxPoint gpxPoint = new SelectedGpxPoint(selectedGpxFile, wpt);
						TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, gpxPoint);
					}
				}
			}
		};
		openTrackButtonController.startIconId = R.drawable.ic_action_polygom_dark;
		openTrackButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);

		additionalButtonsControllers = new ArrayList<>();
		if (mapMarker != null) {
			PointDescription description = mapMarker.getPointDescription(mapActivity);
			MapMarkerMenuController markerMenuController = new MapMarkerMenuController(mapActivity, description, mapMarker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();

			additionalButtonsControllers.add(Pair.create(openTrackButtonController, null));
		} else {
			leftTitleButtonController = openTrackButtonController;
		}

		AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			Recording selectedRec = null;
			for (Recording rec : plugin.getAllRecordings()) {
				if (Math.abs(rec.getLatitude() - wpt.getLat()) < 0.0001
						&& Math.abs(rec.getLongitude() - wpt.getLon()) < 0.0001) {
					selectedRec = rec;
					break;
				}
			}
			if (selectedRec != null) {
				audioVideoNoteController = new AudioVideoNoteMenuController(mapActivity, pointDescription, selectedRec);

				TitleButtonController leftButtonController = audioVideoNoteController.getLeftTitleButtonController();
				TitleButtonController rightButtonController = audioVideoNoteController.getRightTitleButtonController();
				if (leftButtonController != null && rightButtonController != null) {
					additionalButtonsControllers.add(Pair.create(leftButtonController, rightButtonController));
				}
			}
		}
	}

	@Override
	public void updateData() {
		super.updateData();
		if (audioVideoNoteController != null) {
			audioVideoNoteController.updateData();
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof WptPt) {
			this.wpt = (WptPt) object;

			if (builder instanceof WikivoyageWptPtMenuBuilder) {
				((WikivoyageWptPtMenuBuilder) builder).updateDescriptionTokens(wpt);
			}
		}
	}

	@Override
	protected Object getObject() {
		return wpt;
	}

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
			return PointImageUtils.getFromPoint(mapActivity.getMyApplication(),
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
			return getIcon(R.drawable.ic_action_group_name_16, isLight() ? R.color.icon_color_default_light : R.color.icon_color_secondary_dark);
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
				File file = new File(selectedGpxFile.getGpxFile().getPath());
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
		return wpt.getCategory() != null ? wpt.getCategory() : "";
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

	public static WptPtMenuController getInstance(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull WptPt wpt) {
		return new WptPtMenuController(new WikivoyageWptPtMenuBuilder(mapActivity, wpt), mapActivity, pointDescription, wpt);
	}
}