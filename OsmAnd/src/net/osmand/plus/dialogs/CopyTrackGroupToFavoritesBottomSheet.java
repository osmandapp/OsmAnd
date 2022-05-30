package net.osmand.plus.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class CopyTrackGroupToFavoritesBottomSheet extends EditTrackGroupBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(CopyTrackGroupToFavoritesBottomSheet.class);
	private static final String TAG = CopyTrackGroupToFavoritesBottomSheet.class.getName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem titleWithDescr = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.please_provide_group_name_message))
				.setTitle(getString(R.string.copy_to_map_favorites))
				.setLayoutId(R.layout.title_with_desc)
				.create();

		items.add(titleWithDescr);
		super.createMenuItems(savedInstanceState);
	}

	@Override
	public void onRightBottomButtonClick() {
		copyToFavorites();
	}

	private void copyToFavorites() {
		FavouritesHelper favouritesHelper = app.getFavoritesHelper();
		for (GpxDisplayItem item : group.getModifiableList()) {
			if (item.locationStart != null) {
				FavouritePoint fp = FavouritePoint.fromWpt(item.locationStart, app, groupName);
				if (!Algorithms.isEmpty(item.description)) {
					fp.setDescription(item.description);
				}
				favouritesHelper.addFavourite(fp, false);
			}
		}
		favouritesHelper.saveCurrentPointsIntoFile();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnGroupNameChangeListener) {
			OnGroupNameChangeListener listener = (OnGroupNameChangeListener) fragment;
			listener.onTrackGroupChanged();
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_copy;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @NonNull GpxDisplayGroup group) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			CopyTrackGroupToFavoritesBottomSheet fragment = new CopyTrackGroupToFavoritesBottomSheet();
			fragment.group = group;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, CopyTrackGroupToFavoritesBottomSheet.TAG);
		}
	}
}