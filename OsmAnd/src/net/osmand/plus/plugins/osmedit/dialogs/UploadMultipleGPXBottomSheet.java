package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.util.Algorithms.formatDuration;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.io.KFile;

import java.util.List;

public class UploadMultipleGPXBottomSheet extends MultipleSelectionBottomSheet<TrackItem> {

	public static final String TAG = UploadMultipleGPXBottomSheet.class.getSimpleName();

	@Override
	protected int getItemLayoutId() {
		return R.layout.gpx_track_select_item;
	}

	@Override
	protected void updateItemView(SelectableItem<TrackItem> item, View view) {
		super.updateItemView(item, view);

		TextView time = view.findViewById(R.id.time);
		TextView distance = view.findViewById(R.id.distance);
		TextView pointsCount = view.findViewById(R.id.points_count);

		TrackItem trackItem = item.getObject();
		GpxTrackAnalysis analysis = GpxUiHelper.getGpxTrackAnalysis(trackItem, app, null);
		if (analysis != null) {
			pointsCount.setText(String.valueOf(analysis.getWptPoints()));
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
			time.setText(formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()));
		}
		AndroidUiHelper.setVisibility(View.VISIBLE, distance, pointsCount, time);
	}

	@Override
	public void onSelectedItemsChanged() {
		super.onSelectedItemsChanged();
		updateSizeDescription();
	}

	private void updateSizeDescription() {
		long size = 0;
		for (SelectableItem<TrackItem> item : selectedItems) {
			KFile file = item.getObject().getFile();
			if (file != null) {
				size += file.length();
			}
		}
		String total = getString(R.string.shared_string_total);
		titleDescription.setText(getString(R.string.ltr_or_rtl_combine_via_colon, total,
				AndroidUtils.formatSize(app, selectedItems.isEmpty() ? 1 : size)));
	}

	@Nullable
	public static UploadMultipleGPXBottomSheet showInstance(@NonNull FragmentManager manager,
	                                                        @NonNull List<SelectableItem<TrackItem>> items,
	                                                        @Nullable List<SelectableItem<TrackItem>> selected) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			UploadMultipleGPXBottomSheet fragment = new UploadMultipleGPXBottomSheet();
			fragment.setItems(items);
			fragment.setSelectedItems(selected);
			fragment.show(manager, TAG);
			return fragment;
		}
		return null;
	}
}