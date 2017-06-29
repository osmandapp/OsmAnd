package net.osmand.plus.mapillary;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;

public class MapillaryFiltersMapShadowFragment extends Fragment {

    public static final String TAG = "MAPILLARY_FILTERS_MAP_SHADOW_FRAGMENT";

    public MapillaryFiltersMapShadowFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.shadow_on_map, null);
    }
}