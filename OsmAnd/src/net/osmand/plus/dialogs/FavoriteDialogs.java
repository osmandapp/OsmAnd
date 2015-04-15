package net.osmand.plus.dialogs;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesListFragment.FavouritesAdapter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class FavoriteDialogs {
	public static final String KEY_FAVORITE = "favorite";
	
	public static Dialog createReplaceFavouriteDialog(final Activity activity, final Bundle args) {
		final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
		final List<FavouritePoint> points = new ArrayList<FavouritePoint>(helper.getFavouritePoints());
		final FavouritesAdapter favouritesAdapter = new FavouritesAdapter(activity, 
				((OsmandApplication) activity.getApplication()).getFavorites().getFavouritePoints());
		final Dialog[] dlgHolder = new Dialog[1];
		OnItemClickListener click = new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FavouritePoint fp = favouritesAdapter.getItem(position);
				if(dlgHolder != null && dlgHolder.length > 0 && dlgHolder[0] != null) {
					dlgHolder[0].dismiss();
				}
				FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
				if (helper.editFavourite(fp, point.getLatitude(), point.getLongitude())) {
					AccessibleToast.makeText(activity, activity.getString(R.string.fav_points_edited),
							Toast.LENGTH_SHORT).show();
				}
				if (activity instanceof MapActivity) {
					((MapActivity) activity).getMapView().refreshMap();
				}
			}
		};
		if (activity instanceof MapActivity) {
			favouritesAdapter.updateLocation(((MapActivity) activity).getMapLocation());
		}
		final String[] names = new String[points.size()];
		if(points.size() == 0){
			AccessibleToast.makeText(activity, activity.getString(R.string.fav_points_not_exist), Toast.LENGTH_SHORT).show();
			return null;
		}
		return showFavoritesDialog(activity, favouritesAdapter, click, null, dlgHolder, true);
	}
	
	public static void prepareAddFavouriteDialog(Activity activity, Dialog dialog, Bundle args, double lat, double lon, PointDescription desc) {
		final Resources resources = activity.getResources();
		String name = desc == null ? "" : desc.getName();
		if(name.length() == 0) {
			name = resources.getString(R.string.add_favorite_dialog_default_favourite_name);
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final FavouritePoint point = new FavouritePoint(lat, lon, name, app.getSettings().LAST_FAV_CATEGORY_ENTERED.get());
		args.putSerializable(KEY_FAVORITE, point);
		final EditText editText =  (EditText) dialog.findViewById(R.id.Name);
		editText.setText(point.getName());
		editText.selectAll();
		editText.requestFocus();
		final AutoCompleteTextView cat =  (AutoCompleteTextView) dialog.findViewById(R.id.Category);
		cat.setText(point.getCategory());
		AndroidUtils.softKeyboardDelayed(editText);
	}
	
	public  static Dialog createAddFavouriteDialog(final Activity activity, final Bundle args) {
    	Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.favourites_context_menu_edit);
		final View v = activity.getLayoutInflater().inflate(R.layout.favorite_edit_dialog, null, false);
		final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
		builder.setView(v);
		final EditText editText =  (EditText) v.findViewById(R.id.Name);
		final EditText description = (EditText) v.findViewById(R.id.descr);
		final AutoCompleteTextView cat =  (AutoCompleteTextView) v.findViewById(R.id.Category);
		List<FavoriteGroup> gs = helper.getFavoriteGroups();
		String[] list = new String[gs.size()];
		for (int i = 0; i < list.length; i++) {
			list[i] = gs.get(i).name;
		}
		cat.setAdapter(new ArrayAdapter<String>(activity, R.layout.list_textview, list));
		
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setNeutralButton(R.string.update_existing, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Don't use showDialog because it is impossible to refresh favorite items list
				Dialog dlg = createReplaceFavouriteDialog(activity, args);
				if(dlg != null) {
					dlg.show();
				}
				// mapActivity.showDialog(DIALOG_REPLACE_FAVORITE);
			}
			
		});
		builder.setPositiveButton(R.string.shared_string_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
				OsmandApplication app = (OsmandApplication) activity.getApplication();
				String categoryStr = cat.getText().toString().trim();
				final FavouritesDbHelper helper = app.getFavorites();
				app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(categoryStr);
				point.setName(editText.getText().toString().trim());
				point.setDescription(description.getText().toString().trim());
				point.setCategory(categoryStr);
				Builder bld = FavouritesDbHelper.checkDublicates(point, helper, activity);
				if(bld != null) {
					bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							addFavorite(activity, point, helper);							
						}
					});
					bld.show();
				} else {
					addFavorite(activity, point, helper);
				}
			}

			protected void addFavorite(final Activity activity, FavouritePoint point, final FavouritesDbHelper helper) {
				boolean added = helper.addFavourite(point);
				if (added) {
					AccessibleToast.makeText(activity, MessageFormat.format(
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
	
	public static final AlertDialog showFavoritesDialog(
			final Context uiContext,
			final FavouritesAdapter favouritesAdapter, final OnItemClickListener click,
			final OnDismissListener dismissListener, final Dialog[] dialogHolder, final boolean sortByDist) {
		ListView listView = new ListView(uiContext);
		Builder bld = new AlertDialog.Builder(uiContext);
		final Collator inst = Collator.getInstance();
		favouritesAdapter.sort(new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint lhs, FavouritePoint rhs) {
				if (sortByDist) {
					if (favouritesAdapter.getLocation() == null) {
						return 0;
					}
					double ld = MapUtils.getDistance(favouritesAdapter.getLocation(), lhs.getLatitude(),
							lhs.getLongitude());
					double rd = MapUtils.getDistance(favouritesAdapter.getLocation(), rhs.getLatitude(),
							rhs.getLongitude());
					return Double.compare(ld, rd);
				}
				return inst.compare(lhs.getName(), rhs.getName());
			}
		});
		
		listView.setAdapter(favouritesAdapter);
		listView.setOnItemClickListener(click);
		bld.setPositiveButton(sortByDist ? R.string.sort_by_name :
			R.string.sort_by_distance, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showFavoritesDialog(uiContext, favouritesAdapter, click, dismissListener, dialogHolder, !sortByDist);
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.setView(listView);
		AlertDialog dlg = bld.show();
		if(dialogHolder != null) {
			dialogHolder[0] = dlg;
		}
		dlg.setOnDismissListener(dismissListener);
		return dlg;
	}
	
}
