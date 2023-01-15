package net.osmand.plus.firstusage;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.firstusage.FirstUsageWizardFragment.WizardType;

public class FirstUsageActionsBottomSheet extends BaseFirstUsageBottomSheet {

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_other);
	}

	@Override
	protected void setupItems(@NonNull ViewGroup container, @NonNull LayoutInflater inflater) {
		container.addView(createItemView(inflater, getString(R.string.restore_from_osmand_cloud), R.drawable.ic_action_restore, view -> {
			listener.onRestoreFromCloud();
			dismiss();
		}));

		if (wizardType != WizardType.MAP_DOWNLOAD) {
			container.addView(createItemView(inflater, getString(R.string.application_dir), R.drawable.ic_action_folder, view -> {
				listener.onSelectStorageFolder();
				dismiss();
			}));
		}
	}
}