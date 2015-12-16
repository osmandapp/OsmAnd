package net.osmand.plus.liveupdates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.IncrementalChangesManager;

import org.apache.commons.logging.Log;

import java.util.List;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);
	@Override
	public void onReceive(Context context, Intent intent) {
		String localIndexInfo = intent.getAction();
		new PerformLiveUpdateAsyncTask(context).execute(localIndexInfo);
	}

	public static class PerformLiveUpdateAsyncTask
			extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {
		private final Context context;

		public PerformLiveUpdateAsyncTask(Context context) {
			this.context = context;
		}

		protected void onPreExecute() {

		}

		@Override
		protected IncrementalChangesManager.IncrementalUpdateList doInBackground(String... params) {
			final OsmandApplication myApplication = (OsmandApplication) context.getApplicationContext();
			IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
			return cm.getUpdatesByMonth(params[0]);
		}

		protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
			if (result.errorMessage != null) {
				Toast.makeText(context, result.errorMessage, Toast.LENGTH_SHORT).show();
			} else {
				List<IncrementalChangesManager.IncrementalUpdate> ll = result.getItemsForUpdate();
				if (ll.isEmpty()) {
					Toast.makeText(context, R.string.no_updates_available, Toast.LENGTH_SHORT).show();
				} else {
					int i = 0;
					IndexItem[] is = new IndexItem[ll.size()];
					for (IncrementalChangesManager.IncrementalUpdate iu : ll) {
						IndexItem ii = new IndexItem(iu.fileName, "Incremental update", iu.timestamp, iu.sizeText,
								iu.contentSize, iu.containerSize, DownloadActivityType.LIVE_UPDATES_FILE);
						is[i++] = ii;
					}
					final OsmandApplication application = (OsmandApplication) context.getApplicationContext();
					DownloadValidationManager downloadValidationManager =
							new DownloadValidationManager(application);
					downloadValidationManager.startDownload(context, is);
				}
			}
		}
	}
}
