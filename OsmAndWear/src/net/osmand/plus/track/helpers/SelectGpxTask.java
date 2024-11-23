package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.shared.io.KFile;

import java.util.List;

public class SelectGpxTask extends AsyncTask<Void, SelectedGpxFile, Void> {

	private final OsmandApplication app;
	private final GpxSelectionHelper selectionHelper;
	private final List<String> selectedPaths;
	private final SelectGpxTaskListener listener;

	SelectGpxTask(@NonNull OsmandApplication app, @NonNull List<String> selectedPaths, @Nullable SelectGpxTaskListener listener) {
		this.app = app;
		this.selectionHelper = app.getSelectedGpxHelper();
		this.selectedPaths = selectedPaths;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onGpxSelectionStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		processSelectedItems();
		return null;
	}

	private void processSelectedItems() {
		GpxSelectionParams selectionParams = GpxSelectionParams.getDefaultSelectionParams();
		for (String path : selectedPaths) {
			if (isCancelled()) {
				break;
			}
			SelectedGpxFile selectedGpxFile;
			if (path.equals(CURRENT_TRACK)) {
				selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
				selectionHelper.updateSelected(true, selectedGpxFile);
			} else {
				selectedGpxFile = selectionHelper.getSelectedFileByPath(path);
				if (selectedGpxFile == null) {
					GpxFile gpxFile = SharedUtil.loadGpxFile(new KFile(path));
					selectedGpxFile = selectionHelper.selectGpxFile(gpxFile, selectionParams);
				} else {
					selectionHelper.updateSelected(true, selectedGpxFile);
				}
			}
			publishProgress(selectedGpxFile);
		}
	}

	@Override
	protected void onProgressUpdate(SelectedGpxFile... selectedGpxFiles) {
		if (listener != null) {
			for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
				listener.onGpxSelectionInProgress(selectedGpxFile);
			}
		}
	}

	@Override
	protected void onPostExecute(Void v) {
		app.getOsmandMap().refreshMap();

		if (listener != null) {
			listener.onGpxSelectionFinished();
		}
	}

	public interface SelectGpxTaskListener {

		default void onGpxSelectionStarted() {

		}

		default void onGpxSelectionInProgress(@NonNull SelectedGpxFile selectedGpxFile) {

		}

		default void onGpxSelectionFinished() {

		}
	}
}
