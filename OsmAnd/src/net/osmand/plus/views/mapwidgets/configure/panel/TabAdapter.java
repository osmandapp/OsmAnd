package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabAdapter extends FragmentStateAdapter {

	private Fragment wrapperFragment;
	private final List<WidgetsPanel> widgetsPanels;
	private final Map<Integer, WidgetsListFragment> fragments = new HashMap<>();

	public TabAdapter(@NonNull Fragment fragment,
	                  @NonNull List<WidgetsPanel> widgetsPanels) {
		super(fragment);
		this.wrapperFragment = fragment;
		this.widgetsPanels = widgetsPanels;
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		WidgetsListFragment fragment = new WidgetsListFragment();
		fragment.setWrapperFragment(wrapperFragment);
		fragment.setPanel(widgetsPanels.get(position));
		fragments.put(position, fragment);
		return fragment;
	}

	@Override
	public int getItemCount() {
		return widgetsPanels.size();
	}

	public WidgetsListFragment getFragment(int position) {
		return fragments.get(position);
	}

}
