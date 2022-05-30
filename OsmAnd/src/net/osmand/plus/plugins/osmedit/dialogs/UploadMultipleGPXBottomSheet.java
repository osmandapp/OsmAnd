package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.plus.myplaces.ui.AvailableGPXFragment.getGpxTrackAnalysis;
import static net.osmand.util.Algorithms.formatDuration;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.ui.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;

import java.util.List;

public class UploadMultipleGPXBottomSheet extends MultipleSelectionBottomSheet<GpxInfo> {

	public static final String TAG = UploadMultipleGPXBottomSheet.class.getSimpleName();

	@Override
	protected int getItemLayoutId() {
		return R.layout.gpx_track_select_item;
	}

	@Override
	protected void updateItemView(SelectableItem<GpxInfo> item, View view) {
		super.updateItemView(item, view);

		TextView time = view.findViewById(R.id.time);
		TextView distance = view.findViewById(R.id.distance);
		TextView pointsCount = view.findViewById(R.id.points_count);

		GpxInfo info = item.getObject();
		GPXTrackAnalysis analysis = getGpxTrackAnalysis(info, app, null);
		if (analysis != null) {
			pointsCount.setText(String.valueOf(analysis.wptPoints));
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
			time.setText(formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()));
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
		for (SelectableItem<GpxInfo> item : selectedItems) {
			size += item.getObject().getSize();
		}
		String total = getString(R.string.shared_string_total);
		titleDescription.setText(getString(R.string.ltr_or_rtl_combine_via_colon, total,
				AndroidUtils.formatSize(app, selectedItems.size() == 0 ? 1 : size)));
	}

	@Nullable
	public static UploadMultipleGPXBottomSheet showInstance(@NonNull FragmentManager manager,
	                                                        @NonNull List<SelectableItem<GpxInfo>> items,
	                                                        @Nullable List<SelectableItem<GpxInfo>> selected) {
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