package net.osmand.plus.download.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

public class DownloadMapToolbarController extends TopToolbarController {

	private MapActivity mapActivity;
	private DownloadValidationManager downloadValidationManager;

	private boolean nightMode;

	private View btnClose;
	private View btnDownload;
	private TextView tvDescription;
	private TextView tvSize;

	private IndexItem indexItem;
	private String regionName;

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

		UiUtilities.setupDialogButton(nightMode, btnClose, UiUtilities.DialogButtonType.SECONDARY, mapActivity.getString(R.string.shared_string_close));
		UiUtilities.setupDialogButton(nightMode, btnDownload, UiUtilities.DialogButtonType.PRIMARY, mapActivity.getString(R.string.shared_string_download));

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
				Typeface typeface = FontCache.getRobotoMedium(mapActivity);
				description.setSpan(new CustomTypefaceSpan(typeface), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			tvDescription.setText(description);
		}

		if (indexItem != null) {
			String size = indexItem.getSizeDescription(mapActivity);
			tvSize.setText(size);

			btnDownload.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					downloadValidationManager.startDownload(mapActivity, indexItem);
					dismiss();
				}
			});
		}

		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}

	private void dismiss() {
		lastProcessedRegionName = regionName;
		mapActivity.hideTopToolbar(DownloadMapToolbarController.this);
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
