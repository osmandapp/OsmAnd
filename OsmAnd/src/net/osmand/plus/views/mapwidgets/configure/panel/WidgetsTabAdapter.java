package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WidgetsTabAdapter extends FragmentStateAdapter {

	private final Map<Integer, WeakReference<WidgetsListFragment>> fragments = new HashMap<>();

	public WidgetsTabAdapter(@NonNull Fragment fragment) {
		super(fragment);
	}

	public void updateFragmentsContent() {
		for (WeakReference<WidgetsListFragment> fragmentRef : fragments.values()) {
			WidgetsListFragment fragment = fragmentRef.get();
			if (fragment != null && fragment.getView() != null && !fragment.isRemoving()) {
				fragment.updateContent();
			}
		}
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		WidgetsListFragment fragment = new WidgetsListFragment();
		fragment.setSelectedPanel(WidgetsPanel.values()[position]);
		fragments.put(position, new WeakReference<>(fragment));
		return fragment;
	}

	@Override
	public int getItemCount() {
		return WidgetsPanel.values().length;
	}
}