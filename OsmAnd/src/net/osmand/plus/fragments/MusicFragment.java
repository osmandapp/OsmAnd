package net.osmand.plus.fragments;

import net.osmand.plus.R;
import net.osmand.plus.fragments.TabFragment;

public class MusicFragment extends TabFragment {
    public MusicFragment() {
    	super(MainMenuFragment.MUSIC_TAB_INDEX);
    }

    @Override
    public int getResource() {
        return R.layout.music_fragment;
    }
}