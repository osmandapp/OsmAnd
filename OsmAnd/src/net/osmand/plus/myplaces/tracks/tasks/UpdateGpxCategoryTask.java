package net.osmand.plus.myplaces.tracks.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.util.Algorithms;

import java.io.File;

public class UpdateGpxCategoryTask extends BaseLoadAsyncTask<Void, Void, Exception> {

	private final GPXFile gpxFile;
	private final String prevGroupName;
	private final PointsGroup pointsGroup;
	private final UpdateGpxListener listener;
	private final boolean updatePointsAppearance;

	private boolean gpxUpdated;

	public UpdateGpxCategoryTask(@NonNull FragmentActivity activity,
	                             @NonNull GPXFile gpxFile,
	                             @NonNull String prevGroupName,
	                             @NonNull PointsGroup pointsGroup,
	                             @Nullable UpdateGpxListener listener,
	                             boolean updatePointsAppearance) {
		super(activity);
		this.gpxFile = gpxFile;
		this.listener = listener;
		this.prevGroupName = prevGroupName;
		this.pointsGroup = pointsGroup;
		this.updatePointsAppearance = updatePointsAppearance;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		updatePoints();
	}

	@Override
	protected Exception doInBackground(Void... voids) {
		if (!gpxFile.showCurrentTrack && gpxUpdated) {
			return GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile);
		}
		return null;
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

	private void updatePoints() {
		long modifiedTime = gpxFile.modifiedTime;
		for (WptPt wpt : gpxFile.getPoints()) {
			if (Algorithms.stringsEqual(wpt.category, prevGroupName)
					|| Algorithms.isEmpty(wpt.category) && Algorithms.isEmpty(prevGroupName)) {
				updatePoint(wpt);
			}
		}
		gpxFile.updatePointsGroup(prevGroupName, pointsGroup);
		gpxUpdated = modifiedTime != gpxFile.modifiedTime;
	}

	private void updatePoint(@NonNull WptPt wpt) {
		String category = pointsGroup.name;
		String iconName = updatePointsAppearance ? pointsGroup.iconName : wpt.getIconName();
		String backgroundType = updatePointsAppearance ? pointsGroup.backgroundType : wpt.getBackgroundType();
		int color = updatePointsAppearance ? pointsGroup.color : wpt.colourARGB;

		if (gpxFile.showCurrentTrack) {
			app.getSavingTrackHelper().updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
					wpt.desc, wpt.name, category, color, iconName, backgroundType);
		} else {
			WptPt wptInfo = new WptPt(wpt.getLatitude(), wpt.getLongitude(), wpt.desc, wpt.name, category,
					Algorithms.colorToString(color), iconName, backgroundType);
			gpxFile.updateWptPt(wpt, wptInfo);
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