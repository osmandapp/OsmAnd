package net.osmand.plus.monitoring;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.ItemType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.TextViewEx;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
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

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		int verticalBig = getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(R.string.track_recording_description))
				.setDescriptionColorId(!nightMode ? R.color.text_color_primary_light : R.color.text_color_primary_dark)
				.setDescriptionMaxLines(4)
				.setTitle(app.getString(R.string.track_recording_title))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupButton(inflater, ItemType.STOP_AND_DISCARD))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						tag = ItemType.STOP_AND_DISCARD;
						if (plugin != null && settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							plugin.stopRecording();
							app.getNotificationHelper().refreshNotifications();
						}
						app.getSavingTrackHelper().clearRecordedData(true);
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupButton(inflater, ItemType.SAVE_AND_STOP))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						tag = ItemType.SAVE_AND_STOP;
						if (plugin != null && settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							plugin.saveCurrentTrack(null, mapActivity);
							app.getNotificationHelper().refreshNotifications();
						}
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalSmall));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupButton(inflater, ItemType.CANCEL))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						tag = ItemType.CANCEL;
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalSmall));
	}

	private View setupButton(LayoutInflater inflater, ItemType type) {
		View button = inflater.inflate(R.layout.bottom_sheet_button_with_icon, null);
		button.setTag(type);
		Context context = button.getContext();
		LinearLayout container = button.findViewById(R.id.button_container);
		container.setClickable(false);
		container.setFocusable(false);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		int horizontal = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		params.setMargins(horizontal, 0, horizontal, 0);
		button.setLayoutParams(params);

		if (type.getTitleId() != null) {
			UiUtilities.setupDialogButton(nightMode, button, type.getEffect(), type.getTitleId());
		}

		TextViewEx title = button.findViewById(R.id.button_text);
		int margin = context.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_medium);
		UiUtilities.setMargins(title, 0, margin, 0, margin);

		int colorRes;
		if (type.getEffect() == DialogButtonType.SECONDARY_HARMFUL) {
			colorRes = R.color.color_osm_edit_delete;
		} else {
			colorRes = nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
		}
		AppCompatImageView icon = button.findViewById(R.id.icon);
		if (type.getIconId() != null) {
			icon.setImageDrawable(getIcon(type.getIconId(), colorRes));
		}

		if (type == ItemType.STOP_AND_DISCARD) {
			int size = context.getResources().getDimensionPixelSize(R.dimen.map_widget_height);
			LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
			icon.setLayoutParams(iconParams);
		}

		return button;
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
		if (tag == ItemType.CANCEL) {
			Fragment target = getTargetFragment();
			if (target instanceof TripRecordingActiveBottomSheet) {
				((TripRecordingActiveBottomSheet) target).show();
			}
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(MapActivity mapActivity, @NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			StopTrackRecordingBottomFragment fragment = new StopTrackRecordingBottomFragment();
			fragment.setMapActivity(mapActivity);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
