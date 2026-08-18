package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;

public class FavouriteGroupEditorFragment extends GroupEditorFragment {

	private FavouritesHelper favouritesHelper;

	@Nullable
	private FavoriteGroup favoriteGroup;
	private boolean launchPrevIntent;

	@ColorInt
	@Override
	public int getDefaultColor() {
		return ContextCompat.getColor(requireContext(), R.color.color_favorite);
	}

	@Nullable
	@Override
	protected PointEditor getEditor() {
		return requireMapActivity().getContextMenu().getFavoritePointEditor();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesHelper = app.getFavoritesHelper();

		if (pointsGroup != null) {
			favoriteGroup = favouritesHelper.getGroup(pointsGroup.getName());
		}
	}

	@Override
	protected boolean isCategoryExists(@NonNull String name) {
		return favouritesHelper.groupExists(name);
	}

	@Override
	public void addNewGroup() {
		FavoriteGroup favoriteGroup = favouritesHelper.addFavoriteGroup(groupName, getColor(), getIconName(), getBackgroundType());
		pointsGroup = favoriteGroup.toPointsGroup(app);
	}

	@Override
	public void editPointsGroup(boolean updatePoints) {
		if (favoriteGroup != null) {
			favouritesHelper.updateGroupColor(favoriteGroup, getColor(), updatePoints, false);
			favouritesHelper.updateGroupIconName(favoriteGroup, getIconName(), updatePoints, false);
			favouritesHelper.updateGroupBackgroundType(favoriteGroup, getBackgroundType(), updatePoints, false);
			favouritesHelper.updateGroupName(favoriteGroup, getNameTextValue(), false);

			favouritesHelper.saveCurrentPointsIntoFile(true);
			saved = true;
		}
		dismiss();
	}

	@Override
	public void onDestroy() {
		MapActivity mapActivity = getMapActivity();
		if (launchPrevIntent && mapActivity != null && !mapActivity.isChangingConfigurations()) {
			mapActivity.launchPrevActivityIntent();
		}
		super.onDestroy();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @Nullable PointsGroup pointsGroup,
	                                @Nullable CategorySelectionListener listener,
	                                boolean launchPrevIntent) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FavouriteGroupEditorFragment fragment = new FavouriteGroupEditorFragment();
			fragment.listener = listener;
			fragment.pointsGroup = pointsGroup;
			fragment.launchPrevIntent = launchPrevIntent;
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}