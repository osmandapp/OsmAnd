package net.osmand.plus.liveupdates;

import android.os.AsyncTask;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.IncrementalChangesManager;

import java.util.List;

public class PerformLiveUpdateAsyncTask
		extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {
	private final AbstractDownloadActivity activity;

	public PerformLiveUpdateAsyncTask(AbstractDownloadActivity activity) {
		this.activity = activity;
	}

	protected void onPreExecute() {
		activity.setSupportProgressBarIndeterminateVisibility(true);

	}

	@Override
	protected IncrementalChangesManager.IncrementalUpdateList doInBackground(String... params) {
		final OsmandApplication myApplication = activity.getMyApplication();
		IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
		return cm.getUpdatesByMonth(params[0]);
	}

	protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
		activity.setSupportProgressBarIndeterminateVisibility(false);
		if (result.errorMessage != null) {
			Toast.makeText(activity, result.errorMessage, Toast.LENGTH_SHORT).show();
		} else {
			List<IncrementalChangesManager.IncrementalUpdate> ll = result.getItemsForUpdate();
			if (ll.isEmpty()) {
				Toast.makeText(activity, R.string.no_updates_available, Toast.LENGTH_SHORT).show();
			} else {
				int i = 0;
				IndexItem[] is = new IndexItem[ll.size()];
				for (IncrementalChangesManager.IncrementalUpdate iu : ll) {
					IndexItem ii = new IndexItem(iu.fileName, "Incremental update", iu.timestamp, iu.sizeText,
							iu.contentSize, iu.containerSize, DownloadActivityType.LIVE_UPDATES_FILE);
					is[i++] = ii;
				}
				activity.startDownload(is);
			}
		}
	}
}
