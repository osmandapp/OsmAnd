package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.dialogs.CopyTrackGroupToFavoritesBottomSheet;
import net.osmand.plus.dialogs.EditTrackGroupBottomSheet.OnTrackGroupChangeListener;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;

public class AddToFavoritesBottomSheet extends MenuBottomSheetDialogFragment implements OnTrackGroupChangeListener {

	private static final String TAG = AddToFavoritesBottomSheet.class.getName();

	protected GpxDisplayGroup group;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		callActivity(activity -> {
			items.add(new TitleItem(getString(R.string.add_to_favorites)));
			items.add(createCopyToFavoritesItem());
			items.add(new DividerHalfItem(activity));
			items.add(createAddToFavorites());
		});
	}

	@NonNull
	private BaseBottomSheetItem createCopyToFavoritesItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_folder_add))
				.setTitle(getString(R.string.copy_as_new_folder))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> callActivity(activity -> {
					FragmentManager manager = getParentFragmentManager();
					CopyTrackGroupToFavoritesBottomSheet.showInstance(manager, this, group);
				}))
				.create();
	}

	@NonNull
	private BaseBottomSheetItem createAddToFavorites() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_folder_open))
				.setTitle(getString(R.string.add_to_a_folder))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> callActivity(activity -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					SelectFavouriteGroupBottomSheet.showInstance(manager, null, new CategorySelectionListener() {
						@Override
						public void onCategorySelected(PointsGroup pointsGroup) {
							FavouritesHelper favouritesHelper = app.getFavoritesHelper();
							favouritesHelper.copyToFavorites(group, pointsGroup.getName());
							onTrackGroupChanged();
						}

						@Override
						public void onAddGroupOpened() {
							dismiss();
						}
					});
				}))
				.create();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @Nullable Fragment target,
	                                @NonNull GpxDisplayGroup group) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AddToFavoritesBottomSheet fragment = new AddToFavoritesBottomSheet();
			Bundle args = new Bundle();
			fragment.group = group;
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void onTrackGroupChanged() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnTrackGroupChangeListener listener) {
			dismiss();
			listener.onTrackGroupChanged();
		}
	}
}
