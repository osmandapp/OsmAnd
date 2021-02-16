package net.osmand.plus.monitoring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.ItemType;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class ClearRecordedDataBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = ClearRecordedDataBottomSheetFragment.class.getSimpleName();

	private OsmandApplication app;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		int verticalBig = getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(R.string.clear_recorded_data_warning))
				.setDescriptionColorId(!nightMode ? R.color.text_color_primary_light : R.color.text_color_primary_dark)
				.setDescriptionMaxLines(2)
				.setTitle(app.getString(R.string.clear_recorded_data))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(TripRecordingActiveBottomSheet.createButton(inflater, ItemType.CLEAR_DATA, nightMode))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getSavingTrackHelper().clearRecordedData(true);
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(TripRecordingActiveBottomSheet.createButton(inflater, ItemType.CANCEL, nightMode))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalSmall));
	}

	@Override
	public void onResume() {
		super.onResume();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingActiveBottomSheet) {
			((TripRecordingActiveBottomSheet) target).hide();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingActiveBottomSheet) {
			((TripRecordingActiveBottomSheet) target).show();
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			ClearRecordedDataBottomSheetFragment fragment = new ClearRecordedDataBottomSheetFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}