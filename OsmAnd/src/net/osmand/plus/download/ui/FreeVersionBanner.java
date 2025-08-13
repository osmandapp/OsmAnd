package net.osmand.plus.download.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.plus.chooseplan.OsmAndFeature.UNLIMITED_MAP_DOWNLOADS;
import static net.osmand.plus.download.DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.settings.backend.OsmandSettings;

public class FreeVersionBanner {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final DownloadActivity activity;

	private final View freeVersionBanner;
	private final View freeVersionBannerTitle;
	private final TextView freeVersionDescriptionTextView;
	private final TextView downloadsLeftTextView;
	private final ProgressBar downloadsLeftProgressBar;

	private final OnClickListener onCollapseListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (freeVersionDescriptionTextView.getVisibility() == View.VISIBLE
					&& DownloadActivity.isDownloadingPermitted(settings)) {
				collapseBanner();
			} else {
				app.logEvent("click_free_dialog");
				ChoosePlanFragment.showInstance(activity, UNLIMITED_MAP_DOWNLOADS);
			}
		}
	};

	public FreeVersionBanner(@NonNull View view, @NonNull DownloadActivity activity) {
		this.activity = activity;
		this.app = activity.getMyApplication();
		this.settings = app.getSettings();

		freeVersionBanner = view.findViewById(R.id.freeVersionBanner);
		freeVersionBannerTitle = freeVersionBanner.findViewById(R.id.freeVersionBannerTitle);
		downloadsLeftTextView = freeVersionBanner.findViewById(R.id.downloadsLeftTextView);
		downloadsLeftProgressBar = freeVersionBanner.findViewById(R.id.downloadsLeftProgressBar);
		freeVersionDescriptionTextView = freeVersionBanner.findViewById(R.id.freeVersionDescriptionTextView);
	}

	private void collapseBanner() {
		freeVersionDescriptionTextView.setVisibility(View.GONE);
		freeVersionBannerTitle.setVisibility(View.VISIBLE);
	}

	public void expandBanner() {
		freeVersionDescriptionTextView.setVisibility(View.VISIBLE);
		freeVersionBannerTitle.setVisibility(View.VISIBLE);
	}

	public void initFreeVersionBanner() {
		if (!DownloadActivity.shouldShowFreeVersionBanner(app)) {
			freeVersionBanner.setVisibility(View.GONE);
			return;
		}
		freeVersionBanner.setVisibility(View.VISIBLE);
		downloadsLeftProgressBar.setMax(MAXIMUM_AVAILABLE_FREE_DOWNLOADS);
		freeVersionDescriptionTextView.setText(DownloadValidationManager.getFreeVersionMessage(activity));

		LinearLayout marksContainer = freeVersionBanner.findViewById(R.id.marksLinearLayout);

		Space spaceView = new Space(activity);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1);
		spaceView.setLayoutParams(params);
		marksContainer.addView(spaceView);

		int markWidth = (int) (1 * activity.getResources().getDisplayMetrics().density);
		int colorBlack = activity.getColor(R.color.activity_background_color_dark);
		for (int i = 1; i < MAXIMUM_AVAILABLE_FREE_DOWNLOADS; i++) {
			View markView = new View(activity);
			params = new LinearLayout.LayoutParams(markWidth, MATCH_PARENT);
			markView.setLayoutParams(params);
			markView.setBackgroundColor(colorBlack);
			marksContainer.addView(markView);

			spaceView = new Space(activity);
			params = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1);
			spaceView.setLayoutParams(params);
			marksContainer.addView(spaceView);
		}
		updateFreeVersionBanner();
		collapseBanner();
	}

	public void updateFreeVersionBanner() {
		if (!DownloadActivity.shouldShowFreeVersionBanner(app)) {
			if (freeVersionBanner.getVisibility() == View.VISIBLE) {
				freeVersionBanner.setVisibility(View.GONE);
			}
			return;
		}
		setMinimizedFreeVersionBanner(false);
		int mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
		downloadsLeftProgressBar.setProgress(mapsDownloaded);
		int downloadsLeft = MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
		downloadsLeft = Math.max(downloadsLeft, 0);
		downloadsLeftTextView.setText(activity.getString(R.string.downloads_left_template, String.valueOf(downloadsLeft)));
		freeVersionBanner.findViewById(R.id.bannerTopLayout).setOnClickListener(onCollapseListener);
	}

	protected void setMinimizedFreeVersionBanner(boolean minimize) {
		if (minimize && DownloadActivity.isDownloadingPermitted(settings)) {
			collapseBanner();
			freeVersionBannerTitle.setVisibility(View.GONE);
		} else {
			freeVersionBannerTitle.setVisibility(View.VISIBLE);
		}
	}

	protected void updateAvailableDownloads() {
		int activeTasks = activity.getDownloadThread().getCountedDownloads();
		int mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get() + activeTasks;
		downloadsLeftProgressBar.setProgress(mapsDownloaded);
	}
}
