package net.osmand.plus.fragments;

import net.osmand.plus.R;
import net.osmand.plus.fragments.TabFragment;

public class RoutingFragment extends TabFragment {
    public RoutingFragment() {
    	super(MainMenuFragment.MAP_TAB_INDEX);
    }

    @Override
    public int getResource() {
        return R.layout.routing_fragment;
    }
}