package net.osmand.plus.liveupdates;

import android.os.AsyncTask;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.LocalIndexHelper;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.download.LocalIndexType;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class LoadLiveMapsTask
		extends AsyncTask<Void, LocalIndexInfo, Void>
		implements AbstractLoadLocalIndexTask {

	public interface LocalIndexInfoAdapter {
		void addData(@NonNull List<LocalIndexInfo> indexes);

		void clearData();

		void onDataUpdated();
	}

	private final LocalIndexInfoAdapter adapter;
	private final LocalIndexHelper helper;

	public LoadLiveMapsTask(LocalIndexInfoAdapter adapter, OsmandApplication app) {
		this.adapter = adapter;
		helper = new LocalIndexHelper(app);
	}

	@Override
	protected void onPreExecute() {
		adapter.clearData();
	}

	@Override
	protected Void doInBackground(Void... params) {
		helper.getLocalFullMaps(this);
		return null;
	}

	@Override
	public void loadFile(LocalIndexInfo... loaded) {
		publishProgress(loaded);
	}

	@Override
	protected void onProgressUpdate(LocalIndexInfo... values) {
		List<LocalIndexInfo> matchingIndexes = new ArrayList<>();
		for (LocalIndexInfo localIndexInfo : values) {
			String fileNameLC = localIndexInfo.getFileName().toLowerCase();
			if (localIndexInfo.getType() == LocalIndexType.MAP_DATA
					&& !fileNameLC.contains("world") && !fileNameLC.startsWith("depth_")) {
				matchingIndexes.add(localIndexInfo);
			}
		}

		if (!Algorithms.isEmpty(matchingIndexes)) {
			adapter.addData(matchingIndexes);
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		adapter.onDataUpdated();
	}
}
