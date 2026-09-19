package net.osmand.plus.dialogs;


import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.UiUtilities;

public class FavoriteDialogs {

	public static final String KEY_FAVORITE = "favorite";

	@Nullable
	public static AlertDialog.Builder checkDuplicates(@NonNull FavouritePoint point, @NonNull Activity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		FavouritesHelper helper = app.getFavoritesHelper();

		String name = point.getName();
		boolean emoticons = name.length() != point.getName().length();

		String index = "";
		int number = 0;
		point.setCategory(point.getCategory());

		boolean fl = true;
		while (fl) {
			fl = false;
			for (FavouritePoint fp : helper.getFavouritePoints()) {
				if (fp.getName().equals(name)
						&& point.getLatitude() != fp.getLatitude()
						&& point.getLongitude() != fp.getLongitude()
						&& fp.getCategory().equals(point.getCategory())) {
					number++;
					index = " (" + number + ")";
					name = point.getName() + index;
					fl = true;
					break;
				}
			}
		}
		if ((!index.isEmpty() || emoticons)) {
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
			AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
			builder.setTitle(R.string.fav_point_dublicate);
			if (emoticons) {
				builder.setMessage(activity.getString(R.string.fav_point_emoticons_message, name));
			} else {
				builder.setMessage(activity.getString(R.string.fav_point_dublicate_message, name));
			}
			point.setName(name);
			return builder;
		}
		return null;
	}
}
