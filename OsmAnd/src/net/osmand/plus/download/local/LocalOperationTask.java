package net.osmand.plus.download.local;

import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.dialogs.LiveGroupItem;

public class LocalOperationTask extends AsyncTask<BaseLocalItem, BaseLocalItem, String> {

	private final OsmandApplication app;
	private final OperationType type;
	private final OperationListener listener;
	private final LocalOperationHelper localOperationHelper;

	public LocalOperationTask(@NonNull OsmandApplication app, @NonNull OperationType type,
	                          @Nullable OperationListener listener) {
		this.app = app;
		this.type = type;
		this.listener = listener;
		this.localOperationHelper = new LocalOperationHelper(app);
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onOperationStarted();
		}
	}

	@Override
	protected void onProgressUpdate(BaseLocalItem... values) {
		if (listener != null) {
			listener.onOperationProgress(type, values);
		}
	}

	@Override
	protected String doInBackground(BaseLocalItem... params) {
		int count = 0;
		int total = 0;
		for (BaseLocalItem item : params) {
			if (!isCancelled()) {
				boolean success = processItem(item);
				total++;

				if (success) {
					count++;
					publishProgress(item);
				}
			}
		}
		if (type == DELETE_OPERATION) {
			app.getDownloadThread().updateLoadedFiles();
		}
		if (type == DELETE_OPERATION) {
			return app.getString(R.string.local_index_items_deleted, count, total);
		} else if (type == BACKUP_OPERATION) {
			return app.getString(R.string.local_index_items_backuped, count, total);
		} else if (type == RESTORE_OPERATION) {
			return app.getString(R.string.local_index_items_restored, count, total);
		}
		return "";
	}

	@Override
	protected void onPostExecute(String result) {
		if (listener != null) {
			listener.onOperationFinished(type, result);
		}
	}

	private boolean processItem(@NonNull BaseLocalItem item) {
		if (item instanceof LocalItem) {
			return processItem((LocalItem) item);
		} else if (item instanceof LiveGroupItem groupItem) {
			boolean success = false;
			for (LocalItem localItem : groupItem.getItems()) {
				if (!isCancelled()) {
					success |= processItem(localItem);
				}
			}
			return success;
		}
		return false;
	}

	private boolean processItem(@NonNull LocalItem item) {
		return localOperationHelper.execute(item, type);
	}

	public interface OperationListener {
		default void onOperationStarted() {

		}

		default void onOperationProgress(@NonNull OperationType type, @NonNull BaseLocalItem... items) {

		}

		default void onOperationFinished(@NonNull OperationType type, @NonNull String result) {

		}
	}
}
