package net.osmand.plus.download.ui;


import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;

public class DownloadMapToolbarController extends SuggestMapToolbarController {

	private final DownloadValidationManager downloadValidationManager;
	private final IndexItem indexItem;

	public DownloadMapToolbarController(@NonNull MapActivity mapActivity, @NonNull IndexItem indexItem, @NonNull String regionName) {
		super(mapActivity, regionName);
		this.indexItem = indexItem;
		downloadValidationManager = new DownloadValidationManager(app);
		initializeUI();
	}

	@Override
	protected int getPrimaryTextPattern() {
		return R.string.download_detailed_map;
	}

	@NonNull
	@Override
	protected String getSecondaryText() {
		return indexItem.getSizeDescription(mapActivity);
	}

	@Override
	protected int getIconId() {
		return R.drawable.img_download;
	}

	@Override
	protected int getPreferredIconHeight() {
		return LayoutParams.WRAP_CONTENT;
	}

	@Override
	protected int getPreferredIconWidth() {
		return LayoutParams.WRAP_CONTENT;
	}

	@NonNull
	@Override
	protected String getApplyButtonTitle() {
		return app.getString(R.string.shared_string_download);
	}

	@Override
	protected void onApply() {
		downloadValidationManager.startDownload(mapActivity, indexItem);
		dismiss();
	}
}
