package net.osmand.plus.osmedit;

import java.io.File;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.osmedit.OsmEditingPlugin.UploadVisibility;
import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

public class UploadGPXFilesTask extends AsyncTask<GpxInfo, String, String> {

	private final String visibility;
	private final String description;
	private final String tagstring;
	private Activity la;

	public UploadGPXFilesTask(Activity la,
			String description, String tagstring, UploadVisibility visibility) {
		this.la = la;
		this.description = description;
		this.tagstring = tagstring;
		this.visibility = visibility != null ? visibility.asURLparam() : UploadVisibility.Private.asURLparam();

	}

	@Override
	protected String doInBackground(GpxInfo... params) {
		int count = 0;
		int total = 0;
		for (GpxInfo info : params) {
			if (!isCancelled() && info.file != null) {
				String warning = null;
				File file = info.file;
				warning = new OpenstreetmapRemoteUtil((OsmandApplication) la.getApplication()).uploadGPXFile(tagstring, description, visibility,
						file);
				total++;
				if (warning == null) {
					count++;
				} else {
					publishProgress(warning);
				}
			}
		}
		return la.getString(R.string.local_index_items_uploaded, count, total);
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length > 0) {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (i > 0) {
					b.append("\n");
				}
				b.append(values[i]);
			}
			AccessibleToast.makeText(la, b.toString(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onPreExecute() {
		la.setProgressBarIndeterminateVisibility(true);
	}

	@Override
	protected void onPostExecute(String result) {
		la.setProgressBarIndeterminateVisibility(false);
		AccessibleToast.makeText(la, result, Toast.LENGTH_LONG).show();
	}

}