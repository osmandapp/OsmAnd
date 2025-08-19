package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.plugins.monitoring.TripRecordingOptionsBottomSheet.ACTION_STOP_AND_DISMISS;

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
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.DismissTargetFragment;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.ItemType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class TripRecordingDiscardBottomSheet extends MenuBottomSheetDialogFragment implements DismissTargetFragment {

	public static final String TAG = TripRecordingDiscardBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		int verticalBig = getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalNormal = getDimensionPixelSize(R.dimen.content_padding);
		View buttonDiscard = createItem(ItemType.STOP);
		View buttonCancel = createItem(ItemType.CANCEL);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.track_recording_description))
				.setDescriptionColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setTitle(getString(R.string.track_recording_stop_without_saving))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonDiscard)
				.setOnClickListener(v -> {
					if (plugin != null && settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
						plugin.stopRecording(true);
					}
					dismiss();

					Fragment target = getTargetFragment();
					if (target instanceof TripRecordingOptionsBottomSheet) {
						Bundle args = target.getArguments();
						if (args != null) {
							args.putBoolean(ACTION_STOP_AND_DISMISS, true);
						} else {
							args = new Bundle();
							args.putBoolean(ACTION_STOP_AND_DISMISS, true);
							target.setArguments(args);
						}
					}
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
		if (getTargetFragment() instanceof TripRecordingOptionsBottomSheet bottomSheet) {
			bottomSheet.hide();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (getTargetFragment() instanceof TripRecordingOptionsBottomSheet bottomSheet) {
			bottomSheet.show();
		}
	}

	@Override
	public void dismissTarget() {
		if (getTargetFragment() instanceof TripRecordingOptionsBottomSheet bottomSheet) {
			bottomSheet.dismiss();
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			TripRecordingDiscardBottomSheet fragment = new TripRecordingDiscardBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
