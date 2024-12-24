package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.shared.gpx.GpxFile.DEFAULT_WPT_GROUP_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class UpdatePointsGroupsTask extends BaseLoadAsyncTask<Void, Void, Exception> {

	private final GpxFile gpxFile;
	private final Map<String, PointsGroup> changedGroups;
	private final UpdateGpxListener listener;
	private boolean updatePointsAppearance;

	private boolean gpxUpdated;

	public UpdatePointsGroupsTask(@NonNull FragmentActivity activity, @NonNull GpxFile gpxFile,
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

		if (!gpxFile.isShowCurrentTrack() && gpxUpdated) {
			return SharedUtil.writeGpxFile(new File(gpxFile.getPath()), gpxFile);
		}
		return null;
	}

	private void updatePoints() {
		long modifiedTime = gpxFile.getModifiedTime();
		for (WptPt wpt : gpxFile.getPointsList()) {
			String name = wpt.getCategory() != null ? wpt.getCategory() : DEFAULT_WPT_GROUP_NAME;
			PointsGroup pointsGroup = changedGroups.get(name);
			if (pointsGroup != null) {
				updatePoint(pointsGroup, wpt);
			}
		}
		for (Map.Entry<String, PointsGroup> entry : changedGroups.entrySet()) {
			gpxFile.updatePointsGroup(entry.getKey(), entry.getValue());
		}
		gpxUpdated = modifiedTime != gpxFile.getModifiedTime();
	}

	private void updatePoint(@NonNull PointsGroup pointsGroup, @NonNull WptPt wpt) {
		String category = pointsGroup.getName();
		String iconName = updatePointsAppearance ? pointsGroup.getIconName() : wpt.getIconName();
		String backgroundType = updatePointsAppearance ? pointsGroup.getBackgroundType() : wpt.getBackgroundType();
		int color = updatePointsAppearance ? pointsGroup.getColor() : wpt.getColor();

		if (gpxFile.isShowCurrentTrack()) {
			app.getSavingTrackHelper().updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
					wpt.getDesc(), wpt.getName(), category, color, iconName, backgroundType);
		} else {
			WptPt wptInfo = new WptPt(wpt.getLatitude(), wpt.getLongitude(), wpt.getDesc(), wpt.getName(), category,
					Algorithms.colorToString(color), iconName, backgroundType);
			gpxFile.updateWptPt(wpt, wptInfo, false);
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
