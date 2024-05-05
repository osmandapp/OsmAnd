package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.plus.charts.GPXDataSetType.ALTITUDE;
import static net.osmand.plus.charts.GPXDataSetType.SLOPE;
import static net.osmand.plus.charts.GPXDataSetType.SPEED;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.track.SplitTrackAsyncTask;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.TrackDisplayGroup;

import java.util.ArrayList;
import java.util.List;

public class OpenGpxDetailsTask extends BaseLoadAsyncTask<Void, Void, GpxDisplayItem> {

	private final GPXFile gpxFile;
	private final WptPt selectedPoint;

	public OpenGpxDetailsTask(@NonNull FragmentActivity activity, @NonNull GPXFile gpxFile, @Nullable WptPt selectedPoint) {
		super(activity);
		this.gpxFile = gpxFile;
		this.selectedPoint = selectedPoint;
	}

	@Nullable
	@Override
	protected GpxDisplayItem doInBackground(Void... voids) {
		Track generalTrack = gpxFile.getGeneralTrack();

		GpxDisplayGroup gpxDisplayGroup = null;
		if (generalTrack != null) {
			gpxFile.addGeneralTrack();
			gpxDisplayGroup = buildGeneralGpxDisplayGroup(gpxFile, generalTrack);
		} else if (!gpxFile.tracks.isEmpty()) {
			gpxDisplayGroup = buildGeneralGpxDisplayGroup(gpxFile, gpxFile.tracks.get(0));
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

	@NonNull
	private GpxDisplayGroup buildGeneralGpxDisplayGroup(@NonNull GPXFile gpxFile, @NonNull Track track) {
		String name = GpxDisplayHelper.getGroupName(app, gpxFile);
		TrackDisplayGroup group = new TrackDisplayGroup(gpxFile, track, true);
		group.applyName(app, name);
		group.setColor(track.getColor(gpxFile.getColor(0)));
		String description = "";
		if (track.name != null && !track.name.isEmpty()) {
			description = track.name + " " + description;
		}
		group.setDescription(description);
		SplitTrackAsyncTask.processGroupTrack(app, group, null, false);
		return group;
	}

	@Override
	protected void onPostExecute(@Nullable GpxDisplayItem gpxItem) {
		hideProgress();

		if (gpxItem != null && gpxItem.analysis != null) {
			ArrayList<GPXDataSetType> list = new ArrayList<>();
			if (gpxItem.analysis.hasElevationData()) {
				list.add(ALTITUDE);
			}
			if (gpxItem.analysis.hasSpeedData()) {
				list.add(SPEED);
			} else if (gpxItem.analysis.hasElevationData()) {
				list.add(SLOPE);
			}
			if (!list.isEmpty()) {
				gpxItem.chartTypes = list.toArray(new GPXDataSetType[0]);
			}
			gpxItem.locationOnMap = selectedPoint;
			settings.setMapLocationToShow(gpxItem.locationStart.lat, gpxItem.locationStart.lon,
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
					false,
					gpxItem);

			FragmentActivity activity = activityRef.get();
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				mapActivity.getContextMenu().hide();
				mapActivity.getDashboard().hideDashboard();
			}
			Context context = activity != null ? activity : app;
			MapActivity.launchMapActivityMoveToTop(context);
		}
	}
}
