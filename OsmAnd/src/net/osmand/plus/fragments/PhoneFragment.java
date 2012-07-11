package net.osmand.plus.fragments;

import net.osmand.plus.R;
import net.osmand.plus.fragments.TabFragment;

public class PhoneFragment extends TabFragment {
    public PhoneFragment() {
    	super(MainMenuFragment.PHONE_TAB_INDEX);
    }

    @Override
    public int getResource() {
        return R.layout.phone_fragment;
    }
}