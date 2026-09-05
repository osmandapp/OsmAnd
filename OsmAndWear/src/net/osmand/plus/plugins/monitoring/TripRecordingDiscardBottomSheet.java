package net.osmand.plus.plugins.monitoring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.DismissTargetFragment;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.ItemType;

import static net.osmand.plus.plugins.monitoring.TripRecordingOptionsBottomSheet.ACTION_STOP_AND_DISMISS;

public class TripRecordingDiscardBottomSheet extends MenuBottomSheetDialogFragment implements DismissTargetFragment {

	public static final String TAG = TripRecordingDiscardBottomSheet.class.getSimpleName();

	private OsmandApplication app;

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingDiscardBottomSheet fragment = new TripRecordingDiscardBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		int verticalBig = getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalNormal = getResources().getDimensionPixelSize(R.dimen.content_padding);
		View buttonDiscard = createItem(inflater, ItemType.STOP);
		View buttonCancel = createItem(inflater, ItemType.CANCEL);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.track_recording_description))
				.setDescriptionColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setTitle(app.getString(R.string.track_recording_stop_without_saving))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonDiscard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (plugin != null && app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
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
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonCancel)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalNormal));
	}

	private View createItem(LayoutInflater inflater, ItemType type) {
		return TripRecordingBottomSheet.createItem(app, nightMode, inflater, type);
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
	public void dismissTarget() {
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingOptionsBottomSheet) {
			((TripRecordingOptionsBottomSheet) target).dismiss();
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}
}
