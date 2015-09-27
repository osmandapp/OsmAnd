package net.osmand.plus.download.newimplementation;

import android.content.res.Resources;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.BaseDownloadActivity;

public final class DownloadsUiInitHelper {
	private DownloadsUiInitHelper() {
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
}
