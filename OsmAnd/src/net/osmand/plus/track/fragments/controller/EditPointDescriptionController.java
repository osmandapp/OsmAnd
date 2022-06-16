package net.osmand.plus.track.fragments.controller;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.track.helpers.SavingTrackHelper;

import java.io.File;

public class EditPointDescriptionController {

	private final MapActivity mapActivity;

	public EditPointDescriptionController(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public void saveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		Object object = getObject();
		if (object instanceof FavouritePoint) {
			saveFavouriteDescription(editedText, (FavouritePoint) object);
		} else if (object instanceof WptPt) {
			saveWptDescription(editedText, (WptPt) object);
		}
		mapActivity.getContextMenu().updateMenuUI();
		callback.onDescriptionSaved();
	}

	private void saveFavouriteDescription(@NonNull String editedText, @NonNull FavouritePoint p) {
		FavouritesHelper helper = mapActivity.getMyApplication().getFavoritesHelper();
		helper.editFavouriteDescription(p, editedText);
		LatLon latLon = new LatLon(p.getLatitude(), p.getLongitude());
		updateContextMenu(latLon, p.getPointDescription(mapActivity), p);
	}

	private void saveWptDescription(@NonNull String editedText, @NonNull WptPt wpt) {
		OsmandApplication app = mapActivity.getMyApplication();
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
		if (selectedGpxFile != null && selectedGpxFile.getGpxFile() != null) {
			GPXFile gpx = selectedGpxFile.getGpxFile();
			if (gpx.showCurrentTrack) {
				SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
				savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(), editedText,
						wpt.name, wpt.category, wpt.getColor(), wpt.getIconName(), wpt.getBackgroundType());
			} else {
				gpx.updateWptPt(wpt, wpt.getLatitude(), wpt.getLongitude(), editedText, wpt.name,
						wpt.category, wpt.getColor(), wpt.getIconName(), wpt.getBackgroundType());
				saveGpx(gpx);
			}
			LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
			updateContextMenu(latLon, new WptLocationPoint(wpt).getPointDescription(mapActivity), wpt);
		}
	}

	private void saveGpx(@NonNull GPXFile gpxFile) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, new SaveGpxListener() {

			@Override
			public void gpxSavingStarted() { }

			@Override
			public void gpxSavingFinished(Exception errorMessage) { }

		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void updateContextMenu(LatLon latLon, PointDescription pointDescription, Object object) {
		MapContextMenu menu = mapActivity.getContextMenu();
		if (menu.getLatLon() != null && menu.getLatLon().equals(latLon)) {
			menu.update(latLon, pointDescription, object);
		}
	}

	@NonNull
	public String getTitle() {
		Object object = getObject();
		if (object instanceof FavouritePoint) {
			return ((FavouritePoint) object).getName();
		} else if (object instanceof WptPt) {
			return ((WptPt) object).name;
		}
		return mapActivity.getString(R.string.shared_string_description);
	}

	@Nullable
	public String getImageUrl() {
		return null;
	}

	@Nullable
	private Object getObject() {
		return mapActivity.getContextMenu().getObject();
	}

}
