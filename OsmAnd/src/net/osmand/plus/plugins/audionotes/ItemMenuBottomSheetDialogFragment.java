package net.osmand.plus.plugins.audionotes;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;


public class ItemMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ItemMenuBottomSheetDialogFragment";

	private ItemMenuFragmentListener listener;
	private MediaNote note;

	public void setListener(ItemMenuFragmentListener listener) {
		this.listener = listener;
	}

	public void setNote(MediaNote note) {
		this.note = note;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (note != null) {
			items.add(new TitleItem(note.getName(getContext(), app.getGalleryHelper().getMetadataRepository(), true)));

			BaseBottomSheetItem playItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(note.isPhoto() ? R.drawable.ic_action_view : R.drawable.ic_play_dark))
					.setTitle(getString(note.isPhoto() ? R.string.watch : R.string.recording_context_menu_play))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(v -> {
						if (listener != null) {
							listener.playOnClick(note);
						}
						dismiss();
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
					.setOnClickListener(v -> {
						if (listener != null) {
							listener.shareOnClick(note);
						}
						dismiss();
					})
					.create();
			items.add(shareItem);

			BaseBottomSheetItem showOnMapItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(R.string.route_descr_lat_lon, note.getLatitude(), note.getLongitude()))
					.setIcon(getContentIcon(R.drawable.ic_show_on_map))
					.setTitle(getString(R.string.shared_string_show_on_map))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
					.setOnClickListener(v -> {
						if (listener != null) {
							listener.showOnMapOnClick(note);
						}
						dismiss();
					})
					.create();
			items.add(showOnMapItem);

			items.add(new DividerHalfItem(getContext()));

			if (note.isRecording()) {
				BaseBottomSheetItem renameItem = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
						.setTitle(getString(R.string.shared_string_rename))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(v -> {
							if (listener != null) {
								listener.renameOnClick(note);
							}
							dismiss();
						})
						.create();
				items.add(renameItem);
			}

			BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setTitle(getString(R.string.shared_string_delete))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(v -> {
						if (listener != null) {
							listener.deleteOnClick(note);
						}
						dismiss();
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

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ItemMenuFragmentListener listener,
	                                @NonNull MediaNote note) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ItemMenuBottomSheetDialogFragment fragment = new ItemMenuBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setListener(listener);
			fragment.setNote(note);
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	interface ItemMenuFragmentListener {

		void playOnClick(MediaNote note);

		void shareOnClick(MediaNote note);

		void showOnMapOnClick(MediaNote note);

		void renameOnClick(MediaNote note);

		void deleteOnClick(MediaNote note);
	}
}
