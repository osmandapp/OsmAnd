package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.List;

public class WidgetsTabAdapter extends FragmentStateAdapter {

	private final List<WidgetsPanel> widgetsPanels;

	public WidgetsTabAdapter(@NonNull Fragment fragment,
	                         @NonNull List<WidgetsPanel> widgetsPanels) {
		super(fragment);
		this.widgetsPanels = widgetsPanels;
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		WidgetsListFragment fragment = new WidgetsListFragment();
		fragment.setPanel(widgetsPanels.get(position));
		return fragment;
	}

	@Override
	public int getItemCount() {
		return widgetsPanels.size();
	}

}
