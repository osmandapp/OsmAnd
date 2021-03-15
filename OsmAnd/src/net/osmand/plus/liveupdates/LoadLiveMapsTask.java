package net.osmand.plus.liveupdates;

import android.os.AsyncTask;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;

import java.util.List;

public class LoadLiveMapsTask
		extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
		implements AbstractLoadLocalIndexTask {

	public interface LocalIndexInfoAdapter {
		void addData(LocalIndexInfo localIndexInfo);

		void clearData();

		void onDataUpdated();
	}

	//private List<LocalIndexInfo> result;
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
	protected List<LocalIndexInfo> doInBackground(Void... params) {
		return helper.getLocalFullMaps(this);
	}

	@Override
	public void loadFile(LocalIndexInfo... loaded) {
		publishProgress(loaded);
	}

	@Override
	protected void onProgressUpdate(LocalIndexInfo... values) {
		String fileNameL;
		for (LocalIndexInfo localIndexInfo : values) {
			fileNameL = localIndexInfo.getFileName().toLowerCase();
			if (localIndexInfo.getType() == LocalIndexHelper.LocalIndexType.MAP_DATA
					&& !fileNameL.contains("world") && !fileNameL.startsWith("depth_")) {
				adapter.addData(localIndexInfo);
			}
		}
	}

	@Override
	protected void onPostExecute(List<LocalIndexInfo> result) {
		//this.result = result;
		adapter.onDataUpdated();
	}
}
