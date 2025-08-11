package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.plugins.monitoring.TripRecordingOptionsBottomSheet.ACTION_CLEAR_DATA;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.DismissTargetFragment;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.ItemType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class TripRecordingClearDataBottomSheet extends MenuBottomSheetDialogFragment implements DismissTargetFragment {

	public static final String TAG = TripRecordingClearDataBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		int verticalBig = getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalNormal = getDimensionPixelSize(R.dimen.content_padding);
		String description = getString(R.string.clear_recorded_data_warning)
				.concat("\n").concat(getString(R.string.lost_data_warning));
		View buttonClear = createItem(ItemType.CLEAR_DATA);
		View buttonCancel = createItem(ItemType.CANCEL);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(description)
				.setDescriptionColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setTitle(app.getString(R.string.clear_recorded_data))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonClear)
				.setOnClickListener(v -> {
					app.getSavingTrackHelper().clearRecordedData(true);
					dismiss();
					dismissTarget();
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonCancel)
				.setOnClickListener(v -> dismiss())
				.create());

		items.add(new DividerSpaceItem(app, verticalNormal));
	}

	private View createItem(ItemType type) {
		return TripRecordingBottomSheet.createItem(app, nightMode, getThemedInflater(), type);
	}

	@Override
	public void onResume() {
		super.onResume();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingOptionsBottomSheet) {
			((TripRecordingOptionsBottomSheet) target).hide();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingOptionsBottomSheet) {
			((TripRecordingOptionsBottomSheet) target).show();
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	@Override
	public void dismissTarget() {
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingOptionsBottomSheet) {
			Bundle args = target.getArguments();
			if (args != null) {
				args.putBoolean(ACTION_CLEAR_DATA, true);
			} else {
				args = new Bundle();
				args.putBoolean(ACTION_CLEAR_DATA, true);
				target.setArguments(args);
			}
			((TripRecordingOptionsBottomSheet) target).dismiss();
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			TripRecordingClearDataBottomSheet fragment = new TripRecordingClearDataBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
