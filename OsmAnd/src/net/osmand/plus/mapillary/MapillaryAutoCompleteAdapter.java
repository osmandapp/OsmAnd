package net.osmand.plus.mapillary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MapillaryAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

    private static final String TAG = MapillaryAutoCompleteAdapter.class.getSimpleName();
    private final String WRONG_USER_NAME;
    private final String NO_INTERNET_CONNECTION;
    private final ArrayList<String> names;
    private final OsmandApplication app;
    private boolean wrong;

    public MapillaryAutoCompleteAdapter(@NonNull Context context, @LayoutRes int resource, OsmandApplication app) {
        super(context, resource);
        names = new ArrayList<>();
        this.app = app;
        WRONG_USER_NAME = app.getString(R.string.wrong_user_name);
        NO_INTERNET_CONNECTION = app.getString(R.string.no_inet_connection);
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
        if (text.equals(WRONG_USER_NAME) || text.equals(NO_INTERNET_CONNECTION)) {
            return false;
        }
        return true;
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
                            names.add(NO_INTERNET_CONNECTION);
                            wrong = true;
                        } else {
                            Pair<String, String> user = new GetMapillaryUserAsyncTask().execute(constraint.toString()).get();
                            if (user != null) {
                                settings.MAPILLARY_FILTER_USER_KEY.set(user.first);
                                settings.MAPILLARY_FILTER_USERNAME.set(user.second);
                                names.add(user.second);
                                wrong = false;
                            } else {
                                settings.MAPILLARY_FILTER_USER_KEY.set("");
                                settings.MAPILLARY_FILTER_USERNAME.set("");
                                names.add(WRONG_USER_NAME);
                                wrong = true;
                            }
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    } catch (ExecutionException e) {
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.auto_complete_suggestion, parent, false);
        TextView nameTv = (TextView) view.findViewById(R.id.title);
        ImageView iconIv = (ImageView) view.findViewById(R.id.icon);

        nameTv.setText(names.get(position));
        if (wrong) {
            Drawable icon = app.getIconsCache().getPaintedIcon(R.drawable.ic_warning, app.getResources().getColor(R.color.color_warning));
            iconIv.setImageDrawable(icon);
            iconIv.setVisibility(View.VISIBLE);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
            nameTv.setTextColor(typedValue.data);
        }

        return view;
    }

    private class GetMapillaryUserAsyncTask extends AsyncTask<String, Void, Pair<String, String>> {

        private final String TAG = GetMapillaryUserAsyncTask.class.getSimpleName();
        private static final String DOWNLOAD_PATH = "https://a.mapillary.com/v3/users?usernames=%s&client_id=%s";
        private static final String CLIENT_ID = "LXJVNHlDOGdMSVgxZG5mVzlHQ3ZqQTo0NjE5OWRiN2EzNTFkNDg4";

        @Override
        protected Pair<String, String> doInBackground(String... params) {
            try {
                URL url = new URL(String.format(DOWNLOAD_PATH, params[0], CLIENT_ID));
                URLConnection conn = NetworkUtils.getHttpURLConnection(url);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder(1024);
                String tmp;

                while ((tmp = reader.readLine()) != null) {
                    json.append(tmp).append("\n");
                }
                reader.close();

                JSONArray data = new JSONArray(json.toString());

                if (data.length() > 0) {
                    JSONObject user = data.getJSONObject(0);
                    String name = user.getString("username");
                    String key = user.getString("key");
                    if (name != null && key != null) {
                        return new Pair<>(key, name);
                    }
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unable to create url", e);
            } catch (IOException e) {
                Log.e(TAG, "Unable to open connection", e);
            } catch (JSONException e) {
                Log.e(TAG, "Unable to create json", e);
            }

            return null;
        }
    }
}
