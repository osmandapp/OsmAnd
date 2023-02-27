package net.osmand.plus.firstusage;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class FirstUsageLocationBottomSheet extends BaseFirstUsageBottomSheet {

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_location);
	}

	@Override
	protected void setupItems(@NonNull ViewGroup container, @NonNull LayoutInflater inflater) {
		container.addView(createItemView(inflater, getString(R.string.search_another_country), R.drawable.ic_show_on_map, view -> {
			listener.onSelectCountry();
			dismiss();
		}));

		container.addView(createItemView(inflater, getString(R.string.determine_location), R.drawable.ic_action_marker_dark, view -> {
			listener.onDetermineLocation();
			dismiss();
		}));
	}
}