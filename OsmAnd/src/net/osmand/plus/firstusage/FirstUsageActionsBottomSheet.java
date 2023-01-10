package net.osmand.plus.firstusage;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import net.osmand.plus.R;
import net.osmand.plus.firstusage.FirstUsageWizardFragment.WizardType;

public class FirstUsageActionsBottomSheet extends BaseFirstUsageBottomSheet {
	@Override
	protected void fillLayout(LinearLayout layout, LayoutInflater inflater) {
		layout.addView(createItemView(inflater, getString(R.string.restore_from_osmand_cloud), R.drawable.ic_action_restore, view -> {
			dismiss();
			listener.onRestoreFromCloud();
		}));

		if (wizardType != WizardType.MAP_DOWNLOAD) {
			layout.addView(createItemView(inflater, getString(R.string.application_dir), R.drawable.ic_action_folder, view -> {
				dismiss();
				listener.onSelectStorageFolder();
			}));
		}
	}

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_other);
	}
}
