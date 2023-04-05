package net.osmand.plus.track.helpers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
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
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		for (String filePath : selectedItems.keySet()) {
			SelectedGpxFile sf;
			if (!filePath.equals(GpxSelectionHelper.CURRENT_TRACK)) {
				sf = selectionHelper.getSelectedFileByPath(filePath);
				if (sf == null) {
					sf = new SelectedGpxFile();
					sf.setGpxFile(new GPXFile(null), app);
				}
				sf.getGpxFile().path = filePath;
			} else {
				sf = selectionHelper.getSelectedCurrentRecordingTrack();
				if (sf == null) {
					sf = savingTrackHelper.getCurrentTrack();
				}
			}
			boolean visible = false;
			if (selectedItems.get(filePath) != null) {
				visible = selectedItems.get(filePath);
			}
			if (visible) {
				if (!sf.isShowCurrentTrack()) {
					sf.getGpxFile().modifiedTime = -1;
					sf.getGpxFile().pointsModifiedTime = -1;
				}
				originalSelectedItems.add(sf.getGpxFile());
			}
			selectionHelper.updateSelected(visible, sf);
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (gpxTaskListener != null) {
			gpxTaskListener.gpxSelectionFinished();
		}
	}

	public interface SelectGpxTaskListener {

		void gpxSelectionInProgress();

		void gpxSelectionStarted();

		void gpxSelectionFinished();

	}
}
