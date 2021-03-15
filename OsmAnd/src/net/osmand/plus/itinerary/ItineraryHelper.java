package net.osmand.plus.itinerary;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper.OnGroupSyncedListener;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItineraryHelper {

	private static final Log LOG = PlatformUtil.getLog(ItineraryHelper.class);

	private OsmandApplication app;

	private MapMarkersHelper markersHelper;

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	private Set<OnGroupSyncedListener> syncListeners = new HashSet<>();

	public ItineraryHelper(@NonNull OsmandApplication app) {
		this.app = app;
		markersHelper = app.getMapMarkersHelper();
	}

	public void addSyncListener(OnGroupSyncedListener listener) {
		syncListeners.add(listener);
	}

	public void removeSyncListener(OnGroupSyncedListener listener) {
		syncListeners.remove(listener);
	}

	public void runSynchronization(final @NonNull MapMarkersGroup group) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				new SyncGroupTask(group).executeOnExecutor(executorService);
			}
		});
	}

	private class SyncGroupTask extends AsyncTask<Void, Void, Void> {

		private MapMarkersGroup group;

		SyncGroupTask(MapMarkersGroup group) {
			this.group = group;
		}

		@Override
		protected void onPreExecute() {
			if (!syncListeners.isEmpty()) {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						for (OnGroupSyncedListener listener : syncListeners) {
							listener.onSyncStarted();
						}
					}
				});
			}
		}

		@Override
		protected Void doInBackground(Void... voids) {
			runGroupSynchronization();
			return null;
		}

		// TODO extract method from Asynctask to Helper directly
		private void runGroupSynchronization() {
			List<MapMarker> groupMarkers = new ArrayList<>(group.getMarkers());
			if (group.getType() == MapMarkersGroup.FAVORITES_TYPE) {
				FavoriteGroup favGroup = app.getFavorites().getGroup(group.getName());
				if (favGroup == null) {
					return;
				}
				group.setVisible(favGroup.isVisible());
				if (!group.isVisible() || group.isDisabled()) {
					markersHelper.removeGroupActiveMarkers(group, true);
					return;
				}
				List<FavouritePoint> points = new ArrayList<>(favGroup.getPoints());
				for (FavouritePoint fp : points) {
					markersHelper.addNewMarkerIfNeeded(group, groupMarkers, new LatLon(fp.getLatitude(), fp.getLongitude()), fp.getName(), fp, null);
				}
			} else if (group.getType() == MapMarkersGroup.GPX_TYPE) {
				GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
				File file = new File(group.getId());
				if (!file.exists()) {
					return;
				}

				String gpxPath = group.getId();
				SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(gpxPath);
				GPXFile gpx = selectedGpxFile == null ? null : selectedGpxFile.getGpxFile();
				group.setVisible(gpx != null || group.isVisibleUntilRestart());
				if (gpx == null || group.isDisabled()) {
					markersHelper.removeGroupActiveMarkers(group, true);
					return;
				}

				boolean addAll = group.getWptCategories() == null || group.getWptCategories().isEmpty();
				List<WptPt> gpxPoints = new ArrayList<>(gpx.getPoints());
				for (WptPt pt : gpxPoints) {
					if (addAll || group.getWptCategories().contains(pt.category)
							|| (pt.category == null && group.getWptCategories().contains(""))) {
						markersHelper.addNewMarkerIfNeeded(group, groupMarkers, new LatLon(pt.lat, pt.lon), pt.name, null, pt);
					}
				}
			}
			markersHelper.removeOldMarkersIfPresent(groupMarkers);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (!syncListeners.isEmpty()) {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						for (OnGroupSyncedListener listener : syncListeners) {
							listener.onSyncDone();
						}
					}
				});
			}
		}
	}
}