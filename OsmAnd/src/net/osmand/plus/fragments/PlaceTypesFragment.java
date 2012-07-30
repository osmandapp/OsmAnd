package net.osmand.plus.fragments;

import java.util.ArrayList;

import net.osmand.plus.activities.PlacePickerActivity;
import android.app.ListFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PlaceTypesFragment extends ListFragment {
    boolean mDualPane;
    int mCurCheckPosition = 0;
    ArrayList<String> places;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // TODO(natashaj): populate in a different way eventually
        places = new ArrayList<String>();
        places.add("Favorites");
        places.add("Address");
        places.add("Food");
        places.add("Petrol");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_activated_1, places) {
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

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

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

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    void showDetails(int index) {
        mCurCheckPosition = index;
        getListView().setItemChecked(index, true);
        ((PlacePickerActivity)getActivity()).getPlacePickerListener().onPlaceTypeChanged(index);
    }

}
