package net.osmand.plus.firstusage;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import net.osmand.plus.R;

public class FirstUsageLocationBottomSheet extends BaseFirstUsageBottomSheet{
	@Override
	protected void fillLayout(LinearLayout layout, LayoutInflater inflater) {
		layout.addView(createItemView(inflater, getString(R.string.search_another_country), R.drawable.ic_show_on_map, view -> {
			dismiss();
			listener.onSelectCountry();
		}));

		layout.addView(createItemView(inflater, getString(R.string.determine_location), R.drawable.ic_action_marker_dark, view -> {
			dismiss();
			listener.onDetermineLocation();
		}));
	}

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_location);
	}
}
