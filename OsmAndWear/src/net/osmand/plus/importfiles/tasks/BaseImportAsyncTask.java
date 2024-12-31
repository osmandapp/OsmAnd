package net.osmand.plus.importfiles.tasks;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.importfiles.ImportHelper;

public abstract class BaseImportAsyncTask<Params, Progress, Result> extends BaseLoadAsyncTask<Params, Progress, Result> {

	private final ImportHelper importHelper;

	public BaseImportAsyncTask(@NonNull FragmentActivity activity) {
		super(activity);
		importHelper = app.getImportHelper();
	}

	protected void notifyImportFinished() {
		importHelper.notifyImportFinished();
	}
}