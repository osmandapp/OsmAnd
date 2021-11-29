package net.osmand.plus.myplaces;

import static net.osmand.plus.myplaces.AvailableGPXFragment.getGpxTrackAnalysis;
import static net.osmand.util.Algorithms.formatDuration;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;

import java.util.List;

public class ExportGPXMultipleSelectionBottomSheet extends MultipleSelectionBottomSheet {

	public static final String TAG = ExportGPXMultipleSelectionBottomSheet.class.getSimpleName();

	@Override
	protected int getItemLayoutId() {
		return R.layout.gpx_track_select_item;
	}

	@Override
	protected void updateItemView(SelectableItem item, View view) {
		boolean checked = selectedItems.contains(item);
		ImageView imageView = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.name);
		TextView distance = view.findViewById(R.id.distance);
		TextView pointsCount = view.findViewById(R.id.points_count);
		TextView time = view.findViewById(R.id.time);
		final CheckBox checkBox = view.findViewById(R.id.compound_button);
		AndroidUiHelper.setVisibility(View.VISIBLE, imageView, title, distance, pointsCount, time, checkBox);
		checkBox.setChecked(checked);
		CompoundButtonCompat.setButtonTintList(checkBox, AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes));

		view.setOnClickListener(v -> {
			boolean isSelected = !checkBox.isChecked();
			checkBox.setChecked(isSelected);
			if (isSelected) {
				selectedItems.add(item);
			} else {
				selectedItems.remove(item);
			}
			onSelectedItemsChanged();
		});
		title.setText(item.getTitle());

		GpxInfo info = (GpxInfo) item.getObject();
		GPXTrackAnalysis analysis = getGpxTrackAnalysis(info, app, null);
		if (analysis != null) {
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
			pointsCount.setText(String.valueOf(analysis.wptPoints));
			time.setText(formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()));
		}
		imageView.setImageDrawable(uiUtilities.getIcon(item.getIconId(), activeColorRes));
	}

	public static ExportGPXMultipleSelectionBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                                 @NonNull List<SelectableItem> items,
	                                                                 @Nullable List<SelectableItem> selected) {
		ExportGPXMultipleSelectionBottomSheet fragment = new ExportGPXMultipleSelectionBottomSheet();
		fragment.setItems(items);
		fragment.setSelectedItems(selected);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}
}