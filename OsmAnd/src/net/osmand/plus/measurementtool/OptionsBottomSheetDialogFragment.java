package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class OptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "OptionsBottomSheetDialogFragment";

	public static final String SNAP_TO_ROAD_ENABLED_KEY = "snap_to_road_enabled";
	public static final String ADD_LINE_MODE_KEY = "add_line_mode";

	private OptionsFragmentListener listener;

	public void setListener(OptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		boolean snapToRoadEnabled = args.getBoolean(SNAP_TO_ROAD_ENABLED_KEY);
		boolean addLineMode = args.getBoolean(ADD_LINE_MODE_KEY);

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		BaseBottomSheetItem snapToRoadItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.routing_profile_straightline))
				.setIcon(getContentIcon(R.drawable.ic_action_split_interval))
				.setTitle(getString(R.string.route_between_points))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.snapToRoadOnCLick();
						}
						dismiss();
					}
				})
				.create();
		items.add(snapToRoadItem);

		items.add(new DividerHalfItem(getContext()));

		if (addLineMode && !snapToRoadEnabled) {
			BaseBottomSheetItem saveAsNewSegmentItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
					.setTitle(getString(R.string.shared_string_save))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.addToGpxOnClick();
							}
							dismiss();
						}
					})
					.create();
			items.add(saveAsNewSegmentItem);
		} else if (addLineMode) {

			items.add(getSaveAsNewTrackItem());

			BaseBottomSheetItem saveAsNewSegmentItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
					.setTitle("Overwrite GPX")
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.overwriteOldTrackOnClick();
							}
							dismiss();
						}
					})
					.create();
			items.add(saveAsNewSegmentItem);
		} else {
			items.add(getSaveAsNewTrackItem());

			BaseBottomSheetItem addToTrackItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_split_interval))
					.setTitle(getString(R.string.add_to_a_track))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.addToTheTrackOnClick();
							}
							dismiss();
						}
					})
					.create();
			items.add(addToTrackItem);
		}

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem clearAllItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_reset_to_default_dark))
				.setTitle(getString(R.string.shared_string_clear_all))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.clearAllOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(clearAllItem);
	}

	private BaseBottomSheetItem getSaveAsNewTrackItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_save_to_file))
				.setTitle(getString(R.string.edit_filter_save_as_menu_item))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.saveAsNewTrackOnClick();
						}
						dismiss();
					}
				})
				.create();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	interface OptionsFragmentListener {

		void snapToRoadOnCLick();

		void addToGpxOnClick();

		void saveAsNewTrackOnClick();

		void addToTheTrackOnClick();

		void overwriteOldTrackOnClick();

		void clearAllOnClick();
	}
}
