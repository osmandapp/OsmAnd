package net.osmand.plus.fragments;

import net.osmand.plus.R;
import net.osmand.plus.fragments.TabFragment;

public class CarFragment extends TabFragment {
    public CarFragment() {
    	super(MainMenuFragment.CAR_TAB_INDEX);
    }

    @Override
    public int getResource() {
        return R.layout.car_fragment;
    }
}