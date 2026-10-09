package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.content.Context;
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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.SelectFavouriteBottomSheet;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import static net.osmand.plus.dialogs.FavoriteDialogs.KEY_FAVORITE;

public class SelectFavouriteToReplaceBottomSheet extends SelectFavouriteBottomSheet {

	@Override
	protected void onFavouriteSelected(@NonNull FavouritePoint favourite) {
		showConfirmationDialog(favourite);
	}

	private void showConfirmationDialog(@NonNull FavouritePoint favourite) {
		boolean nightMode = isNightMode(app);
		Context themedContext = UiUtilities.getThemedContext(getContext(), nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setTitle(R.string.update_existing);
		builder.setMessage(getString(R.string.replace_favorite_confirmation, favourite.getName()));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			onApplyReplacement(favourite);
		});
		builder.show();
	}

	private void onApplyReplacement(@NonNull FavouritePoint favourite) {
		FavouritePoint point = AndroidUtils.getSerializable(getArguments(), KEY_FAVORITE, FavouritePoint.class);
		FavouritesHelper helper = app.getFavoritesHelper();
		favourite.setAddress(point.getAddress()); // Use address from the new point
		if (point != null && helper.editFavourite(favourite, point.getLatitude(), point.getLongitude())) {
			helper.deleteFavourite(point);
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				FragmentManager fm = mapActivity.getSupportFragmentManager();
				Fragment fragment = fm.findFragmentByTag(FavoritePointEditor.TAG);
				if (fragment instanceof FavoritePointEditorFragment) {
					((FavoritePointEditorFragment) fragment).exitEditing();
				}
				dismiss();
				MapContextMenu contextMenu = mapActivity.getContextMenu();
				contextMenu.show(new LatLon(point.getLatitude(), point.getLongitude()), favourite.getPointDescription(activity), favourite);
				mapActivity.refreshMap();
			}
		}
	}

	public static void showInstance(@NonNull Activity activity, @Nullable Bundle args) {
		SelectFavouriteToReplaceBottomSheet fragment = new SelectFavouriteToReplaceBottomSheet();
		fragment.setArguments(args);
		showFragment((FragmentActivity) activity, fragment);
	}

}
