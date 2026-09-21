package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WidgetsTabAdapter extends FragmentStateAdapter {
	private final List<WidgetsPanel> panels;
	private final boolean isAndroidAutoMode;

	public WidgetsTabAdapter(@NonNull Fragment fragment, boolean isAndroidAutoMode) {
		super(fragment);
		this.isAndroidAutoMode = isAndroidAutoMode;
		if (isAndroidAutoMode) {
			panels = List.of(WidgetsPanel.ANDROID_AUTO);
		} else {
			panels = new ArrayList<>(WidgetsPanel.mapPanels);
		}
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		WidgetsListFragment fragment = new WidgetsListFragment();
		fragment.setSelectedPanel(getPanel(position));
		fragment.setAndroidAutoMode(isAndroidAutoMode);
		return fragment;
	}

	@Override
	public int getItemCount() {
		return panels.size();
	}

	public WidgetsPanel getPanel(int position) {
		return panels.get(position);
	}

	public int getTabPosition(WidgetsPanel panel) {
		return panels.indexOf(panel);
	}
}