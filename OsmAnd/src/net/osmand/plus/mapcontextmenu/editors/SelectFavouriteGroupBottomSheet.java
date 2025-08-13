package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class SelectFavouriteGroupBottomSheet extends SelectPointsCategoryBottomSheet {

	@Override
	protected int getDefaultColorId() {
		return R.color.color_favorite;
	}

	@Nullable
	@Override
	protected PointEditor getPointEditor() {
		return ((MapActivity) requireActivity()).getContextMenu().getFavoritePointEditor();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FavouritesHelper helper = app.getFavoritesHelper();
		for (FavoriteGroup favoriteGroup : helper.getFavoriteGroups()) {
			PointsGroup pointsGroup = favoriteGroup.toPointsGroup(app);
			pointsGroups.put(pointsGroup.getName(), pointsGroup);
		}
	}

	@NonNull
	protected BaseBottomSheetItem createCategoriesListItem() {
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.favorite_categories_dialog, null);
		ViewGroup container = view.findViewById(R.id.list_container);

		List<FavoriteGroup> favoriteGroups = app.getFavoritesHelper().getFavoriteGroups();
		for (FavoriteGroup favoriteGroup : favoriteGroups) {
			PointsGroup pointsGroup = favoriteGroup.toPointsGroup(app);
			container.addView(createCategoryItem(pointsGroup, !favoriteGroup.isVisible()));
		}
		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	@Override
	protected void showAddNewCategoryFragment(CategorySelectionListener listener) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			FavouriteGroupEditorFragment.showInstance(manager, null, listener, false);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull String selectedCategory,
	                                @Nullable CategorySelectionListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectFavouriteGroupBottomSheet fragment = new SelectFavouriteGroupBottomSheet();
			Bundle args = new Bundle();
			args.putString(KEY_SELECTED_CATEGORY, selectedCategory);

			fragment.setArguments(args);
			fragment.setListener(listener);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}