package net.osmand.plus.dialogs;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FavoriteDialogs {
	public static final String KEY_FAVORITE = "favorite";
	
	public static Dialog createReplaceFavouriteDialog(final Activity activity, final Bundle args) {
		final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
		final List<FavouritePoint> points = new ArrayList<FavouritePoint>(helper.getFavouritePoints());
		final Collator ci = java.text.Collator.getInstance();
		final boolean distance = args.containsKey("DISTANCE");
		Collections.sort(points, new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint o1, FavouritePoint o2) {
				if (distance && activity instanceof MapActivity) {
					float f1 = (float) MapUtils.getDistance(((MapActivity) activity).getMapLocation(), o1.getLatitude(),
							o1.getLongitude());
					float f2 = (float) MapUtils.getDistance(((MapActivity) activity).getMapLocation(), o2.getLatitude(),
							o2.getLongitude());
					return Float.compare(f1, f2);
				}
				return ci.compare(o1.getCategory() + " " + o1.getName(), o2.getCategory() + " " + o2.getName());
			}
		});
		final String[] names = new String[points.size()];
		if(points.size() == 0){
			AccessibleToast.makeText(activity, activity.getString(R.string.fav_points_not_exist), Toast.LENGTH_SHORT).show();
			return null;
		}
			
		Builder b = new AlertDialog.Builder(activity);
		final FavouritePoint[] favs = new FavouritePoint[points.size()];
		Iterator<FavouritePoint> it = points.iterator();
		int i=0;
		while (it.hasNext()) {
			FavouritePoint fp = it.next();
			// filter gpx points
			favs[i] = fp;
			if(fp.getCategory().trim().length() ==0){
				names[i] = fp.getName();
			} else {
				names[i] = fp.getCategory() + ": " + fp.getName();
			}
			if(activity instanceof MapActivity) {
				names[i] += "  " + OsmAndFormatter.getFormattedDistance(
						(float) MapUtils.getDistance(((MapActivity) activity).getMapLocation(), fp.getLatitude(), 
								fp.getLongitude()), ((MapActivity) activity).getMyApplication());
			}
			i++;
		}
		final int layout;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			layout = R.layout.list_menu_item;
		} else {
			layout = R.layout.list_menu_item_native;
		}
		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layout, R.id.title,
				names) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = activity.getLayoutInflater().inflate(layout, null);
					int vl = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, activity.getResources()
							.getDisplayMetrics());
					final LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(vl, vl);
					ll.setMargins(vl / 4, vl / 4, vl / 4, vl / 4);
					v.findViewById(R.id.icon).setLayoutParams(ll);
				}
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				FavouritePoint fp = points.get(position);
				icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(activity, fp.getColor()));
				
				icon.setVisibility(View.VISIBLE);
				TextView tv = (TextView) v.findViewById(R.id.title);
				tv.setText(names[position]);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
					ch.setVisibility(View.INVISIBLE);
				return v;
			}
		};
		b.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritePoint fv = favs[which];
				FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
				if (helper.editFavourite(fv, point.getLatitude(), point.getLongitude())) {
					AccessibleToast.makeText(activity, activity.getString(R.string.fav_points_edited),
							Toast.LENGTH_SHORT).show();
				}
				if (activity instanceof MapActivity) {
					((MapActivity) activity).getMapView().refreshMap();
				}
			}
		});
		if (activity instanceof MapActivity) {
			b.setPositiveButton(distance ? R.string.sort_by_name : R.string.sort_by_distance,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (distance) {
								args.remove("DISTANCE");
							} else {
								args.putBoolean("DISTANCE", true);
							}
							createReplaceFavouriteDialog(activity, args).show();
						}
					});
		}
		AlertDialog al = b.create();
		return al;
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
	
}
