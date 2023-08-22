package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.GpxSelectionParams;

import java.io.File;
import java.util.Map;

public class SelectGpxTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final GpxSelectionHelper selectionHelper;
	private final Map<String, Boolean> selectedItems;
	private final SelectGpxTaskListener gpxTaskListener;

	SelectGpxTask(@NonNull OsmandApplication app, @NonNull Map<String, Boolean> selectedItems,
	              @Nullable SelectGpxTaskListener gpxTaskListener) {
		this.app = app;
		this.selectionHelper = app.getSelectedGpxHelper();
		this.selectedItems = selectedItems;
		this.gpxTaskListener = gpxTaskListener;
	}

	@Override
	protected void onPreExecute() {
		if (gpxTaskListener != null) {
			gpxTaskListener.gpxSelectionStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		processSelectedItems();
		return null;
	}

	private void processSelectedItems() {
		for (Map.Entry<String, Boolean> entry : selectedItems.entrySet()) {
			if (isCancelled()) {
				break;
			}
			String filePath = entry.getKey();
			boolean visible = Boolean.TRUE.equals(selectedItems.get(filePath));

			if (filePath.equals(CURRENT_TRACK)) {
				selectionHelper.updateSelected(visible, app.getSavingTrackHelper().getCurrentTrack());
			} else {
				SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByPath(filePath);
				if (selectedGpxFile == null) {
					GPXFile gpxFile = GPXUtilities.loadGPXFile(new File(filePath));

					GpxSelectionParams params = GpxSelectionParams.newInstance()
							.showOnMap().selectedByUser().syncGroup()
							.addToHistory().addToMarkers().saveSelection();

					selectionHelper.selectGpxFile(gpxFile, params);
				} else {
					selectionHelper.updateSelected(visible, selectedGpxFile);
				}
			}
			publishProgress();
		}
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		if (gpxTaskListener != null) {
			gpxTaskListener.gpxSelectionInProgress();
		}
	}

	@Override
	protected void onPostExecute(Void v) {
		if (gpxTaskListener != null) {
			gpxTaskListener.gpxSelectionFinished();
		}
	}

	public interface SelectGpxTaskListener {

		default void gpxSelectionStarted() {

		}

		default void gpxSelectionInProgress() {

		}

		default void gpxSelectionFinished() {

		}
	}
}
