package net.osmand.plus.fragments;

import net.osmand.plus.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlaceDetailsFragment extends Fragment {
    /**
     * Create a new instance of DetailsFragment, initialized to
     * show the text at 'index'.
     */
    public static PlaceDetailsFragment newInstance(int index) {
        PlaceDetailsFragment f = new PlaceDetailsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);

        return f;
    }

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        TextView text = new TextView(getActivity());
        text.setBackgroundResource(R.color.color_light_gray);
        text.setText("Hello " + String.valueOf(getShownIndex()));
        return text;
    }
}
