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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectGpxTask extends AsyncTask<Void, Void, String> {

	private final OsmandApplication app;
	private final GpxSelectionHelper selectionHelper;
	private final Set<GPXFile> originalSelectedItems = new HashSet<>();
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
	protected String doInBackground(Void... voids) {
		for (GPXFile gpxFile : originalSelectedItems) {
			if (isCancelled()) {
				break;
			}
			if (!gpxFile.showCurrentTrack) {
				gpxFile = GPXUtilities.loadGPXFile(new File(gpxFile.path));
			}
			GpxSelectionParams params = GpxSelectionParams.newInstance()
					.showOnMap().selectedByUser().syncGroup()
					.addToHistory().addToMarkers().saveSelection();
			selectionHelper.selectGpxFile(gpxFile, params);
			publishProgress();
		}
		return "";
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		if (gpxTaskListener != null) {
			gpxTaskListener.gpxSelectionInProgress();
		}
	}

	@Override
	protected void onPreExecute() {
		collectSelectedItems();
		if (gpxTaskListener != null) {
			gpxTaskListener.gpxSelectionStarted();
		}
	}

	private void collectSelectedItems() {
		for (String filePath : selectedItems.keySet()) {
			SelectedGpxFile selectedGpxFile;
			if (filePath.equals(CURRENT_TRACK)) {
				selectedGpxFile = selectionHelper.getSelectedCurrentRecordingTrack();
				if (selectedGpxFile == null) {
					selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
				}
			} else {
				selectedGpxFile = selectionHelper.getSelectedFileByPath(filePath);
				if (selectedGpxFile == null) {
					selectedGpxFile = new SelectedGpxFile();
					selectedGpxFile.setGpxFile(new GPXFile(null), app);
				}
				selectedGpxFile.getGpxFile().path = filePath;
			}
			boolean visible = false;
			if (selectedItems.get(filePath) != null) {
				visible = selectedItems.get(filePath);
			}
			if (visible) {
				if (!selectedGpxFile.isShowCurrentTrack()) {
					selectedGpxFile.getGpxFile().modifiedTime = -1;
					selectedGpxFile.getGpxFile().pointsModifiedTime = -1;
				}
				originalSelectedItems.add(selectedGpxFile.getGpxFile());
			}
			selectionHelper.updateSelected(visible, selectedGpxFile);
		}
	}

	@Override
	protected void onPostExecute(String result) {
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
