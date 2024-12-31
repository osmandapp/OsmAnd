package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditsUploadListener;
import net.osmand.plus.plugins.osmedit.helpers.OsmEditsUploadListenerHelper;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadOpenstreetmapPointAsyncTask;

import java.util.Map;

public class SimpleProgressDialogPoiUploader implements ProgressDialogPoiUploader {

	private final MapActivity mapActivity;

	public SimpleProgressDialogPoiUploader(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously) {
		ProgressDialogFragment dialog = ProgressDialogFragment.createInstance(
				R.string.uploading,
				R.string.local_openstreetmap_uploading,
				ProgressDialog.STYLE_HORIZONTAL);
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(mapActivity,
				mapActivity.getString(R.string.local_openstreetmap_were_uploaded)) {
			@Override
			public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
				super.uploadEnded(loadErrorsMap);
				mapActivity.getContextMenu().close();
				OsmBugsLayer l = mapActivity.getMapView().getLayerByClass(OsmBugsLayer.class);
				if (l != null) {
					l.clearCache();
					mapActivity.refreshMap();
				}
			}
		};
		dialog.show(mapActivity.getSupportFragmentManager(), ProgressDialogFragment.TAG);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
				dialog, listener, plugin, points.length, closeChangeSet, anonymously);
		uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, points);
	}
}