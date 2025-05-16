package net.osmand.plus.download.ui;

import static net.osmand.IProgress.EMPTY_PROGRESS;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;

public class ActivateMapToolbarController extends SuggestMapToolbarController {

	private final LocalItem localItem;

	public ActivateMapToolbarController(@NonNull MapActivity mapActivity,
	                                    @NonNull LocalItem localItem, @NonNull String regionName) {
		super(mapActivity, regionName, R.layout.activate_map_widget);
		this.localItem = localItem;
		initializeUI();
	}

	@Override
	protected int getSummaryPattern() {
		return R.string.suggest_activate_map_msg;
	}

	@Override
	protected void onApply() {
		if (localItem != null) {
			LocalOperationTask task = new LocalOperationTask(app, OperationType.RESTORE_OPERATION, new OperationListener() {
				@Override
				public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
					app.getResourceManager().reloadIndexesAsync(EMPTY_PROGRESS, warnings -> app.getOsmandMap().refreshMap());
				}
			});
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, localItem);
		}
		dismiss();
	}
}
