package net.osmand.plus.plugins.mapillary;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MapillaryAutoCompleteAdapter extends ArrayAdapter<String> {

	private static final String TAG = MapillaryAutoCompleteAdapter.class.getSimpleName();

	private final OsmandApplication app;
	private final MapillaryPlugin plugin;

	private final ArrayList<String> names = new ArrayList<>();

	private final String wrongUserName;
	private final String noInternetConnection;

	private boolean wrong;

	public MapillaryAutoCompleteAdapter(@NonNull Context context, @LayoutRes int resource) {
		super(context, resource);
		this.app = (OsmandApplication) context.getApplicationContext();
		this.plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);

		wrongUserName = app.getString(R.string.wrong_user_name);
		noInternetConnection = app.getString(R.string.no_inet_connection);
	}

	@Override
	public int getCount() {
		return names.size();
	}

	@Nullable
	@Override
	public String getItem(int position) {
		return names.get(position);
	}

	@Override
	public boolean isEnabled(int position) {
		String text = names.get(position);
		return !text.equals(wrongUserName) && !text.equals(noInternetConnection);
	}

	@NonNull
	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();
				if (constraint != null) {
					try {
						OsmandSettings settings = app.getSettings();
						names.clear();
						if (!settings.isInternetConnectionAvailable()) {
							names.add(noInternetConnection);
							wrong = true;
						} else {
							GetMapillaryUserAsyncTask task = new GetMapillaryUserAsyncTask();
							Pair<String, String> user = task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, constraint.toString()).get();
							if (user != null) {
								plugin.MAPILLARY_FILTER_USER_KEY.set(user.first);
								plugin.MAPILLARY_FILTER_USERNAME.set(user.second);
								names.add(user.second);
								wrong = false;
							} else {
								plugin.MAPILLARY_FILTER_USER_KEY.set("");
								plugin.MAPILLARY_FILTER_USERNAME.set("");
								names.add(wrongUserName);
								wrong = true;
							}
						}
					} catch (InterruptedException | ExecutionException e) {
						Log.e(TAG, e.toString());
					}
					filterResults.values = names;
					filterResults.count = names.size();
				}

				return filterResults;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if (results != null) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		};
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			convertView = inflater.inflate(R.layout.auto_complete_suggestion, parent, false);
		}
		TextView nameTv = convertView.findViewById(R.id.title);
		ImageView iconIv = convertView.findViewById(R.id.icon);

		nameTv.setText(names.get(position));
		if (wrong) {
			Drawable icon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_alert, app.getColor(R.color.color_warning));
			iconIv.setImageDrawable(icon);
			iconIv.setVisibility(View.VISIBLE);
		}
		return convertView;
	}
}