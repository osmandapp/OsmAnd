package net.osmand.plus.profiles;

import android.widget.Toast;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

public class SelectAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<SelectProfileMenuAdapter> {
	private List<ApplicationMode> activeModes = new ArrayList<>();

	@Override
	public void onResume() {
		super.onResume();
		activeModes = new ArrayList<>(ApplicationMode.values(getMyApplication()));
		adapter.updateItemsList(activeModes);
		setupHeightAndBackground(getView());
	}

	@Override
	protected void getData() {
		activeModes.addAll(ApplicationMode.values(getMyApplication()));
	}

	@Override
	protected SelectProfileMenuAdapter getMenuAdapter() {
		return new SelectProfileMenuAdapter(activeModes, getMyApplication(), getString(R.string.shared_string_manage), nightMode);
	}

	@Override
	protected String getTitle() {
		return getString(R.string.switch_profile);
	}

	@Override
	public void onProfilePressed(ApplicationMode item) {
		if (!(item == getMyApplication().getSettings().APPLICATION_MODE.get())) {
			getMyApplication().getSettings().APPLICATION_MODE.set(item);
			Toast.makeText(getMyApplication(), String.format(getString(R.string.application_profile_changed), item.toHumanString(getMyApplication())), Toast.LENGTH_SHORT).show();
		}
		dismiss();
	}
}
