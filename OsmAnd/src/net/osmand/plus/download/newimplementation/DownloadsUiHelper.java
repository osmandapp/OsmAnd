package net.osmand.plus.download.newimplementation;

import android.content.res.Resources;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;

public final class DownloadsUiHelper {
	private DownloadsUiHelper() {
	}

	public static void initFreeVersionBanner(View header, OsmandSettings settings,
											 Resources resources) {
		TextView downloadsLeftTextView = (TextView) header.findViewById(R.id.downloadsLeftTextView);
		final int downloadsLeft = BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS
				- settings.NUMBER_OF_FREE_DOWNLOADS.get();
		downloadsLeftTextView.setText(resources.getString(R.string.downloads_left_template,
				downloadsLeft));
		final TextView freeVersionDescriptionTextView =
				(TextView) header.findViewById(R.id.freeVersionDescriptionTextView);
		freeVersionDescriptionTextView.setText(resources.getString(R.string.free_version_message,
				BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));

		final View buttonsLinearLayout = header.findViewById(R.id.buttonsLinearLayout);

		header.findViewById(R.id.freeVersionBanner).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (freeVersionDescriptionTextView.getVisibility() == View.VISIBLE) {
					freeVersionDescriptionTextView.setVisibility(View.GONE);
					buttonsLinearLayout.setVisibility(View.GONE);
				} else {
					freeVersionDescriptionTextView.setVisibility(View.VISIBLE);
					buttonsLinearLayout.setVisibility(View.VISIBLE);
				}
			}
		});

		ProgressBar downloadsLeftProgressBar =
				(ProgressBar) header.findViewById(R.id.downloadsLeftProgressBar);
		downloadsLeftProgressBar.setProgress(settings.NUMBER_OF_FREE_DOWNLOADS.get());
	}

	public static class MapDownloadListener implements DownloadActivity.OnProgressUpdateListener {
		private final View freeVersionBanner;
		private final View downloadProgressLayout;
		private final ProgressBar progressBar;
		private final TextView leftTextView;
		private final TextView rightTextView;
		private final Resources resources;

		public MapDownloadListener(View view, Resources resources) {
			this.resources = resources;
			freeVersionBanner = view.findViewById(R.id.freeVersionBanner);
			downloadProgressLayout = view.findViewById(R.id.downloadProgressLayout);
			progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
			leftTextView = (TextView) view.findViewById(R.id.leftTextView);
			rightTextView = (TextView) view.findViewById(R.id.rightTextView);
		}
		@Override
		public void onProgressUpdate(int progressPercentage, int activeTasks) {
			if (freeVersionBanner.getVisibility() == View.VISIBLE) {
				freeVersionBanner.setVisibility(View.GONE);
				downloadProgressLayout.setVisibility(View.VISIBLE);
			}
			progressBar.setProgress(progressPercentage);
			final String format = resources.getString(R.string.downloading_number_of_fiels);
			String numberOfTasks = String.format(format, activeTasks);
			leftTextView.setText(numberOfTasks);
			rightTextView.setText(progressPercentage + "%");
		}

		@Override
		public void onFinished() {
			freeVersionBanner.setVisibility(View.VISIBLE);
			downloadProgressLayout.setVisibility(View.GONE);
		}
	}
}
