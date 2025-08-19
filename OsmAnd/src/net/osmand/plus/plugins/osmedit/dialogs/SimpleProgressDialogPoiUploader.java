package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmAndTaskManager;
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
		OsmEditingPlugin plugin = PluginsHelper.requirePlugin(OsmEditingPlugin.class);
		OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(mapActivity,
				mapActivity.getString(R.string.local_openstreetmap_were_uploaded)) {
			@Override
			public void uploadEnded(@NonNull Map<OsmPoint, String> loadErrorsMap) {
				super.uploadEnded(loadErrorsMap);
				mapActivity.getContextMenu().close();
				OsmBugsLayer l = mapActivity.getMapView().getLayerByClass(OsmBugsLayer.class);
				if (l != null) {
					l.clearCache();
					mapActivity.refreshMap();
				}
			}
		};
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
				showProgressDialog(), listener, plugin, points.length, closeChangeSet, anonymously);
		OsmAndTaskManager.executeTask(uploadTask, points);
	}

	@NonNull
	private ProgressDialogFragment showProgressDialog() {
		return ProgressDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
				R.string.uploading, R.string.local_openstreetmap_uploading, ProgressDialog.STYLE_HORIZONTAL);
	}
}