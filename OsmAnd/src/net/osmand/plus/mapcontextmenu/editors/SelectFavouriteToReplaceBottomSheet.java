package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.SelectFavouriteBottomSheet;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;

public class SelectFavouriteToReplaceBottomSheet extends SelectFavouriteBottomSheet {

	@Override
	protected void onFavouriteSelected(@NonNull FavouritePoint favourite) {
		showConfirmationDialog(favourite);
	}

	private void showConfirmationDialog(@NonNull FavouritePoint favourite) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext());
		builder.setTitle(R.string.update_existing);
		builder.setMessage(getString(R.string.replace_favorite_confirmation, favourite.getName()));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			onApplyReplacement(favourite);
		});
		builder.show();
	}

	private void onApplyReplacement(@NonNull FavouritePoint favourite) {
		FavouritePoint point = getReplacementPoint();
		if (point == null) return;

		favourite.setAddress(point.getAddress()); // Use address from the new point
		FavouritesHelper helper = app.getFavoritesHelper();
		if (helper.editFavourite(favourite, point.getLatitude(), point.getLongitude())) {
			helper.deleteFavourite(point);
			callMapActivity(mapActivity -> {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				Fragment fragment = fragmentManager.findFragmentByTag(FavoritePointEditor.TAG);
				if (fragment instanceof FavoritePointEditorFragment editorFragment) {
					editorFragment.exitEditing();
				}
				dismiss();
				MapContextMenu contextMenu = mapActivity.getContextMenu();
				contextMenu.show(new LatLon(point.getLatitude(), point.getLongitude()), favourite.getPointDescription(mapActivity), favourite);
				mapActivity.refreshMap();
			});
		}
	}

	@Nullable
	private FavouritePoint getReplacementPoint() {
		Fragment target = getTargetFragment();
		if (target instanceof FavoritePointEditorFragment editorFragment) {
			return editorFragment.getFavorite();
		}
		Fragment fragment = requireActivity().getSupportFragmentManager().findFragmentByTag(FavoritePointEditor.TAG);
		if (fragment instanceof FavoritePointEditorFragment editorFragment) {
			return editorFragment.getFavorite();
		}
		return null;
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull Fragment targetFragment) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectFavouriteToReplaceBottomSheet fragment = new SelectFavouriteToReplaceBottomSheet();
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(manager, SelectFavouriteBottomSheet.TAG);
		}
	}
}
