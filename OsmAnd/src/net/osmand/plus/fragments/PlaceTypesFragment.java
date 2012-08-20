package net.osmand.plus.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.plus.activities.PlacePickerActivity;
import net.osmand.plus.R;
import android.app.ListFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PlaceTypesFragment extends ListFragment {
    public static final String FAVORITES_KEY = "Favorites";
    public static final String ADDRESS_SEARCH_KEY = "Address";
    
    public static TreeMap<String, String> places;
    
    boolean mDualPane;
    int mCurCheckPosition = 0;

    static {
        // TODO(natashaj): Remove hard-coding once we support adding / removing filters
        places = new TreeMap<String, String>();
        places.put("Food", "user_food_shop");
        places.put("Fuel", "user_fuel");
    }
    
    public static Map<String, String> getPlacesMap() {
        return places;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_activated_1, getPlacesList()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemView = super.getView(position, convertView, parent);
                if (mCurCheckPosition == position)
                    itemView.setBackgroundColor(Color.GRAY);
                else
                    itemView.setBackgroundColor(Color.TRANSPARENT);
                return itemView;
            }
        };
        setListAdapter(adapter);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setSelected(true);
        showDetails(mCurCheckPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(position);
    }

    List<String> getPlacesList() {
        ArrayList<String> placesList = new ArrayList<String>();
        placesList.add(PlaceTypesFragment.FAVORITES_KEY);
        placesList.add(PlaceTypesFragment.ADDRESS_SEARCH_KEY);
        for (Map.Entry<String, String> entry : places.entrySet()) {
            placesList.add(entry.getKey());
        }
        return placesList;
    }
    
    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    void showDetails(int index) {
        mCurCheckPosition = index;
        getListView().setItemChecked(index, true);
        ((PlacePickerActivity)getActivity()).getPlacePickerListener().onPlaceTypeChanged((String)getListView().getItemAtPosition(index));
    }
}
