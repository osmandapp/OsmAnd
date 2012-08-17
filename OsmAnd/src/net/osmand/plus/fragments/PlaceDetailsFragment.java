package net.osmand.plus.fragments;

import net.osmand.plus.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlaceDetailsFragment extends Fragment {
    public static final String PLACE_TYPE_KEY = "placeTypeKey";

    /**
     * Create a new instance of DetailsFragment, initialized to
     * show the text at 'index'.
     */
    public static PlaceDetailsFragment newInstance(String placeTypeKey) {
        PlaceDetailsFragment f;
        if (placeTypeKey == PlaceTypesFragment.FAVORITES_KEY) {
            f = new PlaceDetailsFragment();
        } else {
            f = new SearchPOIFragment();
        }

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(PLACE_TYPE_KEY, placeTypeKey);
        f.setArguments(args);

        return f;
    }

    public String getShownPlaceType() {
        return getArguments().getString(PLACE_TYPE_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        TextView text = new TextView(getActivity());
        text.setBackgroundResource(R.color.color_light_gray);
        text.setText("Hello " + String.valueOf(getShownPlaceType()));
        return text;
    }
}
