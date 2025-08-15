package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;

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

import static net.osmand.plus.dialogs.FavoriteDialogs.KEY_FAVORITE;

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
		FavouritePoint point = AndroidUtils.getSerializable(requireArguments(), KEY_FAVORITE, FavouritePoint.class);
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

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable FavouritePoint favouritePoint) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putSerializable(KEY_FAVORITE, favouritePoint);

			SelectFavouriteToReplaceBottomSheet fragment = new SelectFavouriteToReplaceBottomSheet();
			fragment.setArguments(args);
			fragment.show(manager, SelectFavouriteBottomSheet.TAG);
		}
	}
}
