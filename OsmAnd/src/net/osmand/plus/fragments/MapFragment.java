package net.osmand.plus.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.fragments.TabFragment;

public class MapFragment extends TabFragment {
    public MapFragment() {
    	super(MainMenuFragment.MAP_TAB_INDEX);
    }

    @Override
    public int getResource() {
        return R.layout.main_map_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // MapFragment relies on a lot of legacy code which does not use fragments
        return new TextView(getActivity());
    }
}