package net.osmand.plus.card.color.palette.gradient;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;

public class DuplicateGradientTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final String gradientFileName;
	private final DuplicateGradientListener listener;

	public DuplicateGradientTask(@NonNull OsmandApplication app, @NonNull String gradientFileName, @NonNull DuplicateGradientListener listener) {
		this.app = app;
		this.gradientFileName = gradientFileName;
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		File fileToDuplicate = new File(app.getAppPath(IndexConstants.CLR_PALETTE_DIR), gradientFileName);
		if (!fileToDuplicate.exists()) {
			PlatformUtil.getLog(DuplicateGradientTask.class).error("File to duplicate doesn't exist");
			return false;
		}

		File palettesDir = app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
		String newFilename = fileToDuplicate.getName();
		do {
			newFilename = AndroidUtils.createNewFileName(newFilename);
		} while (new File(palettesDir, newFilename).exists());

		try {
			Algorithms.fileCopy(fileToDuplicate, new File(palettesDir, newFilename));
		} catch (IOException e) {
			PlatformUtil.getLog(DuplicateGradientTask.class).error("Error copying file");
			return false;
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean successful) {
		listener.onDuplicatingFinished(successful);
	}

	public interface DuplicateGradientListener {
		void onDuplicatingFinished(boolean duplicated);
	}
}
