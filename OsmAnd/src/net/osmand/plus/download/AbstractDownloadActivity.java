package net.osmand.plus.download;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.ActionBarProgressActivity;

import java.io.File;

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

	public void startReplacementDownload(@NonNull IndexItem indexItem, @NonNull File fileToDelete) {
		downloadValidationManager.startReplacementDownload(this, indexItem, fileToDelete);
	}

	public void makeSureUserCancelDownload(DownloadItem item) {
		downloadValidationManager.makeSureUserCancelDownload(this, item);
	}
}
