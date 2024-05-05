package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.gpx.GPXUtilities.PointsGroup.DEFAULT_WPT_GROUP_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class UpdatePointsGroupsTask extends BaseLoadAsyncTask<Void, Void, Exception> {

	private final GPXFile gpxFile;
	private final Map<String, PointsGroup> changedGroups;
	private final UpdateGpxListener listener;
	private boolean updatePointsAppearance;

	private boolean gpxUpdated;

	public UpdatePointsGroupsTask(@NonNull FragmentActivity activity, @NonNull GPXFile gpxFile,
	                              @NonNull Map<String, PointsGroup> changedGroups, @Nullable UpdateGpxListener listener) {
		super(activity);
		this.gpxFile = gpxFile;
		this.listener = listener;
		this.changedGroups = changedGroups;
		setShouldShowProgress(false);
	}

	public void setUpdatePointsAppearance(boolean updatePointsAppearance) {
		this.updatePointsAppearance = updatePointsAppearance;
	}

	@Override
	protected Exception doInBackground(Void... voids) {
		updatePoints();

		if (!gpxFile.showCurrentTrack && gpxUpdated) {
			return GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile);
		}
		return null;
	}

	private void updatePoints() {
		long modifiedTime = gpxFile.modifiedTime;
		for (WptPt wpt : gpxFile.getPoints()) {
			String name = wpt.category != null ? wpt.category : DEFAULT_WPT_GROUP_NAME;
			PointsGroup pointsGroup = changedGroups.get(name);
			if (pointsGroup != null) {
				updatePoint(pointsGroup, wpt);
			}
		}
		for (Map.Entry<String, PointsGroup> entry : changedGroups.entrySet()) {
			gpxFile.updatePointsGroup(entry.getKey(), entry.getValue());
		}
		gpxUpdated = modifiedTime != gpxFile.modifiedTime;
	}

	private void updatePoint(@NonNull PointsGroup pointsGroup, @NonNull WptPt wpt) {
		String category = pointsGroup.name;
		String iconName = updatePointsAppearance ? pointsGroup.iconName : wpt.getIconName();
		String backgroundType = updatePointsAppearance ? pointsGroup.backgroundType : wpt.getBackgroundType();
		int color = updatePointsAppearance ? pointsGroup.color : wpt.getColor();

		if (gpxFile.showCurrentTrack) {
			app.getSavingTrackHelper().updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
					wpt.desc, wpt.name, category, color, iconName, backgroundType);
		} else {
			WptPt wptInfo = new WptPt(wpt.getLatitude(), wpt.getLongitude(), wpt.desc, wpt.name, category,
					Algorithms.colorToString(color), iconName, backgroundType);
			gpxFile.updateWptPt(wpt, wptInfo);
		}
	}

	@Override
	protected void onPostExecute(Exception exception) {
		hideProgress();

		if (gpxUpdated) {
			syncGpx();
		}
		if (listener != null) {
			listener.updateGpxFinished(exception);
		}
	}

	private void syncGpx() {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		MapMarkersGroup group = markersHelper.getMarkersGroup(gpxFile);
		if (group != null) {
			markersHelper.runSynchronization(group);
		}
	}

	public interface UpdateGpxListener {

		void updateGpxFinished(@Nullable Exception exception);
	}
}