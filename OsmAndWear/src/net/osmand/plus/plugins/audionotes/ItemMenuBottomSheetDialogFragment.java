package net.osmand.plus.plugins.audionotes;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;


public class ItemMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ItemMenuBottomSheetDialogFragment";

	private ItemMenuFragmentListener listener;
	private Recording recording;

	public void setListener(ItemMenuFragmentListener listener) {
		this.listener = listener;
	}

	public void setRecording(Recording recording) {
		this.recording = recording;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (recording != null) {
			items.add(new TitleItem(recording.getName(getContext(), true)));

			BaseBottomSheetItem playItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(recording.isPhoto() ? R.drawable.ic_action_view : R.drawable.ic_play_dark))
					.setTitle(getString(recording.isPhoto() ? R.string.watch : R.string.recording_context_menu_play))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.playOnClick(recording);
							}
							dismiss();
						}
					})
					.create();
			items.add(playItem);

			Drawable shareIcon = getContentIcon(R.drawable.ic_action_gshare_dark);
			if (shareIcon != null) {
				AndroidUtils.getDrawableForDirection(requireContext(), shareIcon);
			}
			BaseBottomSheetItem shareItem = new SimpleBottomSheetItem.Builder()
					.setIcon(shareIcon)
					.setTitle(getString(R.string.shared_string_share))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.shareOnClick(recording);
							}
							dismiss();
						}
					})
					.create();
			items.add(shareItem);

			BaseBottomSheetItem showOnMapItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(R.string.route_descr_lat_lon, recording.getLatitude(), recording.getLongitude()))
					.setIcon(getContentIcon(R.drawable.ic_show_on_map))
					.setTitle(getString(R.string.shared_string_show_on_map))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.showOnMapOnClick(recording);
							}
							dismiss();
						}
					})
					.create();
			items.add(showOnMapItem);

			items.add(new DividerHalfItem(getContext()));

			BaseBottomSheetItem renameItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
					.setTitle(getString(R.string.shared_string_rename))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.renameOnClick(recording);
							}
							dismiss();
						}
					})
					.create();
			items.add(renameItem);

			BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setTitle(getString(R.string.shared_string_delete))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.deleteOnClick(recording);
							}
							dismiss();
						}
					})
					.create();
			items.add(deleteItem);
		}
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	interface ItemMenuFragmentListener {

		void playOnClick(Recording recording);

		void shareOnClick(Recording recording);

		void showOnMapOnClick(Recording recording);

		void renameOnClick(Recording recording);

		void deleteOnClick(Recording recording);
	}
}
