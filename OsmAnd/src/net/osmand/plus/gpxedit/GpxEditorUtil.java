package net.osmand.plus.gpxedit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.CallbackWithObject;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Koen    Rabaey
 */
public class GpxEditorUtil {
	public static Toast createToast(Activity context, int drawable, String text, int duration) {
		View layout = LayoutInflater.from(context).inflate(R.layout.gpx_edit_toast, (ViewGroup) context.findViewById(R.id.toast_layout_root));

		TextView tv = (TextView) layout.findViewById(R.id.text);
		tv.setText(text);

		ImageView iv = (ImageView) layout.findViewById(R.id.image);
		iv.setImageResource(drawable);

		final Toast toast = new Toast(context.getApplicationContext());
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(duration);
		toast.setView(layout);

		return toast;
	}

	public static void selectFavourite(final MapActivity activity, final boolean multipleChoice, final CallbackWithObject<FavouritePoint> callbackWithObject) {
		List<FavouritePoint> favourites = new FavouritesDbHelper(activity.getMyApplication()).getFavouritePoints();

		if (favourites.isEmpty()) {
			//todo: favourite not found
			AccessibleToast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
			return;
		}

		final ContextMenuAdapter adapter = new ContextMenuAdapter(activity);
		for (final FavouritePoint s : favourites) {
			adapter.item(s.getCategory() + "/" + s.getName()).selected(multipleChoice ? 0 : -1).icons(R.drawable.ic_action_info_dark, R.drawable.ic_action_info_light).reg();
		}

		createDialog(activity, multipleChoice, callbackWithObject, favourites, adapter);
	}

	private static void createDialog(final Activity activity,
									 final boolean multipleChoice, final CallbackWithObject<FavouritePoint> callbackWithObject,
									 final List<FavouritePoint> list, final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();

		AlertDialog.Builder b = new AlertDialog.Builder(activity);

		// final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		final boolean light = app.getSettings().isLightContentMenu();
		final int layout;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			layout = R.layout.list_menu_item;
		} else {
			layout = R.layout.list_menu_item_native;
		}

		final ArrayAdapter<FavouritePoint> listAdapter = new ArrayAdapter<FavouritePoint>(activity, layout, R.id.title, list) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = activity.getLayoutInflater().inflate(layout, null);
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageResource(adapter.getImageId(position, light));
				final ArrayAdapter<FavouritePoint> arrayAdapter = this;
				icon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int nline = adapter.getItemName(position).indexOf('\n');
						if(nline == -1) {
							setDescripionInDialog(arrayAdapter, adapter, activity, list.get(position), position);
						} else {
							adapter.setItemName(position, adapter.getItemName(position).substring(0, nline));
							arrayAdapter.notifyDataSetInvalidated();
						}
				}

				});
				icon.setVisibility(View.VISIBLE);
				TextView tv = (TextView) v.findViewById(R.id.title);
				tv.setText(adapter.getItemName(position));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				if (adapter.getSelection(position) == -1) {
					ch.setVisibility(View.INVISIBLE);
				} else {
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(adapter.getSelection(position) > 0);
					ch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							adapter.setSelection(position, isChecked ? 1 : 0);
						}
					});
				}
				return v;
			}
		};

		DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
			}
		};
		b.setAdapter(listAdapter, onClickListener);
		if (multipleChoice) {
			b.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					List<FavouritePoint> s = new ArrayList<FavouritePoint>();
					for (int i = 0; i < adapter.length(); i++) {
						if (adapter.getSelection(i) > 0) {
							s.add(list.get(i));
						}
					}
					dialog.dismiss();
				}
			});
		}

		final AlertDialog dlg = b.create();
		dlg.setCanceledOnTouchOutside(true);
		dlg.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (multipleChoice) {
					adapter.setSelection(position, adapter.getSelection(position) > 0 ? 0 : 1);
					listAdapter.notifyDataSetInvalidated();
				} else {
					dlg.dismiss();
					callbackWithObject.processResult(list.get(position));
				}
			}
		});
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
	}

	private static void setDescripionInDialog(final ArrayAdapter<?> adapter, final ContextMenuAdapter cmAdapter, Activity activity,
											  final FavouritePoint favouritePoint, final int position) {
		cmAdapter.setItemName(position, favouritePoint.getCategory() + "\n" + favouritePoint.getName() + "\n" + favouritePoint.getLatitude() + "\n" + favouritePoint.getLongitude() + "\n");
		adapter.notifyDataSetInvalidated();
	}
}
