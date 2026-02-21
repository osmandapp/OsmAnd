package net.osmand.plus.views.mapwidgets.configure.dialogs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;

class ConfigureScreenAdapter extends FragmentStateAdapter {

	private final OsmandSettings settings;

	public ConfigureScreenAdapter(@NonNull Fragment fragment) {
		super(fragment);
		this.settings = AndroidUtils.getApp(fragment.requireContext()).getSettings();
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		ScreenLayoutMode layoutMode = settings.USE_SEPARATE_LAYOUTS.get() ? ScreenLayoutMode.values()[position] : null;
		return ConfigureScreenPageFragment.newInstance(layoutMode);
	}

	@Override
	public int getItemCount() {
		return settings.USE_SEPARATE_LAYOUTS.get() ? ScreenLayoutMode.values().length : 1;
	}

	@Override
	public long getItemId(int position) {
		return settings.USE_SEPARATE_LAYOUTS.get() ? ScreenLayoutMode.values()[position].ordinal() : -1;
	}

	@Override
	public boolean containsItem(long itemId) {
		if (settings.USE_SEPARATE_LAYOUTS.get()) {
			return itemId >= 0 && itemId < ScreenLayoutMode.values().length;
		}
		return itemId == -1;
	}
}