package net.osmand.plus.download.newimplementation;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;

public final class DownloadsUiHelper {
	private DownloadsUiHelper() {
	}

	public static void initFreeVersionBanner(View header, OsmandApplication application,
											 Resources resources) {
		final View freeVersionBanner = header.findViewById(R.id.freeVersionBanner);
		OsmandSettings settings = application.getSettings();
		if(!Version.isFreeVersion(application) && !settings.SHOULD_SHOW_FREE_VERSION_BANNER.get()) {
			freeVersionBanner.setVisibility(View.GONE);
			return;
		}

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
		freeVersionBanner.setOnClickListener(
				new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
						buttonsLinearLayout));

		ProgressBar downloadsLeftProgressBar =
				(ProgressBar) header.findViewById(R.id.downloadsLeftProgressBar);
		downloadsLeftProgressBar.setProgress(settings.NUMBER_OF_FREE_DOWNLOADS.get());

		header.findViewById(R.id.getFullVersionButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BaseDownloadActivity context = (BaseDownloadActivity) v.getContext();
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(Version.marketPrefix(context.getMyApplication())
								+ "net.osmand.plus"));
				try {
					context.startActivity(intent);
				} catch (ActivityNotFoundException e) {
				}
			}
		});
		header.findViewById(R.id.laterButton).setOnClickListener(
				new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
						buttonsLinearLayout));
	}

	public static void showDialog(FragmentActivity activity, DialogFragment fragment) {
		FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
		Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		fragment.show(ft, "dialog");
	}

	private static class ToggleCollapseFreeVersionBanner implements View.OnClickListener {
		private final View freeVersionDescriptionTextView;
		private final View buttonsLinearLayout;

		private ToggleCollapseFreeVersionBanner(View freeVersionDescriptionTextView,
												View buttonsLinearLayout) {
			this.freeVersionDescriptionTextView = freeVersionDescriptionTextView;
			this.buttonsLinearLayout = buttonsLinearLayout;
		}

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
			final String format = resources.getString(R.string.downloading_number_of_files);
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
