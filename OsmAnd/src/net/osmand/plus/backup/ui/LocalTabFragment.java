package net.osmand.plus.backup.ui;

import android.os.Bundle;

import net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.List;

public class LocalTabFragment extends ChangesTabFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public ChangesTabType getChangesTabType() {
		return ChangesTabType.LOCAL_CHANGES;
	}

	@Override
	public List<SettingsItem> getSettingsItems() {
		return new ArrayList<>();
	}
}
