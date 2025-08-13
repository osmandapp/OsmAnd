package net.osmand.plus.mapmarkers;

import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.TspAnt;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

class SortMarkersTask extends BaseLoadAsyncTask<Void, Void, List<MapMarker>> {

	private final Location location;
	private final boolean startFromLoc;
	private final CallbackWithObject<List<MapMarker>> callback;

	private long startDialogTime;

	public SortMarkersTask(@NonNull FragmentActivity activity, @Nullable Location location,
			boolean startFromLoc, @Nullable CallbackWithObject<List<MapMarker>> callback) {
		super(activity);
		this.location = location;
		this.startFromLoc = startFromLoc;
		this.callback = callback;
	}

	@Override
	protected List<MapMarker> doInBackground(Void... voids) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> selectedMarkers = markersHelper.getSelectedMarkers();
		List<LatLon> selectedLatLon = markersHelper.getSelectedMarkersLatLon();

		LatLon start = startFromLoc ? new LatLon(location.getLatitude(), location.getLongitude())
				: selectedLatLon.remove(0);

		int[] sequence = new TspAnt().readGraph(selectedLatLon, start, null).solve();

		List<MapMarker> res = new ArrayList<>();
		for (int i = 0; i < sequence.length; i++) {
			if (i == 0 && startFromLoc) {
				continue;
			}
			int index = sequence[i];
			res.add(selectedMarkers.get(startFromLoc ? index - 1 : index));
		}

		return res;
	}

	@Override
	protected void onPostExecute(List<MapMarker> markers) {
		hideProgress();
		if (callback != null) {
			callback.processResult(markers);
		}
	}

	@Override
	protected void showProgress(boolean cancelableOnTouchOutside) {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			startDialogTime = System.currentTimeMillis();
			progress = new ProgressDialog(activityRef.get());
			progress.setTitle("");
			progress.setMessage(app.getString(R.string.intermediate_items_sort_by_distance));
			progress.setCancelable(false);
			progress.show();
		}
	}

	@Override
	protected void hideProgress() {
		FragmentActivity activity = activityRef.get();
		if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			if (progress != null) {
				long t = System.currentTimeMillis();
				if (t - startDialogTime < 500) {
					app.runInUIThread(progress::dismiss, 500 - (t - startDialogTime));
				} else {
					progress.dismiss();
				}
			}
		}
	}
}
