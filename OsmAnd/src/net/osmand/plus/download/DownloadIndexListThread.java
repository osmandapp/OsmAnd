package net.osmand.plus.download;

import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.DownloadIndexActivity.DownloadActivityType;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.IndexItem;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

public class DownloadIndexListThread extends Thread {
	private DownloadIndexActivity uiActivity = null;
	private IndexFileList indexFiles = null;
	private final Context ctx;

	public DownloadIndexListThread(Context ctx) {
		super("DownloadIndexes");
		this.ctx = ctx;

	}

	public void setUiActivity(DownloadIndexActivity uiActivity) {
		this.uiActivity = uiActivity;
	}

	public List<IndexItem> getCachedIndexFiles() {
		return indexFiles != null ? indexFiles.getIndexFiles() : null;
	}

	public boolean isDownloadedFromInternet() {
		return indexFiles != null && indexFiles.isDownloadedFromInternet();
	}

	@Override
	public void run() {
		indexFiles = DownloadOsmandIndexesHelper.getIndexesList(ctx);
		if (uiActivity != null) {
			uiActivity.removeDialog(DownloadIndexActivity.DIALOG_PROGRESS_LIST);
			uiActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (indexFiles != null) {
						boolean basemapExists = ((OsmandApplication) uiActivity.getApplication()).getResourceManager().containsBasemap();
						if (!basemapExists && indexFiles.getBasemap() != null) {
							uiActivity.getEntriesToDownload().put(indexFiles.getBasemap().getFileName(), indexFiles.getBasemap()
									.createDownloadEntry(ctx, uiActivity.getType()));
							AccessibleToast.makeText(uiActivity, R.string.basemap_was_selected_to_download, Toast.LENGTH_LONG).show();
							uiActivity.findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
						}
						uiActivity.setListAdapter(new DownloadIndexAdapter(uiActivity, uiActivity.getFilteredByType()));
						if (indexFiles.isIncreasedMapVersion()) {
							uiActivity.showDialog(DownloadIndexActivity.DIALOG_MAP_VERSION_UPDATE);
						}
					} else {
						AccessibleToast.makeText(uiActivity, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}
}