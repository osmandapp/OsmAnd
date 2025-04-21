package net.osmand.plus.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

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
		ParkingPositionPlugin plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
		FavouritesHelper favouritesHelper = app.getFavoritesHelper();

		List<FavouritePoint> addedPoints = new ArrayList<>();
		List<FavouritePoint> duplicatePoints = new ArrayList<>();
		AddFavoriteOptions options = new AddFavoriteOptions().setLookupAddress(true);

		for (GpxDisplayItem item : group.getDisplayItems()) {
			if (item.locationStart != null) {
				FavouritePoint point = FavouritePoint.fromWpt(item.locationStart, groupName);
				if (!Algorithms.isEmpty(item.description)) {
					point.setDescription(item.description);
				}
				if (plugin != null && point.getSpecialPointType() == SpecialPointType.PARKING) {
					plugin.updateParkingPoint(point);
				}
				switch (favouritesHelper.addFavourite(point, options)) {
					case ADDED -> addedPoints.add(point);
					case DUPLICATE -> duplicatePoints.add(point);
				}
			}
		}
		favouritesHelper.saveCurrentPointsIntoFile(true);

		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnGroupNameChangeListener listener) {
			listener.onTrackGroupChanged();
		}
		if (!addedPoints.isEmpty()) {
			app.showShortToastMessage(getString(R.string.msg_gpx_waypoints_copied_to_favorites, addedPoints.size()));
		}
		if (!duplicatePoints.isEmpty()) {
			app.showShortToastMessage(getString(R.string.msg_favorites_skipped_as_existing, duplicatePoints.size()));
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
			fragment.show(fragmentManager, TAG);
		}
	}
}