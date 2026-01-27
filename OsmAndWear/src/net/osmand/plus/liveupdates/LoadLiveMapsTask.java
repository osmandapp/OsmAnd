package net.osmand.plus.liveupdates;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class LoadLiveMapsTask extends AsyncTask<Void, LocalItem, Void> implements AbstractLoadLocalIndexTask {

	public interface LocalIndexInfoAdapter {
		void addData(@NonNull List<LocalItem> indexes);

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
	public void loadFile(LocalItem... loaded) {
		publishProgress(loaded);
	}

	@Override
	protected void onProgressUpdate(LocalItem... values) {
		List<LocalItem> matchingIndexes = new ArrayList<>();
		for (LocalItem indexInfo : values) {
			String fileNameLC = indexInfo.getFileName().toLowerCase();
			if (indexInfo.getType() == LocalItemType.MAP_DATA
					&& !fileNameLC.contains("world") && !fileNameLC.startsWith("depth_")) {
				matchingIndexes.add(indexInfo);
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
