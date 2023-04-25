package net.osmand.plus.dialogs;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.SelectFavouriteToReplaceBottomSheet;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;


import java.text.MessageFormat;
import java.util.List;

public class FavoriteDialogs {

	public static final String KEY_FAVORITE = "favorite";

	public static void prepareAddFavouriteDialog(Activity activity, Dialog dialog, Bundle args, double lat, double lon, PointDescription desc) {
		Resources resources = activity.getResources();
		String name = desc == null ? "" : desc.getName();
		if (name.length() == 0) {
			name = resources.getString(R.string.add_favorite_dialog_default_favourite_name);
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		FavouritePoint point = new FavouritePoint(lat, lon, name, app.getSettings().LAST_FAV_CATEGORY_ENTERED.get());
		args.putSerializable(KEY_FAVORITE, point);
		EditText editText = dialog.findViewById(R.id.Name);
		editText.setText(point.getName());
		editText.selectAll();
		editText.requestFocus();
		AutoCompleteTextView cat = dialog.findViewById(R.id.Category);
		cat.setText(point.getCategory());
		AndroidUtils.softKeyboardDelayed(activity, editText);
	}

	public static Dialog createAddFavouriteDialog(Activity activity, Bundle args) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setTitle(R.string.favourites_context_menu_edit);
		View v = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.favorite_edit_dialog, null, false);
		FavouritesHelper helper = app.getFavoritesHelper();
		builder.setView(v);
		EditText editText = v.findViewById(R.id.Name);
		EditText description = v.findViewById(R.id.description);
		AutoCompleteTextView cat = v.findViewById(R.id.Category);
		List<FavoriteGroup> gs = helper.getFavoriteGroups();
		String[] list = new String[gs.size()];
		for (int i = 0; i < list.length; i++) {
			list[i] = gs.get(i).getName();
		}
		cat.setAdapter(new ArrayAdapter<>(activity, R.layout.list_textview, list));
		if (app.accessibilityEnabled()) {
			TextView textButton = v.findViewById(R.id.TextButton);
			textButton.setClickable(true);
			textButton.setFocusable(true);
			textButton.setOnClickListener(view -> {
				AlertDialog.Builder b = new AlertDialog.Builder(themedContext);
				b.setTitle(R.string.access_category_choice);
				b.setItems(list, (dialog, which) -> cat.setText(list[which]));
				b.setNegativeButton(R.string.shared_string_cancel, null);
				b.show();
			});
		}

		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setNeutralButton(R.string.update_existing, (dialog, which) -> {
			SelectFavouriteToReplaceBottomSheet.showInstance(activity, args);
		});
		builder.setPositiveButton(R.string.shared_string_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
				String categoryStr = cat.getText().toString().trim();
				FavouritesHelper helper = app.getFavoritesHelper();
				app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(categoryStr);
				point.setName(editText.getText().toString().trim());
				point.setDescription(description.getText().toString().trim());
				point.setCategory(categoryStr);
				AlertDialog.Builder bld = checkDuplicates(point, activity);
				if (bld != null) {
					bld.setPositiveButton(R.string.shared_string_ok, (dialog1, which1) -> addFavorite(activity, point, helper));
					bld.show();
				} else {
					addFavorite(activity, point, helper);
				}
			}

			protected void addFavorite(Activity activity, FavouritePoint point, FavouritesHelper helper) {
				boolean added = helper.addFavourite(point);
				if (added) {
					Toast.makeText(activity, MessageFormat.format(
									activity.getString(R.string.add_favorite_dialog_favourite_added_template), point.getName()), Toast.LENGTH_SHORT)
							.show();
				}
				if (activity instanceof MapActivity) {
					((MapActivity) activity).getMapView().refreshMap(true);
				}
			}
		});
		return builder.create();
	}

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
		if ((index.length() > 0 || emoticons)) {
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
