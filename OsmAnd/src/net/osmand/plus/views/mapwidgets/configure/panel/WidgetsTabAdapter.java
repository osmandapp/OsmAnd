package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WidgetsTabAdapter extends FragmentStateAdapter {
	public WidgetsTabAdapter(@NonNull Fragment fragment) {
		super(fragment);
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		WidgetsListFragment fragment = new WidgetsListFragment();
		fragment.setSelectedPanel(WidgetsPanel.values()[position]);
		return fragment;
	}

	@Override
	public int getItemCount() {
		return WidgetsPanel.values().length;
	}
}