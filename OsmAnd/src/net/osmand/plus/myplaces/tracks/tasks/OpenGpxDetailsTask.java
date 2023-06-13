package net.osmand.plus.myplaces.tracks.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxDisplayItem;

import java.util.ArrayList;
import java.util.List;

public class OpenGpxDetailsTask extends BaseLoadAsyncTask<Void, Void, GpxDisplayItem> {

	private final GpxDisplayHelper gpxDisplayHelper;
	private final GPXFile gpxFile;

	public OpenGpxDetailsTask(@NonNull FragmentActivity activity, @NonNull GPXFile gpxFile) {
		super(activity);
		this.gpxFile = gpxFile;
		this.gpxDisplayHelper = app.getGpxDisplayHelper();
	}

	@Nullable
	@Override
	protected GpxDisplayItem doInBackground(Void... voids) {
		Track generalTrack = gpxFile.getGeneralTrack();

		GpxDisplayGroup gpxDisplayGroup = null;
		if (generalTrack != null) {
			gpxFile.addGeneralTrack();
			gpxDisplayGroup = gpxDisplayHelper.buildGeneralGpxDisplayGroup(gpxFile, generalTrack);
		} else if (!gpxFile.tracks.isEmpty()) {
			gpxDisplayGroup = gpxDisplayHelper.buildGeneralGpxDisplayGroup(gpxFile, gpxFile.tracks.get(0));
		}
		List<GpxDisplayItem> items = null;
		if (gpxDisplayGroup != null) {
			items = gpxDisplayGroup.getDisplayItems();
		}
		if (items != null && !items.isEmpty()) {
			return items.get(0);
		}
		return null;
	}

	@Override
	protected void onPostExecute(@Nullable GpxDisplayItem gpxItem) {
		hideProgress();

		if (gpxItem != null && gpxItem.analysis != null) {
			ArrayList<GPXDataSetType> list = new ArrayList<>();
			if (gpxItem.analysis.hasElevationData()) {
				list.add(GPXDataSetType.ALTITUDE);
			}
			if (gpxItem.analysis.hasSpeedData()) {
				list.add(GPXDataSetType.SPEED);
			} else if (gpxItem.analysis.hasElevationData()) {
				list.add(GPXDataSetType.SLOPE);
			}
			if (!list.isEmpty()) {
				gpxItem.chartTypes = list.toArray(new GPXDataSetType[0]);
			}
			OsmandSettings settings = app.getSettings();
			settings.setMapLocationToShow(gpxItem.locationStart.lat, gpxItem.locationStart.lon,
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
					false,
					gpxItem);

			FragmentActivity activity = activityRef.get();
			Context context = activity != null ? activity : app;
			MapActivity.launchMapActivityMoveToTop(context);
		}
	}
}
