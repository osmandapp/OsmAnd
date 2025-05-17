package net.osmand.plus.download.ui;


import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;

public class DownloadMapToolbarController extends SuggestMapToolbarController {

	private final DownloadValidationManager downloadValidationManager;
	private final IndexItem indexItem;

	private final TextView tvSize;

	public DownloadMapToolbarController(@NonNull MapActivity mapActivity, @NonNull IndexItem indexItem, @NonNull String regionName) {
		super(mapActivity, regionName, R.layout.download_detailed_map_widget);
		this.indexItem = indexItem;
		downloadValidationManager = new DownloadValidationManager(app);
		tvSize = mainView.findViewById(R.id.fileSize);
		initializeUI();
	}

	@NonNull
	public IndexItem getIndexItem() {
		return indexItem;
	}

	@Override
	protected void refreshView() {
		super.refreshView();
		if (indexItem != null) {
			String size = indexItem.getSizeDescription(mapActivity);
			tvSize.setText(size);
		}
	}

	@Override
	protected int getSummaryPattern() {
		return R.string.download_detailed_map;
	}

	@Override
	protected void onApply() {
		downloadValidationManager.startDownload(mapActivity, indexItem);
		dismiss();
	}
}
