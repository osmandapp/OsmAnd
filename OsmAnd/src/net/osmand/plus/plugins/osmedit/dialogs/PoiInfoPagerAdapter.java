package net.osmand.plus.plugins.osmedit.dialogs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.plugins.osmedit.fragments.AdvancedEditPoiFragment;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment;

public class PoiInfoPagerAdapter extends FragmentStateAdapter {

	private final Fragment[] fragments = {new BasicEditPoiFragment(), new AdvancedEditPoiFragment()};
	private final String[] titles;

	PoiInfoPagerAdapter(Fragment fragment, String[] titles) {
		super(fragment);
		this.titles = titles;
	}

	public CharSequence getPageTitle(int position) {
		return titles[position];
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		return fragments[position];
	}

	@Override
	public int getItemCount() {
		return fragments.length;
	}
}
