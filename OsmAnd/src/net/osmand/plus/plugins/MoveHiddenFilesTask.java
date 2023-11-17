package net.osmand.plus.plugins;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class MoveHiddenFilesTask extends AsyncTask<Void, Void, Void> {
	private static final Log LOG = PlatformUtil.getLog(MoveHiddenFilesTask.class);

	private ArrayList<Pair<File, File>> filesToMove;
	private OsmandApplication app;

	MoveHiddenFilesTask(@NonNull OsmandApplication app, @NonNull ArrayList<Pair<File, File>> filesToMove) {
		this.filesToMove = filesToMove;
		this.app = app;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		for (Pair<File, File> fileToCopy : filesToMove) {
			File origin = fileToCopy.first;
			File target = fileToCopy.second;
			if(copyFiles(origin, target)){
				origin.delete();
			}
		}
		return null;
	}

	private boolean copyFiles(@NonNull File origin, @NonNull File target) {
		boolean success = true;
		Algorithms.createParentDirsForFile(target);
		try {
			Algorithms.fileCopy(origin, target);
		} catch (IOException e) {
			success = false;
			LOG.error("Failed copy " + origin.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
		}
		return success;
	}

	@Override
	protected void onPostExecute(Void unused) {
		app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
	}
}