package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.OperationLog;
import net.osmand.plus.OsmandApplication;

public abstract class BackupCommand extends AsyncTask<Object, Object, Object> {

	private final OsmandApplication app;
	private final BackupHelper helper;

	public BackupCommand(BackupHelper helper) {
		this.helper = helper;
		this.app = helper.getApp();
	}

	public OsmandApplication getApp() {
		return app;
	}

	public BackupHelper getHelper() {
		return helper;
	}

	protected OperationLog createOperationLog(@NonNull String name) {
		return new OperationLog(name, BackupHelper.DEBUG);
	}
}
