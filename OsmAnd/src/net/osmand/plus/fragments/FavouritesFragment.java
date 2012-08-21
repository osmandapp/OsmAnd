package net.osmand.plus.fragments;

import java.util.List;

import net.osmand.FavouritePoint;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.PlacePickerActivity;
import net.osmand.plus.activities.search.SearchActivity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FavouritesFragment extends PlaceDetailsFragment /*implements SearchActivityChild*/ {

    public static final String SELECT_FAVORITE_POINT_INTENT_KEY = "SELECT_FAVORITE_POINT_INTENT_KEY";
    public static final int SELECT_FAVORITE_POINT_RESULT_OK = 1;

    private FavouritesAdapter favouritesAdapter;
    private LatLon location;

    private boolean selectFavoriteMode;
    private OsmandSettings settings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.favourites_fragment, null);

        settings = ((OsmandApplication) getApplication()).getSettings();
        favouritesAdapter = new FavouritesAdapter(((OsmandApplication) getApplication()).getFavorites().getFavouritePoints());

        // ListActivity has a ListView, which you can get with:
        ListView lv = getListView(view);
        lv.setCacheColorHint(getResources().getColor(R.color.activity_background));
        lv.setDivider(getResources().getDrawable(R.drawable.tab_text_separator));
        lv.setAdapter(favouritesAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if (!isSelectFavoriteMode()) {
                    FavouritePoint point = favouritesAdapter.getItem(position);
                    settings.SHOW_FAVORITES.set(true);
                    String name = point.getName();
                    LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
                    ((PlacePickerActivity)FavouritesFragment.this.getActivity()).getPlacePickerListener().onPlacePicked(location, name);
                } else {
                    Intent intent = getIntent();
                    intent.putExtra(SELECT_FAVORITE_POINT_INTENT_KEY, favouritesAdapter.getItem(position));
                    getActivity().setResult(SELECT_FAVORITE_POINT_RESULT_OK, intent);
                    getActivity().finish();
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            selectFavoriteMode = intent.hasExtra(SELECT_FAVORITE_POINT_INTENT_KEY);
            if (intent.hasExtra(SearchActivity.SEARCH_LAT) && intent.hasExtra(SearchActivity.SEARCH_LON)) {
                double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
                double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
                if (lat != 0 || lon != 0) {
                    location = new LatLon(lat, lon);
                }
            }
        }
        if (!isSelectFavoriteMode()) {
            if (location == null) {
                location = settings.getLastKnownMapLocation();
            }
        }
    }

    public boolean isSelectFavoriteMode(){
        return selectFavoriteMode;
    }

    private Application getApplication() {
        return getActivity().getApplication();
    }

    private Intent getIntent() {
        return getActivity().getIntent();
    }

    private ListView getListView(View view) {
        return (ListView) view.findViewById(R.id.list);
    }

    class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {

        public FavouritesAdapter(List<FavouritePoint> list) {
            super(FavouritesFragment.this.getActivity(), R.layout.favourites_list_item, list);
        }

        public String getName(FavouritePoint model){
            return model.getName();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = FavouritesFragment.this.getActivity().getLayoutInflater();
                row = inflater.inflate(R.layout.favourites_list_item, parent, false);
            }

            TextView label = (TextView) row.findViewById(R.id.favourite_label);
            TextView distanceLabel = (TextView) row.findViewById(R.id.favouritedistance_label);
            ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
            final FavouritePoint model = getItem(position);
            if (model.isStored()) {
                icon.setImageResource(R.drawable.favorites);
            } else {
                icon.setImageResource(R.drawable.opened_poi);
            }
            if (location != null) {
                int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), location.getLatitude(), location
                        .getLongitude()));
                distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, FavouritesFragment.this.getActivity()));
                distanceLabel.setVisibility(View.VISIBLE);
            } else {
                distanceLabel.setVisibility(View.GONE);
            }
            label.setText(getName(model));
            final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
            row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
            ch.setVisibility(View.GONE);
            return row;
        }
    }
}
