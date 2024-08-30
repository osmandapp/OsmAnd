package net.osmand.plus.download.ui;


import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

public class DownloadMapToolbarController extends TopToolbarController {

	private final MapActivity mapActivity;
	private final DownloadValidationManager downloadValidationManager;

	private final boolean nightMode;

	private final DialogButton btnClose;
	private final DialogButton btnDownload;
	private final TextView tvDescription;
	private final TextView tvSize;

	private final IndexItem indexItem;
	private final String regionName;

	private static String lastProcessedRegionName;

	public DownloadMapToolbarController(@NonNull MapActivity mapActivity, @NonNull IndexItem indexItem, @NonNull String regionName) {
		super(TopToolbarControllerType.DOWNLOAD_MAP);
		this.mapActivity = mapActivity;
		this.indexItem = indexItem;
		this.regionName = regionName;

		OsmandApplication app = mapActivity.getMyApplication();
		downloadValidationManager = new DownloadValidationManager(app);
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View mainView = View.inflate(new ContextThemeWrapper(mapActivity, themeRes), R.layout.download_detailed_map_widget, null);

		if (!AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			mainView.setBackgroundResource(getLandscapeBottomSidesBgResId());
		} else {
			mainView.setBackgroundResource(getPortraitBgResId());
		}

		tvDescription = mainView.findViewById(R.id.description);
		tvSize = mainView.findViewById(R.id.fileSize);
		btnClose = mainView.findViewById(R.id.btnClose);
		btnDownload = mainView.findViewById(R.id.btnDownload);

		refreshView();
		setBottomView(mainView);
		setTopViewVisible(false);
		setShadowViewVisible(false);
	}

	public static String getLastProcessedRegionName() {
		return lastProcessedRegionName;
	}

	public IndexItem getIndexItem() {
		return indexItem;
	}

	public String getRegionName() {
		return regionName;
	}

	private void refreshView() {
		if (!Algorithms.isEmpty(regionName)) {
			String descriptionText = String.format(mapActivity.getString(R.string.download_detailed_map), regionName);
			int startIndex = descriptionText.indexOf(regionName);
			int endIndex = startIndex + regionName.length();
			SpannableStringBuilder description = new SpannableStringBuilder(descriptionText);
			if (startIndex != -1 && endIndex != -1) {
				description.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			tvDescription.setText(description);
		}

		if (indexItem != null) {
			String size = indexItem.getSizeDescription(mapActivity);
			tvSize.setText(size);

			btnDownload.setOnClickListener(v -> {
				downloadValidationManager.startDownload(mapActivity, indexItem);
				dismiss();
			});
		}

		btnClose.setOnClickListener(v -> dismiss());
	}

	private void dismiss() {
		lastProcessedRegionName = regionName;
		mapActivity.hideTopToolbar(this);
	}

	@DrawableRes
	private int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_top_menu_dark : R.drawable.bg_top_menu_light;
	}

	@DrawableRes
	private int getLandscapeBottomSidesBgResId() {
		return nightMode ? R.drawable.bg_top_sheet_bottom_sides_landscape_dark : R.drawable.bg_top_sheet_bottom_sides_landscape_light;
	}
}
