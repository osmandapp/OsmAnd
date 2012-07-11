package net.osmand.plus.fragments;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import net.osmand.plus.R;

public class RoutingOptionListFragment extends ListFragment {
    int mCurCheckPosition = 0;
    
    static String[] ROUTING_LIST_OPTIONS = {
        "Home",
        "Favorites",
        "Recent",
        "POI"
    };
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Populate list with our static array of titles.
        setListAdapter((ListAdapter) new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_activated_1, ROUTING_LIST_OPTIONS));

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

        // In dual-pane mode, the list view highlights the selected item.
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Make sure our UI is in the correct state.
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

    void showDetails(int index) {
        mCurCheckPosition = index;

        // We can display everything in-place with fragments, so update
        // the list to highlight the selected item and show the data.
        getListView().setItemChecked(index, true);

        /*
        // Check what fragment is currently shown, replace if needed.
        RoutingOptionDetailsFragment details = (RoutingOptionDetailsFragment)
                getFragmentManager().findFragmentById(R.id.routingDetailsView);
        if (details == null || details.getShownIndex() != index) {
            // Make new fragment to show this selection.
            details = RoutingOptionDetailsFragment.newInstance(index);

            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.routingDetailsView, details);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
        */
    }
}