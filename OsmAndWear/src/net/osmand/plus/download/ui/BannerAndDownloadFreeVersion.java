package net.osmand.plus.download.ui;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB_NUMBER;
import static net.osmand.plus.download.DownloadActivity.UPDATES_TAB_NUMBER;

import android.os.AsyncTask;
import android.os.StatFs;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ibm.icu.impl.IllegalIcuArgumentException;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.io.File;

public class BannerAndDownloadFreeVersion {

	private static final Log LOG = PlatformUtil.getLog(BannerAndDownloadFreeVersion.class);

	private final View progressLayout;
	private final ProgressBar progressBar;
	private final TextView leftTextView;
	private final TextView rightTextView;

	private final DownloadActivity activity;
	private final FreeVersionBanner freeVersionBanner;
	private final boolean showSpace;

	public BannerAndDownloadFreeVersion(@NonNull View view, @NonNull DownloadActivity activity, boolean showSpace) {
		this.activity = activity;
		this.showSpace = showSpace;

		progressLayout = view.findViewById(R.id.downloadProgressLayout);
		progressBar = progressLayout.findViewById(R.id.progressBar);
		leftTextView = progressLayout.findViewById(R.id.leftTextView);
		rightTextView = progressLayout.findViewById(R.id.rightTextView);

		freeVersionBanner = new FreeVersionBanner(view, activity);
		freeVersionBanner.initFreeVersionBanner();
		updateBannerInProgress();
	}

	public void updateFreeVersionBanner() {
		freeVersionBanner.updateFreeVersionBanner();
	}

	public void updateBannerInProgress() {
		BasicProgressAsyncTask<?, ?, ?, ?> progressTask = activity.getDownloadThread().getCurrentRunningTask();
		boolean isFinished = progressTask == null || progressTask.getStatus() == AsyncTask.Status.FINISHED;

		int tab = activity.getCurrentTab();
		boolean visible = tab != LOCAL_TAB_NUMBER && (!isFinished || tab != UPDATES_TAB_NUMBER || showSpace);
		AndroidUiHelper.updateVisibility(progressLayout, visible);

		if (isFinished) {
			progressLayout.setOnClickListener(null);
			updateDescriptionTextWithSize(activity.getMyApplication(), progressLayout);
			freeVersionBanner.updateFreeVersionBanner();
		} else {
			freeVersionBanner.setMinimizedFreeVersionBanner(true);
			freeVersionBanner.updateAvailableDownloads();
			progressLayout.setOnClickListener(v -> new ActiveDownloadsDialogFragment().show(activity.getSupportFragmentManager(), "dialog"));

			String message = progressTask.getDescription();
			boolean indeterminate = progressTask.isIndeterminate();
			progressBar.setIndeterminate(indeterminate);
			if (indeterminate) {
				leftTextView.setText(message);
				rightTextView.setText(null);
			} else {
				int percent = (int) progressTask.getDownloadProgress();
				progressBar.setProgress(percent);
				leftTextView.setText(message);
				rightTextView.setText(percent + "%");
			}
		}
	}

	public static void updateDescriptionTextWithSize(@NonNull OsmandApplication app, @NonNull View view) {
		TextView descriptionText = view.findViewById(R.id.rightTextView);
		TextView messageTextView = view.findViewById(R.id.leftTextView);
		ProgressBar sizeProgress = view.findViewById(R.id.progressBar);

		File dir = app.getAppPath(null);
		String size = "";
		int percent = 0;
		if (dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				size = AndroidUtils.formatSize(app, (fs.getAvailableBlocksLong()) * fs.getBlockSizeLong());
				percent = 100 - (int) (fs.getAvailableBlocksLong() * 100 / fs.getBlockCountLong());
			} catch (IllegalIcuArgumentException e) {
				LOG.error(e);
			}
		}
		sizeProgress.setIndeterminate(false);
		sizeProgress.setProgress(percent);
		String text = app.getString(R.string.free, size);
		descriptionText.setText(text);
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());

		messageTextView.setText(R.string.device_memory);
	}
}
