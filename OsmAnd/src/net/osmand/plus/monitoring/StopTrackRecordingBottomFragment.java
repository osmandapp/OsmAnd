package net.osmand.plus.monitoring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.ItemType;
import net.osmand.plus.settings.backend.OsmandSettings;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class StopTrackRecordingBottomFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = StopTrackRecordingBottomFragment.class.getSimpleName();

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private OsmandMonitoringPlugin plugin;
	private ItemType tag = ItemType.CANCEL;

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public static void showInstance(MapActivity mapActivity, @NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			StopTrackRecordingBottomFragment fragment = new StopTrackRecordingBottomFragment();
			fragment.setMapActivity(mapActivity);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		int verticalBig = getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalNormal = getResources().getDimensionPixelSize(R.dimen.content_padding);

//		final View buttonDiscard = createItem(inflater, ItemType.STOP_AND_DISCARD);
		final View buttonSave = createItem(inflater, ItemType.STOP_AND_SAVE);
		final View buttonCancel = createItem(inflater, ItemType.CANCEL);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(R.string.track_recording_description))
				.setDescriptionColorId(!nightMode ? R.color.text_color_primary_light : R.color.text_color_primary_dark)
				.setDescriptionMaxLines(4)
				.setTitle(app.getString(R.string.track_recording_title))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		/*items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonDiscard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						tag = (ItemType) buttonDiscard.getTag();
						if (plugin != null && settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							plugin.stopRecording();
							app.getNotificationHelper().refreshNotifications();
						}
						app.getSavingTrackHelper().clearRecordedData(true);
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));*/

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonSave)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						tag = (ItemType) buttonSave.getTag();
						if (plugin != null && settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							plugin.saveCurrentTrack(null, mapActivity);
							app.getNotificationHelper().refreshNotifications();
						}
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalNormal));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonCancel)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						tag = (ItemType) buttonCancel.getTag();
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalNormal));
	}

	private View createItem(LayoutInflater inflater, ItemType type) {
		return TripRecordingActiveBottomSheet.createItem(app, nightMode, inflater, type);
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
			TripRecordingActiveBottomSheet tripRec = (TripRecordingActiveBottomSheet) target;
			if (tag == ItemType.CANCEL) {
				tripRec.show();
			} else {
				tripRec.dismiss();
			}
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}
}
