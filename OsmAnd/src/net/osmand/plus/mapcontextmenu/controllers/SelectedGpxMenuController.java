package net.osmand.plus.mapcontextmenu.controllers;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.SelectedGpxMenuBuilder;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SelectedGpxMenuController extends MenuController {

	private SelectedGpxPoint selectedGpxPoint;

	public SelectedGpxMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription,
	                                 @NonNull SelectedGpxPoint selectedGpxPoint) {
		super(new SelectedGpxMenuBuilder(mapActivity, selectedGpxPoint), pointDescription, mapActivity);
		this.selectedGpxPoint = selectedGpxPoint;
		builder.setShowOnlinePhotos(false);

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				mapContextMenu.close();
				SelectedGpxFile selectedGpxFile = selectedGpxPoint.getSelectedGpxFile();
				TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, selectedGpxPoint);
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);
		leftTitleButtonController.startIconId = R.drawable.ic_action_folder;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				new OpenGpxDetailsTask(selectedGpxPoint.getSelectedGpxFile(), selectedGpxPoint.getSelectedPoint(), mapActivity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.analyze_on_map);
		rightTitleButtonController.startIconId = R.drawable.ic_action_analyze_intervals;
	}

	public static class OpenGpxDetailsTask extends AsyncTask<Void, Void, GpxDisplayItem> {

		private final OsmandApplication app;

		private final WptPt selectedPoint;
		private final SelectedGpxFile selectedGpxFile;

		private ProgressDialog progressDialog;
		private final WeakReference<MapActivity> activityRef;

		public OpenGpxDetailsTask(SelectedGpxFile selectedGpxFile, WptPt selectedPoint, MapActivity mapActivity) {
			app = mapActivity.getMyApplication();
			this.activityRef = new WeakReference<>(mapActivity);
			this.selectedGpxFile = selectedGpxFile;
			this.selectedPoint = selectedPoint;
		}

		@Override
		protected void onPreExecute() {
			MapActivity activity = activityRef.get();
			if (activity != null && AndroidUtils.isActivityNotDestroyed(activity)) {
				if (selectedGpxFile.getGpxFile().path != null) {
					progressDialog = new ProgressDialog(activity);
					progressDialog.setTitle("");
					progressDialog.setMessage(app.getString(R.string.loading_data));
					progressDialog.setCancelable(false);
					progressDialog.show();
				}
			}
		}

		@Override
		protected GpxDisplayItem doInBackground(Void... voids) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			if (gpxFile.tracks.size() > 0) {
				GpxDisplayGroup gpxDisplayGroup = app.getGpxDisplayHelper().buildGeneralGpxDisplayGroup(gpxFile, gpxFile.tracks.get(0));

				List<GpxDisplayItem> items = gpxDisplayGroup.getModifiableList();
				if (!Algorithms.isEmpty(items)) {
					return items.get(0);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(GpxDisplayItem gpxItem) {
			MapActivity activity = activityRef.get();
			if (activity != null) {
				if (progressDialog != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					progressDialog.dismiss();
				}
				if (gpxItem != null && gpxItem.analysis != null) {
					ArrayList<GpxUiHelper.GPXDataSetType> list = new ArrayList<>();
					if (gpxItem.analysis.hasElevationData) {
						list.add(GpxUiHelper.GPXDataSetType.ALTITUDE);
					}
					if (gpxItem.analysis.hasSpeedData) {
						list.add(GpxUiHelper.GPXDataSetType.SPEED);
					} else if (gpxItem.analysis.hasElevationData) {
						list.add(GpxUiHelper.GPXDataSetType.SLOPE);
					}
					if (list.size() > 0) {
						gpxItem.chartTypes = list.toArray(new GpxUiHelper.GPXDataSetType[0]);
					}
					gpxItem.locationOnMap = selectedPoint;
					OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(gpxItem.locationStart.lat, gpxItem.locationStart.lon,
							settings.getLastKnownMapZoom(),
							new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
							false,
							gpxItem);
					activity.getContextMenu().hide();
					MapActivity.launchMapActivityMoveToTop(activity);
				}
			}
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof SelectedGpxPoint) {
			this.selectedGpxPoint = (SelectedGpxPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return selectedGpxPoint;
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_gpx_track);
		} else {
			return "";
		}
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
		int color = ColorUtilities.getActiveColorId(!isLight());
		return getIcon(R.drawable.ic_action_polygom_dark, color);
	}

	public static class SelectedGpxPoint {

		private final SelectedGpxFile selectedGpxFile;
		private final WptPt selectedPoint;
		private final WptPt prevPoint;
		private final WptPt nextPoint;
		private final float bearing;
		private final boolean showTrackPointMenu;

		public SelectedGpxPoint(SelectedGpxFile selectedGpxFile, WptPt selectedPoint) {
			this(selectedGpxFile, selectedPoint, null, null, Float.NaN, false);
		}

		public SelectedGpxPoint(SelectedGpxFile selectedGpxFile, WptPt selectedPoint, WptPt prevPoint,
		                        WptPt nextPoint, float bearing, boolean showTrackPointMenu) {
			this.prevPoint = prevPoint;
			this.nextPoint = nextPoint;
			this.selectedPoint = selectedPoint;
			this.selectedGpxFile = selectedGpxFile;
			this.bearing = bearing;
			this.showTrackPointMenu = showTrackPointMenu;
		}

		public SelectedGpxFile getSelectedGpxFile() {
			return selectedGpxFile;
		}

		public WptPt getSelectedPoint() {
			return selectedPoint;
		}

		public float getBearing() {
			return bearing;
		}

		public WptPt getPrevPoint() {
			return prevPoint;
		}

		public WptPt getNextPoint() {
			return nextPoint;
		}

		public boolean shouldShowTrackPointMenu() {
			return showTrackPointMenu;
		}
	}
}