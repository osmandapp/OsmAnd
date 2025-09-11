package net.osmand.plus.download;

import android.os.Bundle;

import net.osmand.plus.activities.ActionBarProgressActivity;

public class AbstractDownloadActivity extends ActionBarProgressActivity {

	protected DownloadValidationManager downloadValidationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		downloadValidationManager = new DownloadValidationManager(app);
	}

	public void startDownload(IndexItem... indexItem) {
		downloadValidationManager.startDownload(this, indexItem);
	}

	public void makeSureUserCancelDownload(DownloadItem item) {
		downloadValidationManager.makeSureUserCancelDownload(this, item);
	}
}