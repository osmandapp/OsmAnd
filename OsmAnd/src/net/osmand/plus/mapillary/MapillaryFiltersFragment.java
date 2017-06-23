package net.osmand.plus.mapillary;


import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

public class MapillaryFiltersFragment extends BaseOsmAndFragment {

    public static final String TAG = "MAPILLARY_FILTERS_FRAGMENT";

    public MapillaryFiltersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapillary_filters, container, false);

        boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
        final int backgroundColor = ContextCompat.getColor(getActivity(),
                nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);

        view.setBackgroundColor(backgroundColor);
        return view;
    }
}
