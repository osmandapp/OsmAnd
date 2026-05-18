package net.osmand.plus.myplaces.tracks.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Set;

public class DeletePointsTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final GpxFile gpx;
	private final Set<GpxDisplayItem> selectedItems;
	private final WeakReference<OnPointsDeleteListener> listenerRef;

	public DeletePointsTask(OsmandApplication app, GpxFile gpxFile, Set<GpxDisplayItem> selectedItems, OnPointsDeleteListener listener) {
		this.app = app;
		this.gpx = gpxFile;
		this.selectedItems = selectedItems;
		this.listenerRef = new WeakReference<>(listener);
	}

	@Override
	protected void onPreExecute() {
		OnPointsDeleteListener listener = listenerRef.get();
		if (listener != null) {
			listener.onPointsDeletionStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		if (gpx != null) {
			for (GpxDisplayItem item : selectedItems) {
				if (gpx.isShowCurrentTrack()) {
					savingTrackHelper.deletePointData(item.locationStart);
				} else {
					if (item.group.getType() == GpxDisplayItemType.TRACK_POINTS) {
						gpx.deleteWptPt(item.locationStart);
					} else if (item.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
						gpx.deleteRtePt(item.locationStart);
					}
				}
			}
			if (!gpx.isShowCurrentTrack()) {
				SharedUtil.writeGpxFile(new File(gpx.getPath()), gpx);
				boolean selected = app.getSelectedGpxHelper().getSelectedFileByPath(gpx.getPath()) != null;
				if (selected) {
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
			syncGpx(app, gpx);
		}
		return null;
	}

	public static void syncGpx(@NonNull OsmandApplication app, GpxFile gpxFile) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(gpxFile);
		if (group != null) {
			helper.runSynchronization(group);
		}
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		OnPointsDeleteListener listener = listenerRef.get();
		if (listener != null) {
			listener.onPointsDeleted();
		}
	}

	public interface OnPointsDeleteListener {
		default void onPointsDeletionStarted() {

		}

		void onPointsDeleted();
	}
}