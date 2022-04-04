package net.osmand.plus.myplaces;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.ui.EditTrackGroupDialogFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.SavingTrackHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class UpdateGpxCategoryTask extends AsyncTask<Void, Void, Void> {

	private OsmandApplication app;
	private WeakReference<FragmentActivity> activityRef;

	private GpxDisplayGroup group;

	private String newCategory;
	private Integer newColor;

	private ProgressDialog progressDialog;
	private boolean wasUpdated = false;

	private UpdateGpxCategoryTask(@NonNull FragmentActivity activity, @NonNull GpxDisplayGroup group) {
		this.app = (OsmandApplication) activity.getApplication();
		activityRef = new WeakReference<>(activity);

		this.group = group;
	}

	public UpdateGpxCategoryTask(@NonNull FragmentActivity activity, @NonNull GpxDisplayGroup group,
	                             @NonNull String newCategory) {
		this(activity, group);
		this.newCategory = newCategory;
	}

	public UpdateGpxCategoryTask(@NonNull FragmentActivity activity, @NonNull GpxDisplayGroup group,
	                      @NonNull Integer newColor) {
		this(activity, group);
		this.newColor = newColor;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (activity != null) {
			progressDialog = new ProgressDialog(activity);
			progressDialog.setTitle(EditTrackGroupDialogFragment.getCategoryName(app, group.getName()));
			progressDialog.setMessage(newCategory != null ? "Changing name" : "Changing color");
			progressDialog.setCancelable(false);
			progressDialog.show();

			GPXFile gpxFile = group.getGpx();
			if (gpxFile != null) {
				SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
				List<GpxDisplayItem> items = group.getModifiableList();
				String prevCategory = group.getName();
				boolean emptyCategory = TextUtils.isEmpty(prevCategory);
				for (GpxDisplayItem item : items) {
					WptPt wpt = item.locationStart;
					if (wpt != null) {
						boolean update = false;
						if (emptyCategory) {
							if (TextUtils.isEmpty(wpt.category)) {
								update = true;
							}
						} else if (prevCategory.equals(wpt.category)) {
							update = true;
						}
						if (update) {
							wasUpdated = true;
							String category = newCategory != null ? newCategory : wpt.category;
							int color = newColor != null ? newColor : wpt.colourARGB;
							if (gpxFile.showCurrentTrack) {
								savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
										System.currentTimeMillis(), wpt.desc, wpt.name, category, color);
							} else {
								gpxFile.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(),
										System.currentTimeMillis(), wpt.desc, wpt.name, category, color,
										wpt.getIconName(), wpt.getBackgroundType());
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		GPXFile gpxFile = group.getGpx();
		if (gpxFile != null && !gpxFile.showCurrentTrack && wasUpdated) {
			GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		GPXFile gpxFile = group.getGpx();
		if (gpxFile != null && wasUpdated) {
			syncGpx(gpxFile);
		}

		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		FragmentActivity activity = activityRef.get();
		if (activity instanceof MapActivity) {
			MapActivity mapActivity = (MapActivity) activity;
			TrackMenuFragment fragment = mapActivity.getTrackMenuFragment();
			if (fragment != null) {
				fragment.updateContent();
			}
		}
	}

	private void syncGpx(GPXFile gpxFile) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		MapMarkersGroup group = markersHelper.getMarkersGroup(gpxFile);
		if (group != null) {
			markersHelper.runSynchronization(group);
		}
	}
}