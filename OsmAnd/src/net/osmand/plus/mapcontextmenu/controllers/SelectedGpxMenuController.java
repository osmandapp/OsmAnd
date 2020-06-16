package net.osmand.plus.mapcontextmenu.controllers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectedGpxMenuController extends MenuController {

	private SelectedGpxFile item;

	public SelectedGpxMenuController(@NonNull final MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull final SelectedGpxFile item) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.item = item;

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				Intent intent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getTrackActivity());
				intent.putExtra(TrackActivity.TRACK_FILE_NAME, item.getGpxFile().path);
				intent.putExtra(TrackActivity.OPEN_TRACKS_LIST, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mapActivity.startActivity(intent);
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);
		leftTitleButtonController.startIconId = R.drawable.ic_action_folder;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				new OpenGpxDetailsTask(item).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.analyze_on_map);
		rightTitleButtonController.startIconId = R.drawable.ic_action_track_16;
	}

	private class OpenGpxDetailsTask extends AsyncTask<Void, Void, GpxSelectionHelper.GpxDisplayItem> {

		private SelectedGpxFile item;
		ProgressDialog progressDialog;

		OpenGpxDetailsTask(SelectedGpxFile item) {
			this.item = item;
		}

		@Override
		protected void onPreExecute() {
			if (item.getGpxFile().path != null) {
				progressDialog = new ProgressDialog(getMapActivity());
				progressDialog.setTitle("");
				progressDialog.setMessage(getMapActivity().getResources().getString(R.string.loading_data));
				progressDialog.setCancelable(false);
				progressDialog.show();
			}
		}

		@Override
		protected GpxSelectionHelper.GpxDisplayItem doInBackground(Void... voids) {
			GpxSelectionHelper.GpxDisplayGroup gpxDisplayGroup = null;
			GPXUtilities.GPXFile gpxFile = null;
			GPXUtilities.Track generalTrack = null;
			if (item.getGpxFile().path != null) {
				gpxFile = GPXUtilities.loadGPXFile(new File(item.getGpxFile().path));
			}
			if (gpxFile != null) {
				generalTrack = gpxFile.getGeneralTrack();
			}
			OsmandApplication app = getMapActivity().getMyApplication();
			if (generalTrack != null) {
				gpxFile.addGeneralTrack();
				gpxDisplayGroup = app.getSelectedGpxHelper().buildGeneralGpxDisplayGroup(gpxFile, generalTrack);
			} else if (gpxFile != null && gpxFile.tracks.size() > 0) {
				gpxDisplayGroup = app.getSelectedGpxHelper().buildGeneralGpxDisplayGroup(gpxFile, gpxFile.tracks.get(0));
			}
			List<GpxSelectionHelper.GpxDisplayItem> items = null;
			if (gpxDisplayGroup != null) {
				items = gpxDisplayGroup.getModifiableList();
			}
			if (items != null && items.size() > 0) {
				return items.get(0);
			}
			return null;
		}

		@Override
		protected void onPostExecute(GpxSelectionHelper.GpxDisplayItem gpxItem) {
			if (progressDialog != null) {
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
					gpxItem.chartTypes = list.toArray(new GpxUiHelper.GPXDataSetType[list.size()]);
				}

				MapActivity mapActivity = getMapActivity();
				OsmandApplication app = mapActivity.getMyApplication();
				final OsmandSettings settings = app.getSettings();
				settings.setMapLocationToShow(gpxItem.locationStart.lat, gpxItem.locationStart.lon,
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
						false,
						gpxItem);
				mapActivity.getContextMenu().hide();
				MapActivity.launchMapActivityMoveToTop(mapActivity);
			}
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof SelectedGpxFile) {
			this.item = (SelectedGpxFile) object;
		}
	}

	@Override
	protected Object getObject() {
		return item;
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
	public Drawable getRightIcon() {
		int color = isLight() ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		return getIcon(R.drawable.ic_action_polygom_dark, color);
	}
}
