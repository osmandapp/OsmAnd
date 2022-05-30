package net.osmand.plus.myplaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.util.Algorithms;

import java.io.File;

public class UpdateGpxCategoryTask extends BaseLoadAsyncTask<Void, Void, Exception> {

	private final GPXFile gpxFile;
	private final PointsGroup prevPointsGroup;
	private final PointsGroup updatedPointsGroup;
	private final UpdateGpxListener listener;
	private final boolean updateAppearance;

	private boolean pointsUpdated;

	public UpdateGpxCategoryTask(@NonNull FragmentActivity activity,
	                             @NonNull GPXFile gpxFile,
	                             @NonNull PointsGroup prevPointsGroup,
	                             @NonNull PointsGroup updatedPointsGroup,
	                             @Nullable UpdateGpxListener listener,
	                             boolean updateAppearance) {
		super(activity);
		this.gpxFile = gpxFile;
		this.listener = listener;
		this.prevPointsGroup = prevPointsGroup;
		this.updatedPointsGroup = updatedPointsGroup;
		this.updateAppearance = updateAppearance;
	}

	@Override
	protected void onPreExecute() {
		showProgress();
		updatePoints();
	}

	@Override
	protected Exception doInBackground(Void... voids) {
		if (!gpxFile.showCurrentTrack && pointsUpdated) {
			return GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Exception exception) {
		hideProgress();

		if (pointsUpdated) {
			syncGpx();
		}
		if (listener != null) {
			listener.updateGpxFinished(exception);
		}
	}

	private void updatePoints() {
		for (WptPt wpt : gpxFile.getPoints()) {
			if (Algorithms.stringsEqual(wpt.category, prevPointsGroup.name)
					|| Algorithms.isEmpty(wpt.category) && Algorithms.isEmpty(prevPointsGroup.name)) {
				pointsUpdated = true;

				String category = updatedPointsGroup.name;
				String iconName = updateAppearance ? updatedPointsGroup.iconName : wpt.getIconName();
				String backgroundType = updateAppearance ? updatedPointsGroup.backgroundType : wpt.getBackgroundType();
				int color = updateAppearance ? updatedPointsGroup.color : wpt.colourARGB;

				if (gpxFile.showCurrentTrack) {
					app.getSavingTrackHelper().updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(),
							wpt.desc, wpt.name, category, color, iconName, backgroundType);
				} else {
					gpxFile.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(), wpt.desc, wpt.name,
							category, color, iconName, backgroundType);
				}
			}
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

		void updateGpxFinished(Exception errorMessage);
	}
}