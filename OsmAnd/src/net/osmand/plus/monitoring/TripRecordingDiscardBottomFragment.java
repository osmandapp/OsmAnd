package net.osmand.plus.monitoring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.monitoring.TripRecordingBottomFragment.ItemType;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static net.osmand.AndroidUtils.getPrimaryTextColorId;
import static net.osmand.plus.monitoring.TripRecordingOptionsBottomFragment.ACTION_STOP_AND_DISCARD;
import static net.osmand.plus.monitoring.TripRecordingOptionsBottomFragment.dismissTargetDialog;

public class TripRecordingDiscardBottomFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingDiscardBottomFragment.class.getSimpleName();

	private OsmandApplication app;

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingDiscardBottomFragment fragment = new TripRecordingDiscardBottomFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		int verticalBig = getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalNormal = getResources().getDimensionPixelSize(R.dimen.content_padding);
		String description = getString(R.string.track_recording_description)
				.concat(getString(R.string.lost_data_warning));
		final View buttonDiscard = createItem(inflater, ItemType.STOP_AND_DISCARD);
		final View buttonCancel = createItem(inflater, ItemType.CANCEL);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(description)
				.setDescriptionColorId(getPrimaryTextColorId(nightMode))
				.setTitle(app.getString(R.string.track_recording_title))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonDiscard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (plugin != null && app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							plugin.stopRecording();
							app.getNotificationHelper().refreshNotifications();
						}
						app.getSavingTrackHelper().clearRecordedData(true);
						dismiss();
						dismissTargetDialog(TripRecordingDiscardBottomFragment.this,
								TripRecordingOptionsBottomFragment.class, ACTION_STOP_AND_DISCARD, true);
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
		return TripRecordingBottomFragment.createItem(app, nightMode, inflater, type);
	}

	@Override
	public void onResume() {
		super.onResume();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingOptionsBottomFragment) {
			((TripRecordingOptionsBottomFragment) target).hide();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingOptionsBottomFragment) {
			((TripRecordingOptionsBottomFragment) target).show();
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}
}
